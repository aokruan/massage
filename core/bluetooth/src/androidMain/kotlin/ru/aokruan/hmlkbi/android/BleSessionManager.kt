package ru.aokruan.hmlkbi.android

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import ru.aokruan.hmlkbi.AlarmHistoryParser
import ru.aokruan.hmlkbi.BleConnectParams
import ru.aokruan.hmlkbi.BleConstants
import ru.aokruan.hmlkbi.BleSessionState
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus
import java.util.UUID
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
class BleSessionManager(
    private val context: Context,
    private val bleStore: AndroidBleStore,
    private val reconnectPolicy: AndroidReconnectPolicy = AndroidReconnectPolicy(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val alarmHistoryParser = AlarmHistoryParser()
    private val operationQueue = GattOperationQueue()

    private val _sessionState = MutableStateFlow<BleSessionState>(BleSessionState.Idle)
    val sessionState: StateFlow<BleSessionState> = _sessionState.asStateFlow()

    private val _currentStatus = MutableStateFlow<MedicalCurrentStatus?>(null)
    val currentStatus: StateFlow<MedicalCurrentStatus?> = _currentStatus.asStateFlow()

    private val _alarmEvents = MutableSharedFlow<AlarmEvent>(extraBufferCapacity = 32)
    val alarmEvents: SharedFlow<AlarmEvent> = _alarmEvents.asSharedFlow()

    private val _alarmHistory = MutableStateFlow<List<AlarmEvent>>(emptyList())
    val alarmHistory: StateFlow<List<AlarmEvent>> = _alarmHistory.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var currentStatusCharacteristic: BluetoothGattCharacteristic? = null
    private var alarmEventCharacteristic: BluetoothGattCharacteristic? = null
    private var alarmHistoryCharacteristic: BluetoothGattCharacteristic? = null
    private var controlCharacteristic: BluetoothGattCharacteristic? = null

    private var connectParams: BleConnectParams? = null
    private var reconnectJob: Job? = null
    private var manualDisconnect = false

    suspend fun connect(params: BleConnectParams): Result<Unit> {
        manualDisconnect = false
        connectParams = params
        bleStore.saveLastParams(params)
        reconnectJob?.cancel()
        return connectInternal(params)
    }

    suspend fun reconnectLast(): Result<Unit> {
        val params = connectParams ?: bleStore.getLastParams()
        ?: return Result.failure(IllegalStateException("No saved connect params"))
        return connect(params)
    }

    suspend fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        operationQueue.clear()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        clearCharacteristics()
        _sessionState.value = BleSessionState.Idle
    }

    suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit> {
        val gatt = bluetoothGatt ?: return Result.failure(
            IllegalStateException("Gatt is null")
        )
        val control = controlCharacteristic ?: return Result.failure(
            IllegalStateException("Control characteristic is null")
        )

        val deferred = CompletableDeferred<Boolean>()
        operationQueue.enqueue(
            GattOperation.WriteCharacteristic(
                characteristic = control,
                value = """{"type":"ack","alarmId":$alarmId}""".encodeToByteArray(),
                deferred = deferred,
            )
        )
        if (!operationQueue.drain(gatt)) {
            return Result.failure(IllegalStateException("Failed to drain write queue"))
        }

        return if (deferred.await()) Result.success(Unit)
        else Result.failure(IllegalStateException("ACK write failed"))
    }

    suspend fun refreshAlarmHistory(): Result<List<AlarmEvent>> {
        val gatt = bluetoothGatt ?: return Result.failure(
            IllegalStateException("Gatt is null")
        )
        val history = alarmHistoryCharacteristic ?: return Result.failure(
            IllegalStateException("alarm_history characteristic is null")
        )

        _sessionState.value = BleSessionState.ReadingAlarmHistory

        val deferred = CompletableDeferred<Boolean>()
        operationQueue.enqueue(
            GattOperation.ReadCharacteristic(
                characteristic = history,
                deferred = deferred,
            )
        )
        if (!operationQueue.drain(gatt)) {
            _sessionState.value = BleSessionState.Failed("Failed to start alarm_history read")
            return Result.failure(IllegalStateException("Queue drain failed"))
        }

        val ok = deferred.await()
        return if (ok) {
            _sessionState.value = BleSessionState.Ready
            Result.success(_alarmHistory.value)
        } else {
            _sessionState.value = BleSessionState.Failed("alarm_history read failed")
            Result.failure(IllegalStateException("alarm_history read failed"))
        }
    }

    private suspend fun connectInternal(params: BleConnectParams): Result<Unit> {
        android.util.Log.d("BleSessionManager", "connectInternal params=$params")
        val adapter = bluetoothAdapter ?: return Result.failure(
            IllegalStateException("Bluetooth adapter is not available")
        )
        val scanner = adapter.bluetoothLeScanner ?: return Result.failure(
            IllegalStateException("BLE scanner is not available")
        )

        _sessionState.value = BleSessionState.Scanning

        val device = withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine<BluetoothDevice?> { continuation ->
                val filter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(UUID.fromString(params.serviceUuid)))
                    .build()

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val candidateName = result.device.name ?: result.scanRecord?.deviceName
                        if (candidateName == params.deviceName) {
                            scanner.stopScan(this)
                            if (continuation.isActive) continuation.resume(result.device)
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }

                scanner.startScan(listOf(filter), settings, callback)

                continuation.invokeOnCancellation {
                    scanner.stopScan(callback)
                }
                android.util.Log.d("BleSessionManager", "device matched name=${params.deviceName}")
            }
        } ?: run {
            _sessionState.value = BleSessionState.Failed("Device not found")
            return Result.failure(IllegalStateException("Device not found"))
        }

        _sessionState.value = BleSessionState.Connecting

        return suspendCancellableCoroutine { continuation ->
            operationQueue.clear()

            val callback = object : BluetoothGattCallback() {

                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    android.util.Log.d(
                        "BleSessionManager",
                        "onConnectionStateChange status=$status newState=$newState"
                    )
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        failAndClose(gatt, continuation, "GATT connection failed: $status")
                        scheduleReconnectIfNeeded()
                        return
                    }

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        bluetoothGatt = gatt
                        _sessionState.value = BleSessionState.DiscoveringServices
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        gatt.close()
                        if (!manualDisconnect) {
                            _sessionState.value = BleSessionState.Reconnecting
                            scheduleReconnectIfNeeded()
                        } else {
                            _sessionState.value = BleSessionState.Idle
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    android.util.Log.d("BleSessionManager", "onServicesDiscovered status=$status")
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        failAndClose(gatt, continuation, "Service discovery failed: $status")
                        scheduleReconnectIfNeeded()
                        return
                    }

                    val service: BluetoothGattService? =
                        gatt.getService(UUID.fromString(params.serviceUuid))

                    if (service == null) {
                        failAndClose(gatt, continuation, "Service not found")
                        scheduleReconnectIfNeeded()
                        return
                    }

                    currentStatusCharacteristic =
                        service.getCharacteristic(UUID.fromString(BleConstants.CURRENT_STATUS_UUID))
                    alarmEventCharacteristic =
                        service.getCharacteristic(UUID.fromString(BleConstants.ALARM_EVENT_UUID))
                    alarmHistoryCharacteristic =
                        service.getCharacteristic(UUID.fromString(BleConstants.ALARM_HISTORY_UUID))
                    controlCharacteristic =
                        service.getCharacteristic(UUID.fromString(BleConstants.CONTROL_UUID))

                    if (currentStatusCharacteristic == null ||
                        alarmEventCharacteristic == null ||
                        alarmHistoryCharacteristic == null ||
                        controlCharacteristic == null
                    ) {
                        failAndClose(gatt, continuation, "Required characteristics not found")
                        scheduleReconnectIfNeeded()
                        return
                    }

                    _sessionState.value = BleSessionState.NegotiatingMtu

                    val mtuDeferred = CompletableDeferred<Boolean>()
                    operationQueue.enqueue(
                        GattOperation.RequestMtu(
                            mtu = 185,
                            deferred = mtuDeferred,
                        )
                    )
                    operationQueue.drain(gatt)

                    scope.launch {
                        val mtuOk = mtuDeferred.await()
                        if (!mtuOk) {
                            failAndClose(gatt, continuation, "MTU request failed")
                            scheduleReconnectIfNeeded()
                            return@launch
                        }

                        _sessionState.value = BleSessionState.SubscribingCurrentStatus
                        val currentDeferred = CompletableDeferred<Boolean>()
                        val currentDescriptor = currentStatusCharacteristic
                            ?.getDescriptor(UUID.fromString(BleConstants.CCCD_UUID))

                        if (currentDescriptor == null) {
                            failAndClose(gatt, continuation, "current_status CCCD not found")
                            scheduleReconnectIfNeeded()
                            return@launch
                        }

                        gatt.setCharacteristicNotification(currentStatusCharacteristic, true)
                        operationQueue.enqueue(
                            GattOperation.WriteDescriptor(
                                descriptor = currentDescriptor,
                                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                                deferred = currentDeferred,
                            )
                        )
                        operationQueue.drain(gatt)

                        val currentOk = currentDeferred.await()
                        if (!currentOk) {
                            failAndClose(gatt, continuation, "current_status subscribe failed")
                            scheduleReconnectIfNeeded()
                            return@launch
                        }

                        _sessionState.value = BleSessionState.SubscribingAlarmEvent
                        val alarmDeferred = CompletableDeferred<Boolean>()
                        val alarmDescriptor = alarmEventCharacteristic
                            ?.getDescriptor(UUID.fromString(BleConstants.CCCD_UUID))

                        if (alarmDescriptor == null) {
                            failAndClose(gatt, continuation, "alarm_event CCCD not found")
                            scheduleReconnectIfNeeded()
                            return@launch
                        }

                        gatt.setCharacteristicNotification(alarmEventCharacteristic, true)
                        operationQueue.enqueue(
                            GattOperation.WriteDescriptor(
                                descriptor = alarmDescriptor,
                                value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                                deferred = alarmDeferred,
                            )
                        )
                        operationQueue.drain(gatt)

                        val alarmOk = alarmDeferred.await()
                        if (!alarmOk) {
                            failAndClose(gatt, continuation, "alarm_event subscribe failed")
                            scheduleReconnectIfNeeded()
                            return@launch
                        }

                        _sessionState.value = BleSessionState.ReadingAlarmHistory
                        val historyDeferred = CompletableDeferred<Boolean>()
                        operationQueue.enqueue(
                            GattOperation.ReadCharacteristic(
                                characteristic = requireNotNull(alarmHistoryCharacteristic),
                                deferred = historyDeferred,
                            )
                        )
                        operationQueue.drain(gatt)

                        val historyOk = historyDeferred.await()
                        if (!historyOk) {
                            failAndClose(gatt, continuation, "alarm_history read failed")
                            scheduleReconnectIfNeeded()
                            return@launch
                        }

                        _sessionState.value = BleSessionState.Ready
                        if (continuation.isActive) {
                            continuation.resume(Result.success(Unit))
                        }
                    }
                }

                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    android.util.Log.d("BleSessionManager", "onMtuChanged mtu=$mtu status=$status")
                    operationQueue.completeCurrent(status == BluetoothGatt.GATT_SUCCESS)
                    operationQueue.drain(gatt)
                }

                override fun onDescriptorWrite(
                    gatt: BluetoothGatt,
                    descriptor: BluetoothGattDescriptor,
                    status: Int,
                ) {
                    android.util.Log.d(
                        "BleSessionManager",
                        "onDescriptorWrite uuid=${descriptor.characteristic.uuid} status=$status"
                    )
                    operationQueue.completeCurrent(status == BluetoothGatt.GATT_SUCCESS)
                    operationQueue.drain(gatt)
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int,
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        if (characteristic.uuid.toString().equals(BleConstants.ALARM_HISTORY_UUID, ignoreCase = true)) {
                            val payload = value.decodeToString()
                            _alarmHistory.value = alarmHistoryParser.parse(payload)
                        }
                    }

                    operationQueue.completeCurrent(status == BluetoothGatt.GATT_SUCCESS)
                    operationQueue.drain(gatt)
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int,
                ) {
                    operationQueue.completeCurrent(status == BluetoothGatt.GATT_SUCCESS)
                    operationQueue.drain(gatt)
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                ) {
                    val value = characteristic.value ?: return
                    val payload = value.decodeToString()

                    when (characteristic.uuid.toString().lowercase()) {
                        BleConstants.CURRENT_STATUS_UUID.lowercase() -> {
                            runCatching {
                                json.decodeFromString(MedicalCurrentStatus.serializer(), payload)
                            }.onSuccess {
                                _currentStatus.value = it
                            }
                        }

                        BleConstants.ALARM_EVENT_UUID.lowercase() -> {
                            runCatching {
                                json.decodeFromString(AlarmEvent.serializer(), payload)
                            }.onSuccess {
                                _alarmEvents.tryEmit(it)
                            }
                        }
                    }
                }
            }

            bluetoothGatt = device.connectGatt(context, false, callback)

            continuation.invokeOnCancellation {
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            }
        }
    }

    private fun scheduleReconnectIfNeeded() {
        if (manualDisconnect) return
        val params = connectParams ?: bleStore.getLastParams() ?: return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var attempt = 0
            while (!manualDisconnect) {
                _sessionState.value = BleSessionState.Reconnecting
                delay(reconnectPolicy.delayForAttempt(attempt))
                val result = connectInternal(params)
                if (result.isSuccess) return@launch
                attempt++
            }
        }
    }

    private fun clearCharacteristics() {
        currentStatusCharacteristic = null
        alarmEventCharacteristic = null
        alarmHistoryCharacteristic = null
        controlCharacteristic = null
    }

    private fun failAndClose(
        gatt: BluetoothGatt,
        continuation: kotlin.coroutines.Continuation<Result<Unit>>,
        message: String,
    ) {
        _sessionState.value = BleSessionState.Failed(message)
        operationQueue.clear()
        gatt.close()
        if (continuation is kotlinx.coroutines.CancellableContinuation && continuation.isActive) {
            continuation.resume(Result.failure(IllegalStateException(message)))
        }
    }
}