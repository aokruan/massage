package ru.aokruan.hmlkbi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ru.aokruan.hmlkbi.core.notification.NotificationPayload
import ru.aokruan.hmlkbi.core.notification.UserNotifier
import ru.aokruan.service.domain.GetMassageByIdUseCase
import ru.aokruan.service.domain.GetMassagesPageUseCase
import ru.aokruan.ui.navigation.ServiceListRoute
import ru.aokruan.ui.navigation.serviceGraph

@Composable
fun AppRoot(
    getMassagesPage: GetMassagesPageUseCase,
    getMassageById: GetMassageByIdUseCase,
    notifier: UserNotifier
) {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = ServiceListRoute
        ) {
            serviceGraph(
                navController = navController,
                getMassagesPage = getMassagesPage,
                getMassageById = getMassageById,
                onNotificationRequested = { id, title, body ->
                    notifier.show(
                        NotificationPayload(
                            id = id,
                            title = title,
                            body = body,
                        )
                    )
                },
            )
        }
    }
}