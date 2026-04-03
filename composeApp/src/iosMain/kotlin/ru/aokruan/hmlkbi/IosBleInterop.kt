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

    val currentStatus = MutableStateFlow<MedicalCurrentStatus?>(null)
    val alarmEvents = MutableSharedFlow<AlarmEvent>(extraBufferCapacity = 32)

    var startMonitoring: ((String, String) -> Unit)? = null
    var stopMonitoring: (() -> Unit)? = null
    var acknowledgeAlarm: ((Long) -> Unit)? = null

    fun emitCurrentStatusJson(payload: String) {
        runCatching {
            json.decodeFromString(MedicalCurrentStatus.serializer(), payload)
        }.onSuccess {
            currentStatus.value = it
        }
    }

    fun emitAlarmEventJson(payload: String) {
        runCatching {
            json.decodeFromString(AlarmEvent.serializer(), payload)
        }.onSuccess {
            alarmEvents.tryEmit(it)
        }
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
        IosBleRuntime.startMonitoring?.invoke(serviceUuid, deviceName)
    }

    fun stopMonitoring() {
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
}