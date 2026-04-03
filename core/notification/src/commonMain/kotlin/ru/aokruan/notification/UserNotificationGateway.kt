package ru.aokruan.notification

interface UserNotificationGateway {
    suspend fun ensureNotificationPermission(): Boolean
    fun showCriticalAlarmNotification(
        title: String,
        body: String,
    )
}