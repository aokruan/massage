package ru.aokruan.hmlkbi

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSBundle

fun MainViewController() = ComposeUIViewController {
    val baseUrl = (NSBundle.mainBundle.objectForInfoDictionaryKey("BASE_URL") as? String)
        ?: "http://localhost:8080/"

    App(
        baseUrl = baseUrl,
        enableLogging = true
    )
}
