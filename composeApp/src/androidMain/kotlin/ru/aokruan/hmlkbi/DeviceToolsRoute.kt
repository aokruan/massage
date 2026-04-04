package ru.aokruan.hmlkbi

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import ru.aokruan.hmlkbi.android.AndroidMultiplePermissionRequester
import ru.aokruan.hmlkbi.device.service.BleServiceController
import ru.aokruan.hmlkbi.core.notification.NotificationPayload
import ru.aokruan.hmlkbi.core.notification.UserNotifier
import ru.aokruan.hmlkbi.device.service.AndroidMedicalDeviceSessionBridge
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
import ru.aokruan.notification.android.AndroidNotificationPermissionRequester
import ru.aokruan.notification.android.AndroidUserNotificationGateway
import ru.aokruan.qr.BleQrParser
import ru.aokruan.qr.android.AndroidQrCodeGateway
import ru.aokruan.qr.android.AndroidQrScannerCoordinator

@Composable
actual fun DeviceToolsRoute(
    notifier: UserNotifier,
) {
    Log.e("DeviceToolsRoute", "composed")

    val context = LocalContext.current

    val blePermissionRequester = remember { AndroidMultiplePermissionRequester() }
    val notificationPermissionRequester = remember { AndroidNotificationPermissionRequester() }
    val qrScannerCoordinator = remember { AndroidQrScannerCoordinator() }
    val serviceController = remember(context) { BleServiceController(context) }

    DisposableEffect(Unit) {
        Log.e("DeviceToolsRoute", "DisposableEffect bind")
        serviceController.bind()
        onDispose {
            Log.e("DeviceToolsRoute", "DisposableEffect unbind")
            serviceController.unbind()
        }
    }

    val blePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        Log.e("DeviceToolsRoute", "BLE permissions result=$result")
        blePermissionCallback?.invoke(result.values.all { it })
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.e("DeviceToolsRoute", "Notification permission granted=$granted")
        notificationPermissionCallback?.invoke(granted)
    }

    val qrLauncher = rememberLauncherForActivityResult(
        ScanContract()
    ) { result ->
        Log.e("DeviceToolsRoute", "QR result=${result.contents}")
        qrCallback?.invoke(result.contents)
    }

    LaunchedEffect(Unit) {
        Log.e("DeviceToolsRoute", "LaunchedEffect init")

        blePermissionRequester.bind { permissions, callback ->
            Log.e("DeviceToolsRoute", "Request BLE permissions")
            blePermissionCallback = callback
            blePermissionLauncher.launch(permissions)
        }

        notificationPermissionRequester.bind { callback ->
            Log.e("DeviceToolsRoute", "Request notification permission")
            notificationPermissionCallback = callback
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        qrScannerCoordinator.bind { callback ->
            Log.e("DeviceToolsRoute", "Launch QR scanner")
            qrCallback = callback
            qrLauncher.launch(
                ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Сканируй QR устройства")
                    setBeepEnabled(false)
                    setOrientationLocked(false)
                }
            )
        }
    }

    val viewModel = remember(context) {
        Log.e("DeviceToolsRoute", "Create ViewModel graph")

        val qrGateway = AndroidQrCodeGateway(qrScannerCoordinator)

        val notificationGateway = AndroidUserNotificationGateway(
            context = context,
            permissionRequester = notificationPermissionRequester,
        )

        val sessionBridge = AndroidMedicalDeviceSessionBridge(
            controller = serviceController,
        )

        val repository = MedicalDeviceRepositoryImpl(
            bleQrParser = BleQrParser(),
            sessionBridge = sessionBridge,
            notificationGateway = notificationGateway,
            externalNotifier = { title, body ->
                Log.e("DeviceToolsRoute", "externalNotifier title=$title body=$body")
                notifier.show(
                    NotificationPayload(
                        id = System.currentTimeMillis().toString(),
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

    DeviceToolsScreen(
        viewModel = viewModel,
    )
}

private var blePermissionCallback: ((Boolean) -> Unit)? = null
private var notificationPermissionCallback: ((Boolean) -> Unit)? = null
private var qrCallback: ((String?) -> Unit)? = null
