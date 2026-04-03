package ru.aokruan.hmlkbi.feature.device.domain

import kotlinx.coroutines.flow.Flow
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.BleConnectionState
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus

interface MedicalDeviceRepository {
    suspend fun connectByQr(rawQr: String): BleConnectionState
    fun observeCurrentStatus(): Flow<MedicalCurrentStatus>
    fun observeAlarmEvents(): Flow<AlarmEvent>
    suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit>
    suspend fun disconnect()
}