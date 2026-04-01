package ru.aokruan.hmlkbi.core.notification

interface UserNotifier {
    fun show(payload: NotificationPayload)
}