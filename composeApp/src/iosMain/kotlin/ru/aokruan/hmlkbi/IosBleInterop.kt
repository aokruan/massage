package ru.aokruan.hmlkbi

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus

internal object IosBleRuntime {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val alarmHistoryParser = AlarmHistoryParser()

    val currentStatus = MutableStateFlow<MedicalCurrentStatus?>(null)
    val alarmEvents = MutableSharedFlow<AlarmEvent>(extraBufferCapacity = 32)
    val alarmHistory = MutableStateFlow<List<AlarmEvent>>(emptyList())
    val sessionState = MutableStateFlow<BleSessionState>(BleSessionState.Idle)

    var startMonitoring: ((String, String) -> Unit)? = null
    var stopMonitoring: (() -> Unit)? = null
    var acknowledgeAlarm: ((Long) -> Unit)? = null

    fun emitCurrentStatusJson(payload: String) {
        runCatching {
            json.decodeFromString(MedicalCurrentStatus.serializer(), payload)
        }.onSuccess {
            currentStatus.value = it
            sessionState.value = BleSessionState.Ready
        }
    }

    fun emitAlarmEventJson(payload: String) {
        runCatching {
            json.decodeFromString(AlarmEvent.serializer(), payload)
        }.onSuccess {
            alarmEvents.tryEmit(it)
            alarmHistory.value = alarmHistory.value.mergeAlarm(it)
        }
    }

    fun emitAlarmHistoryJson(payload: String) {
        alarmHistory.value = alarmHistoryParser.parse(payload)
    }

    fun emitSessionState(name: String, message: String?) {
        sessionState.value = when (name.lowercase()) {
            "idle" -> BleSessionState.Idle
            "scanning" -> BleSessionState.Scanning
            "connecting" -> BleSessionState.Connecting
            "discovering_services" -> BleSessionState.DiscoveringServices
            "negotiating_mtu" -> BleSessionState.NegotiatingMtu
            "subscribing_current_status" -> BleSessionState.SubscribingCurrentStatus
            "subscribing_alarm_event" -> BleSessionState.SubscribingAlarmEvent
            "reading_alarm_history" -> BleSessionState.ReadingAlarmHistory
            "ready" -> BleSessionState.Ready
            "reconnecting" -> BleSessionState.Reconnecting
            "failed" -> BleSessionState.Failed(message ?: "Unknown iOS BLE error")
            else -> sessionState.value
        }
    }

    fun clearMonitoringSnapshot() {
        currentStatus.value = null
        alarmHistory.value = emptyList()
        sessionState.value = BleSessionState.Idle
    }
}

class IosBleInterop {
    fun registerStartMonitoring(block: (String, String) -> Unit) {
        IosBleRuntime.startMonitoring = block
    }

    fun registerStopMonitoring(block: () -> Unit) {
        IosBleRuntime.stopMonitoring = block
    }

    fun registerAcknowledgeAlarm(block: (Long) -> Unit) {
        IosBleRuntime.acknowledgeAlarm = block
    }

    fun startMonitoring(serviceUuid: String, deviceName: String) {
        IosBleRuntime.sessionState.value = BleSessionState.Connecting
        IosBleRuntime.startMonitoring?.invoke(serviceUuid, deviceName)
    }

    fun stopMonitoring() {
        IosBleRuntime.clearMonitoringSnapshot()
        IosBleRuntime.stopMonitoring?.invoke()
    }

    fun acknowledgeAlarm(alarmId: Long) {
        IosBleRuntime.acknowledgeAlarm?.invoke(alarmId)
    }

    fun emitCurrentStatusJson(payload: String) {
        IosBleRuntime.emitCurrentStatusJson(payload)
    }

    fun emitAlarmEventJson(payload: String) {
        IosBleRuntime.emitAlarmEventJson(payload)
    }

    fun emitAlarmHistoryJson(payload: String) {
        IosBleRuntime.emitAlarmHistoryJson(payload)
    }

    fun emitSessionState(name: String, message: String?) {
        IosBleRuntime.emitSessionState(name, message)
    }

    fun clearMonitoringSnapshot() {
        IosBleRuntime.clearMonitoringSnapshot()
    }
}

private fun List<AlarmEvent>.mergeAlarm(alarm: AlarmEvent): List<AlarmEvent> {
    val updated = filterNot { it.id == alarm.id }
    return listOf(alarm) + updated
}
