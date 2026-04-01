package ru.aokruan.ui.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import ru.aokruan.service.domain.GetMassageByIdUseCase
import ru.aokruan.service.domain.GetMassagesPageUseCase
import ru.aokruan.ui.screens.ServiceDetailScreen
import ru.aokruan.ui.screens.ServiceListScreen
import ru.aokruan.ui.vm.ServiceListViewModel

fun NavGraphBuilder.serviceGraph(
    navController: NavController,
    getMassagesPage: GetMassagesPageUseCase,
    getMassageById: GetMassageByIdUseCase,
    onNotificationRequested: (id: String, title: String, body: String) -> Unit,
) {
    composable<ServiceListRoute> {
        val vm = remember {
            ServiceListViewModel(
                getMassagesPage = getMassagesPage,
                pageSize = 7
            )
        }

        ServiceListScreen(
            vm = vm,
            onItemClick = { serviceId ->
                navController.navigate(ServiceDetailRoute(id = serviceId))
            }
        )
    }

    composable<ServiceDetailRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ServiceDetailRoute>()

        ServiceDetailScreen(
            id = route.id,
            getMassageById = getMassageById,
            onBack = { navController.popBackStack() },
            onNotificationRequested = onNotificationRequested,
        )
    }
}