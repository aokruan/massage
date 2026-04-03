package ru.aokruan.hmlkbi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.aokruan.designsystem.AppColors
import ru.aokruan.designsystem.components.DecorativeBlobs
import ru.aokruan.designsystem.components.GlassSectionCard
import ru.aokruan.designsystem.components.PremiumTopBar
import ru.aokruan.hmlkbi.feature.device.ui.DeviceMonitorScreen
import ru.aokruan.hmlkbi.feature.device.ui.DeviceMonitorViewModel

@Composable
fun BookingScreen() {
    RootPlaceholderScreen(
        title = "Запись",
        message = "Здесь будет сценарий записи на услугу."
    )
}

@Composable
fun ProfileScreen() {
    RootPlaceholderScreen(
        title = "Профиль",
        message = "Здесь будет профиль пользователя и настройки."
    )
}

@Composable
private fun RootPlaceholderScreen(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg)
    ) {
        PremiumTopBar(title = title)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            DecorativeBlobs()

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GlassSectionCard(title = title) {
                    Text(
                        text = message,
                        color = AppColors.Body,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp,
                        ),
                    )
                }
            }
        }
    }
}