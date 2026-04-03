package ru.aokruan.hmlkbi

import androidx.compose.runtime.Composable
import ru.aokruan.hmlkbi.core.notification.UserNotifier

@Composable
expect fun DeviceToolsRoute(
    notifier: UserNotifier,
)