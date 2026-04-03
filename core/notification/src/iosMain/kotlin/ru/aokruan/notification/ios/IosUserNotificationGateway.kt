package ru.aokruan.notification.ios

import ru.aokruan.notification.UserNotificationGateway

class IosUserNotificationGateway : UserNotificationGateway {
    override suspend fun ensureNotificationPermission(): Boolean = false

    override fun showCriticalAlarmNotification(
        title: String,
        body: String,
    ) = Unit
}