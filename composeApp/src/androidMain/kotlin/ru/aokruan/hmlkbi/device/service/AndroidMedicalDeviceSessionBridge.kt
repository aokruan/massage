package ru.aokruan.hmlkbi.device.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import ru.aokruan.hmlkbi.BleConnectParams
import ru.aokruan.hmlkbi.feature.device.data.MedicalDeviceSessionBridge
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus

class AndroidMedicalDeviceSessionBridge(
    private val controller: BleServiceController,
) : MedicalDeviceSessionBridge {

    override fun startMonitoring(params: BleConnectParams) {
        controller.startMonitoring(params)
    }

    override fun stopMonitoring() {
        controller.stopMonitoring()
    }

    override fun observeCurrentStatus(): Flow<MedicalCurrentStatus> {
        return controller.currentStatus.filterNotNull()
    }

    override fun observeAlarmEvents(): Flow<AlarmEvent> {
        return controller.alarmEvents
    }

    override suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit> {
        return controller.acknowledgeAlarm(alarmId)
    }
}