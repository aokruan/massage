package ru.aokruan.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ServiceRoute

@Serializable
data object ServiceListRoute : ServiceRoute

@Serializable
data class ServiceDetailRoute(
    val id: String
) : ServiceRoute