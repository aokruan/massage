package ru.aokruan.hmlkbi.feature.device.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import ru.aokruan.hmlkbi.BleConnectParams
import ru.aokruan.hmlkbi.feature.device.domain.MedicalDeviceRepository
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.AlarmSeverity
import ru.aokruan.hmlkbi.model.BleConnectError
import ru.aokruan.hmlkbi.model.BleConnectionState
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus
import ru.aokruan.notification.UserNotificationGateway
import ru.aokruan.qr.BleQrParser

class MedicalDeviceRepositoryImpl(
    private val bleQrParser: BleQrParser,
    private val sessionBridge: MedicalDeviceSessionBridge,
    private val notificationGateway: UserNotificationGateway,
    private val externalNotifier: ((title: String, body: String) -> Unit)? = null,
) : MedicalDeviceRepository {

    override suspend fun connectByQr(rawQr: String): BleConnectionState {
        val payload = bleQrParser.parse(rawQr).getOrElse {
            return BleConnectionState.Failed(BleConnectError.InvalidQr)
        }

        notificationGateway.ensureNotificationPermission()

        sessionBridge.startMonitoring(
            BleConnectParams(
                serviceUuid = payload.serviceUuid,
                deviceName = payload.name,
            )
        )

        return BleConnectionState.Connecting
    }

    override fun observeCurrentStatus(): Flow<MedicalCurrentStatus> {
        return sessionBridge.observeCurrentStatus()
    }

    override fun observeAlarmEvents(): Flow<AlarmEvent> {
        return sessionBridge.observeAlarmEvents()
            .onEach { event ->
                if (event.active && event.severity == AlarmSeverity.CRITICAL) {
                    notificationGateway.showCriticalAlarmNotification(
                        title = "Критическое состояние",
                        body = event.message,
                    )
                    externalNotifier?.invoke("Критическое состояние", event.message)
                }
            }
    }

    override suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit> {
        return sessionBridge.acknowledgeAlarm(alarmId)
    }

    override suspend fun disconnect() {
        sessionBridge.stopMonitoring()
    }
}

interface MedicalDeviceSessionBridge {
    fun startMonitoring(params: BleConnectParams)
    fun stopMonitoring()
    fun observeCurrentStatus(): Flow<MedicalCurrentStatus>
    fun observeAlarmEvents(): Flow<AlarmEvent>
    suspend fun acknowledgeAlarm(alarmId: Long): Result<Unit>
}