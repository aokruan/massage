package ru.aokruan.hmlkbi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.aokruan.hmlkbi.core.notification.NotificationPayload
import ru.aokruan.hmlkbi.core.notification.UserNotifier
import ru.aokruan.hmlkbi.device.service.IosMedicalDeviceSessionBridge
import ru.aokruan.hmlkbi.feature.device.data.MedicalDeviceRepositoryImpl
import ru.aokruan.hmlkbi.feature.device.domain.AcknowledgeAlarmUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ConnectMedicalDeviceByQrUseCase
import ru.aokruan.hmlkbi.feature.device.domain.DisconnectMedicalDeviceUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalAlarmEventsUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalAlarmHistoryUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalCurrentStatusUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalSessionStateUseCase
import ru.aokruan.hmlkbi.feature.device.ui.DeviceMonitorViewModel
import ru.aokruan.hmlkbi.feature.device.ui.DeviceToolsScreen
import ru.aokruan.notification.ios.IosUserNotificationGateway
import ru.aokruan.qr.BleQrParser
import kotlin.random.Random

@Composable
actual fun DeviceToolsRoute(
    notifier: UserNotifier,
) {
    val viewModel = remember {
        val qrGateway = IosQrCodeGateway()
        val notificationGateway = IosUserNotificationGateway()
        val sessionBridge = IosMedicalDeviceSessionBridge()

        val repository = MedicalDeviceRepositoryImpl(
            bleQrParser = BleQrParser(),
            sessionBridge = sessionBridge,
            notificationGateway = notificationGateway,
            externalNotifier = { title, body ->
                notifier.show(
                    NotificationPayload(
                        id = Random.nextLong().toString(),
                        title = title,
                        body = body,
                    )
                )
            },
        )

        DeviceMonitorViewModel(
            qrCodeGateway = qrGateway,
            connectMedicalDeviceByQrUseCase = ConnectMedicalDeviceByQrUseCase(repository),
            observeMedicalCurrentStatusUseCase = ObserveMedicalCurrentStatusUseCase(repository),
            observeMedicalAlarmEventsUseCase = ObserveMedicalAlarmEventsUseCase(repository),
            observeMedicalAlarmHistoryUseCase = ObserveMedicalAlarmHistoryUseCase(repository),
            observeMedicalSessionStateUseCase = ObserveMedicalSessionStateUseCase(repository),
            acknowledgeAlarmUseCase = AcknowledgeAlarmUseCase(repository),
            disconnectMedicalDeviceUseCase = DisconnectMedicalDeviceUseCase(repository),
        )
    }

    DeviceToolsScreen(viewModel = viewModel)
}
