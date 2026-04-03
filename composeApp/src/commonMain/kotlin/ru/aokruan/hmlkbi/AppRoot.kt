package ru.aokruan.hmlkbi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.aokruan.designsystem.AppColors
import ru.aokruan.hmlkbi.core.navigation.AppBottomBar
import ru.aokruan.hmlkbi.core.navigation.AppTab
import ru.aokruan.hmlkbi.core.navigation.BookingRoute
import ru.aokruan.hmlkbi.core.navigation.ProfileRoute
import ru.aokruan.hmlkbi.core.navigation.ToolsRoute
import ru.aokruan.hmlkbi.core.navigation.isSelected
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
    notifier: UserNotifier,
) {
    MaterialTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination

        val selectedTab = AppTab.entries.firstOrNull { tab ->
            tab.isSelected(currentDestination)
        }

        val showBottomBar = selectedTab != null

        Scaffold(
            containerColor = AppColors.ScreenBg,
            bottomBar = {
                if (showBottomBar) {
                    AppBottomBar(
                        selectedTab = selectedTab,
                        onTabClick = { tab ->
                            when (tab) {
                                AppTab.Services -> {
                                    navController.navigate(ServiceListRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }

                                AppTab.Booking -> {
                                    navController.navigate(BookingRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }

                                AppTab.Tools -> {
                                    navController.navigate(ToolsRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }

                                AppTab.Profile -> {
                                    navController.navigate(ProfileRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.ScreenBg)
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = ServiceListRoute,
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

                    composable<BookingRoute> {
                        BookingScreen()
                    }

                    composable<ToolsRoute> {
                        DeviceToolsRoute(notifier = notifier)
                    }

                    composable<ProfileRoute> {
                        ProfileScreen()
                    }
                }
            }
        }
    }
}