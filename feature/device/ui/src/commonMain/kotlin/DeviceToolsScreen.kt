package ru.aokruan.hmlkbi.feature.device.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.aokruan.designsystem.AppColors
import ru.aokruan.designsystem.components.DecorativeBlobs
import ru.aokruan.designsystem.components.PremiumTopBar

@Composable
fun DeviceToolsScreen(
    viewModel: DeviceMonitorViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg)
    ) {
        PremiumTopBar(title = "Инструменты")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            DecorativeBlobs()

            DeviceMonitorScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}