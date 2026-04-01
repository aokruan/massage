package ru.aokruan.hmlkbi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.aokruan.core.di.Di
import ru.aokruan.hmlkbi.core.notification.UserNotifier

@Composable
fun App(
    baseUrl: String,
    enableLogging: Boolean,
    notifier: UserNotifier
) {
    val appGraph = remember(baseUrl, enableLogging) {
        Di.createAppGraph(
            baseUrl = baseUrl,
            enableLogging = enableLogging
        )
    }

    AppRoot(
        getMassagesPage = appGraph.serviceGraph.getMassagesPage,
        getMassageById = appGraph.serviceGraph.getMassageById,
        notifier = notifier
    )
}