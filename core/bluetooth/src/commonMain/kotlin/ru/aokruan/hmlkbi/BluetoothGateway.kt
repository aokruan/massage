package ru.aokruan.hmlkbi

import kotlinx.coroutines.flow.Flow
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus

interface BluetoothGateway {
    suspend fun ensurePermissions(): Boolean
    suspend fun ensureBluetoothEnabled(): Boolean

    suspend fun connectByQr(
        serviceUuid: String,
        deviceName: String,
    ): Result<Unit>

    fun observeCurrentStatus(): Flow<MedicalCurrentStatus>
    fun observeAlarmEvents(): Flow<AlarmEvent>

    suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit>
    suspend fun disconnect()
}