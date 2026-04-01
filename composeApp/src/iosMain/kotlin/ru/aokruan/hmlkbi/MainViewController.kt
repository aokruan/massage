package ru.aokruan.hmlkbi

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSBundle
import ru.aokruan.hmlkbi.core.notification.IosUserNotifier

fun MainViewController() = ComposeUIViewController {
    val baseUrl = (NSBundle.mainBundle.objectForInfoDictionaryKey("BASE_URL") as? String)
        ?: "http://localhost:8080/"

    val notifier = IosUserNotifier()

    App(
        baseUrl = baseUrl,
        enableLogging = true,
        notifier = notifier,
    )
}
