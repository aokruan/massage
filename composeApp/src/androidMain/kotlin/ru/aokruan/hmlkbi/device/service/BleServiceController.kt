package ru.aokruan.hmlkbi.device.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.aokruan.hmlkbi.BleConnectParams
import ru.aokruan.hmlkbi.BleSessionState
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus

class BleServiceController(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var service: BleForegroundService? = null
    private var isBound: Boolean = false

    private var sessionStateJob: Job? = null
    private var currentStatusJob: Job? = null
    private var alarmHistoryJob: Job? = null
    private var alarmEventsJob: Job? = null

    private val _sessionState = MutableStateFlow<BleSessionState>(BleSessionState.Idle)
    val sessionState: StateFlow<BleSessionState> = _sessionState.asStateFlow()

    private val _currentStatus = MutableStateFlow<MedicalCurrentStatus?>(null)
    val currentStatus: StateFlow<MedicalCurrentStatus?> = _currentStatus.asStateFlow()

    private val _alarmHistory = MutableStateFlow<List<AlarmEvent>>(emptyList())
    val alarmHistory: StateFlow<List<AlarmEvent>> = _alarmHistory.asStateFlow()

    private val _alarmEvents = MutableSharedFlow<AlarmEvent>(extraBufferCapacity = 32)
    val alarmEvents: SharedFlow<AlarmEvent> = _alarmEvents.asSharedFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.e("BleServiceController", "onServiceConnected name=$name")
            val localBinder = binder as? BleForegroundService.LocalBinder ?: run {
                Log.e("BleServiceController", "binder cast failed")
                return
            }
            service = localBinder.getService()
            isBound = true

            bindServiceFlows()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.e("BleServiceController", "onServiceDisconnected name=$name")
            service = null
            isBound = false
            cancelFlowBridgeJobs()
            _sessionState.value = BleSessionState.Idle
            _currentStatus.value = null
            _alarmHistory.value = emptyList()
        }
    }

    fun bind() {
        Log.e("BleServiceController", "bind()")
        val intent = Intent(context, BleForegroundService::class.java)
        val ok = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        Log.e("BleServiceController", "bindService result=$ok")
    }

    fun unbind() {
        Log.e("BleServiceController", "unbind() isBound=$isBound")
        if (!isBound) return
        runCatching { context.unbindService(connection) }
        isBound = false
        service = null
        cancelFlowBridgeJobs()
    }

    fun startMonitoring(params: BleConnectParams) {
        Log.e(
            "BleServiceController",
            "startMonitoring params=$params isBound=$isBound serviceNull=${service == null}"
        )

        val intent = BleForegroundService.createStartIntent(context, params)
        context.startForegroundService(intent)
        Log.e("BleServiceController", "startForegroundService called")

        if (!isBound) {
            val ok = context.bindService(
                Intent(context, BleForegroundService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )
            Log.e("BleServiceController", "bindService during start result=$ok")
        }
    }

    fun stopMonitoring() {
        Log.e("BleServiceController", "stopMonitoring()")
        context.startService(BleForegroundService.createStopIntent(context))
    }

    suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit> {
        Log.e("BleServiceController", "acknowledgeAlarm alarmId=$alarmId")
        val svc = service ?: return Result.failure(IllegalStateException("Service is not bound"))
        return svc.sessionManager().acknowledgeAlarm(alarmId)
    }

    private fun bindServiceFlows() {
        Log.e("BleServiceController", "bindServiceFlows()")
        val manager = service?.sessionManager() ?: run {
            Log.e("BleServiceController", "sessionManager is null")
            return
        }

        cancelFlowBridgeJobs()

        sessionStateJob = scope.launch {
            manager.sessionState.collectLatest {
                Log.e("BleServiceController", "sessionState=$it")
                _sessionState.value = it
            }
        }

        currentStatusJob = scope.launch {
            manager.currentStatus.collectLatest {
                Log.e("BleServiceController", "currentStatus=$it")
                _currentStatus.value = it
            }
        }

        alarmHistoryJob = scope.launch {
            manager.alarmHistory.collectLatest {
                Log.e("BleServiceController", "alarmHistory size=${it.size}")
                _alarmHistory.value = it
            }
        }

        alarmEventsJob = scope.launch {
            manager.alarmEvents.collectLatest {
                Log.e("BleServiceController", "alarmEvent=$it")
                _alarmEvents.emit(it)
            }
        }
    }

    private fun cancelFlowBridgeJobs() {
        sessionStateJob?.cancel()
        currentStatusJob?.cancel()
        alarmHistoryJob?.cancel()
        alarmEventsJob?.cancel()

        sessionStateJob = null
        currentStatusJob = null
        alarmHistoryJob = null
        alarmEventsJob = null
    }
}
