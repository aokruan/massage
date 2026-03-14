package ru.aokruan.hmlkbi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun App(
    baseUrl: String,
    enableLogging: Boolean,
    pageSize: Int = 7,
) {
    val graph = remember(baseUrl, enableLogging) {
        ru.aokruan.core.di.Di.createServiceGraph(
            baseUrl = baseUrl,
            enableLogging = enableLogging
        )
    }

    val vm = remember { ServiceListViewModel(graph.getMassagesPage, pageSize = pageSize) }
    ServiceListScreen(vm)
}