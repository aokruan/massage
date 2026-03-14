package ru.aokruan.hmlkbi

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSBundle
import ru.aokruan.core.di.Di

fun MainViewController() = ComposeUIViewController {
    val baseUrl = (NSBundle.mainBundle.objectForInfoDictionaryKey("BASE_URL") as? String)
        ?: "http://localhost:8080/"

    val graph = Di.createServiceGraph(
        baseUrl = baseUrl,
        enableLogging = true
    )

    val vm = remember { ServiceListViewModel(graph.getMassagesPage, pageSize = 7) }
    ServiceListScreen(vm)
}