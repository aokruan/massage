package ru.aokruan.hmlkbi.device.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import ru.aokruan.hmlkbi.BleConnectParams
import ru.aokruan.hmlkbi.BleSessionState
import ru.aokruan.hmlkbi.IosBleInterop
import ru.aokruan.hmlkbi.IosBleRuntime
import ru.aokruan.hmlkbi.feature.device.data.MedicalDeviceSessionBridge
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus

class IosMedicalDeviceSessionBridge : MedicalDeviceSessionBridge {
    private val interop = IosBleInterop()

    override fun startMonitoring(params: BleConnectParams) {
        interop.startMonitoring(
            serviceUuid = params.serviceUuid,
            deviceName = params.deviceName,
        )
    }

    override fun stopMonitoring() {
        interop.stopMonitoring()
    }

    override fun observeCurrentStatus(): Flow<MedicalCurrentStatus> {
        return IosBleRuntime.currentStatus.filterNotNull()
    }

    override fun observeAlarmEvents(): Flow<AlarmEvent> {
        return IosBleRuntime.alarmEvents
    }

    override fun observeAlarmHistory(): Flow<List<AlarmEvent>> {
        return IosBleRuntime.alarmHistory
    }

    override fun observeSessionState(): Flow<BleSessionState> {
        return IosBleRuntime.sessionState
    }

    override suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit> {
        interop.acknowledgeAlarm(alarmId)
        return Result.success(Unit)
    }
}
