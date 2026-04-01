package ru.aokruan.hmlkbi.core.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import hmlkbi.composeapp.generated.resources.Res
import hmlkbi.composeapp.generated.resources.ic_booking
import hmlkbi.composeapp.generated.resources.ic_list
import hmlkbi.composeapp.generated.resources.ic_plugin
import hmlkbi.composeapp.generated.resources.ic_profile
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.DrawableResource
import ru.aokruan.ui.navigation.ServiceListRoute

@Serializable
data object BookingRoute

@Serializable
data object ToolsRoute

@Serializable
data object ProfileRoute

enum class AppTab(
    val title: String,
    val iconRes: DrawableResource,
) {
    Services(
        title = "Услуги",
        iconRes = Res.drawable.ic_list,
    ),
    Booking(
        title = "Запись",
        iconRes = Res.drawable.ic_booking,
    ),
    Tools(
        title = "Инструменты",
        iconRes = Res.drawable.ic_plugin,
    ),
    Profile(
        title = "Профиль",
        iconRes = Res.drawable.ic_profile,
    )
}

fun AppTab.isSelected(destination: NavDestination?): Boolean {
    return when (this) {
        AppTab.Services -> destination?.hierarchy?.any { it.hasRoute<ServiceListRoute>() } == true
        AppTab.Booking -> destination?.hierarchy?.any { it.hasRoute<BookingRoute>() } == true
        AppTab.Tools -> destination?.hierarchy?.any { it.hasRoute<ToolsRoute>() } == true
        AppTab.Profile -> destination?.hierarchy?.any { it.hasRoute<ProfileRoute>() } == true
    }
}