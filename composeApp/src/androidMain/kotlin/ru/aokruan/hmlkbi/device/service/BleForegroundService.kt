package ru.aokruan.hmlkbi.device.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.aokruan.hmlkbi.BleConnectParams
import ru.aokruan.hmlkbi.BleSessionState
import ru.aokruan.hmlkbi.android.AndroidBleStore
import ru.aokruan.hmlkbi.android.AndroidReconnectPolicy
import ru.aokruan.hmlkbi.android.BleSessionManager
import ru.aokruan.hmlkbi.model.AlarmSeverity
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus
import ru.aokruan.notification.android.AndroidNotificationPermissionRequester
import ru.aokruan.notification.android.AndroidUserNotificationGateway

private const val EXTRA_SERVICE_UUID = "extra_service_uuid"
private const val EXTRA_DEVICE_NAME = "extra_device_name"

class BleForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var bleStore: AndroidBleStore
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationGateway: AndroidUserNotificationGateway
    private lateinit var sessionManager: BleSessionManager

    private var connectJob: Job? = null
    private var sessionStateJob: Job? = null
    private var currentStatusJob: Job? = null
    private var alarmEventsJob: Job? = null

    private var targetParams: BleConnectParams? = null
    private var lastSessionState: BleSessionState = BleSessionState.Idle
    private var lastStatusSummary: String? = null
    private var isForegroundStarted = false

    @Volatile
    private var hasBoundClients: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d("BleForegroundService", "onCreate")
        bleStore = AndroidBleStore(applicationContext)
        notificationManager = getSystemService(NotificationManager::class.java)
        createChannel()
        notificationGateway = AndroidUserNotificationGateway(
            context = applicationContext,
            permissionRequester = AndroidNotificationPermissionRequester(),
        )

        sessionManager = BleSessionManager(
            context = applicationContext,
            bleStore = bleStore,
            reconnectPolicy = AndroidReconnectPolicy(),
        )

        observeSessionManager()
    }

    override fun onBind(intent: Intent?): IBinder {
        hasBoundClients = true
        return LocalBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hasBoundClients = false
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        hasBoundClients = true
        super.onRebind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_RESTORE_MONITORING) {
            ACTION_START_MONITORING -> {
                val params = intent?.let { it.toConnectParams() }
                if (params == null) {
                    Log.e("BleForegroundService", "Start requested without BLE params")
                } else {
                    startMonitoring(params, persistState = true)
                }
            }

            ACTION_STOP_MONITORING -> stopMonitoring(clearPersistedState = true)
            ACTION_RESTORE_MONITORING -> restoreMonitoringIfNeeded()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        connectJob?.cancel()
        sessionStateJob?.cancel()
        currentStatusJob?.cancel()
        alarmEventsJob?.cancel()
        scope.launch {
            sessionManager.disconnect()
        }
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun getService(): BleForegroundService = this@BleForegroundService
    }

    fun sessionManager(): BleSessionManager = sessionManager

    fun startMonitoring(params: BleConnectParams) {
        startMonitoring(params, persistState = true)
    }

    fun stopMonitoring() {
        stopMonitoring(clearPersistedState = true)
    }

    private fun startMonitoring(
        params: BleConnectParams,
        persistState: Boolean,
    ) {
        val sameTargetAlreadyActive =
            targetParams == params && lastSessionState.isMonitoringInProgress()
        if (sameTargetAlreadyActive) {
            Log.d("BleForegroundService", "Monitoring already active for $params")
            bleStore.setMonitoringEnabled(true)
            ensureForegroundStarted()
            refreshForegroundNotification()
            return
        }

        val shouldResetCurrentSession = targetParams != null && targetParams != params
        targetParams = params
        lastStatusSummary = null

        if (persistState) {
            bleStore.saveLastParams(params)
        }
        bleStore.setMonitoringEnabled(true)
        ensureForegroundStarted()

        Log.d("BleForegroundService", "startMonitoring params=$params")

        connectJob?.cancel()
        connectJob = scope.launch {
            if (shouldResetCurrentSession) {
                sessionManager.disconnect()
            }

            val result = sessionManager.connect(params)
            Log.d("BleForegroundService", "connect result=$result")
        }
    }

    private fun stopMonitoring(
        clearPersistedState: Boolean,
    ) {
        Log.d("BleForegroundService", "stopMonitoring clearPersistedState=$clearPersistedState")
        if (clearPersistedState) {
            bleStore.setMonitoringEnabled(false)
        }

        targetParams = null
        lastStatusSummary = null
        connectJob?.cancel()

        scope.launch {
            sessionManager.disconnect()
            if (isForegroundStarted) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundStarted = false
            }
            stopSelf()
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BLE Monitoring",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(targetParams?.deviceName ?: "Medical device monitoring")
            .setContentText(text)
            .setSmallIcon(R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun ensureForegroundStarted() {
        if (isForegroundStarted) return
        startForeground(
            NOTIFICATION_ID,
            buildNotification(resolveNotificationText()),
        )
        isForegroundStarted = true
    }

    private fun observeSessionManager() {
        sessionStateJob = scope.launch {
            sessionManager.sessionState.collectLatest { state ->
                lastSessionState = state
                if (state != BleSessionState.Ready) {
                    lastStatusSummary = null
                }
                if (state is BleSessionState.Idle && !bleStore.isMonitoringEnabled()) {
                    targetParams = null
                }
                refreshForegroundNotification()
            }
        }

        currentStatusJob = scope.launch {
            sessionManager.currentStatus.collectLatest { status ->
                lastStatusSummary = status?.toNotificationSummary()
                refreshForegroundNotification()
            }
        }

        alarmEventsJob = scope.launch {
            sessionManager.alarmEvents.collectLatest { event ->
                if (!hasBoundClients && event.active && event.severity == AlarmSeverity.CRITICAL) {
                    notificationGateway.showCriticalAlarmNotification(
                        title = "Критическое состояние",
                        body = event.message,
                    )
                }
            }
        }
    }

    private fun refreshForegroundNotification() {
        if (!isForegroundStarted) return
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(resolveNotificationText()),
        )
    }

    private fun resolveNotificationText(): String {
        return when (val state = lastSessionState) {
            BleSessionState.Idle -> "Ожидание запуска мониторинга"
            BleSessionState.Scanning -> "Поиск устройства ${targetParams?.deviceName.orEmpty()}".trim()
            BleSessionState.Connecting -> "Подключение к ${targetParams?.deviceName.orEmpty()}".trim()
            BleSessionState.DiscoveringServices -> "Поиск BLE-сервисов"
            BleSessionState.NegotiatingMtu -> "Настройка BLE-канала"
            BleSessionState.SubscribingCurrentStatus -> "Подписка на статус"
            BleSessionState.SubscribingAlarmEvent -> "Подписка на тревоги"
            BleSessionState.ReadingAlarmHistory -> "Чтение истории тревог"
            BleSessionState.Ready -> lastStatusSummary ?: "Устройство подключено"
            BleSessionState.Reconnecting -> "Переподключение к ${targetParams?.deviceName.orEmpty()}".trim()
            is BleSessionState.Failed -> "Ошибка BLE: ${state.message}"
        }
    }

    private fun restoreMonitoringIfNeeded() {
        if (!bleStore.isMonitoringEnabled()) {
            Log.d("BleForegroundService", "Restore skipped: monitoring disabled")
            return
        }

        val params = bleStore.getLastParams()
        if (params == null) {
            Log.e("BleForegroundService", "Restore skipped: no saved BLE params")
            return
        }

        startMonitoring(params, persistState = false)
    }

    companion object {
        const val ACTION_START_MONITORING =
            "ru.aokruan.hmlkbi.device.service.action.START_MONITORING"
        const val ACTION_STOP_MONITORING =
            "ru.aokruan.hmlkbi.device.service.action.STOP_MONITORING"
        private const val ACTION_RESTORE_MONITORING =
            "ru.aokruan.hmlkbi.device.service.action.RESTORE_MONITORING"
        private const val CHANNEL_ID = "ble_monitoring"
        private const val NOTIFICATION_ID = 1001

        fun createStartIntent(
            context: Context,
            params: BleConnectParams,
        ): Intent {
            return Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_START_MONITORING
                putExtra(EXTRA_SERVICE_UUID, params.serviceUuid)
                putExtra(EXTRA_DEVICE_NAME, params.deviceName)
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
        }
    }
}

private fun Intent.toConnectParams(): BleConnectParams? {
    val serviceUuid = getStringExtra(EXTRA_SERVICE_UUID) ?: return null
    val deviceName = getStringExtra(EXTRA_DEVICE_NAME) ?: return null
    return BleConnectParams(
        serviceUuid = serviceUuid,
        deviceName = deviceName,
    )
}

private fun BleSessionState.isMonitoringInProgress(): Boolean {
    return when (this) {
        BleSessionState.Idle -> false
        is BleSessionState.Failed -> false
        else -> true
    }
}

private fun MedicalCurrentStatus.toNotificationSummary(): String {
    val batteryPart = batteryPercent?.let { " • Батарея $it%" }.orEmpty()
    return "Пульс $heartRate • SpO2 $spo2%$batteryPart"
}
