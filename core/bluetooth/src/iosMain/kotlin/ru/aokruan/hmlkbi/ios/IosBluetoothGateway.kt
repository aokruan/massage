package ru.aokruan.hmlkbi.ios

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ru.aokruan.hmlkbi.BluetoothGateway
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus

class IosBluetoothGateway : BluetoothGateway {
    override suspend fun ensurePermissions(): Boolean = false

    override suspend fun ensureBluetoothEnabled(): Boolean = false

    override suspend fun connectByQr(
        serviceUuid: String,
        deviceName: String,
    ): Result<Unit> = Result.failure(NotImplementedError("iOS BLE not implemented yet"))

    override fun observeCurrentStatus(): Flow<MedicalCurrentStatus> = emptyFlow()

    override fun observeAlarmEvents(): Flow<AlarmEvent> = emptyFlow()

    override suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit> =
        Result.failure(NotImplementedError("iOS BLE not implemented yet"))

    override suspend fun disconnect() = Unit
}