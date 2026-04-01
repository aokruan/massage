package ru.aokruan.hmlkbi.core.notification

import platform.Foundation.NSError
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosUserNotifier : UserNotifier {

    override fun show(payload: NotificationPayload) {
        val content = UNMutableNotificationContent().apply {
            setTitle(payload.title)
            setBody(payload.body)
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = 1.0,
            repeats = false,
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = payload.id,
            content = content,
            trigger = trigger,
        )

        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request) { error: NSError? ->
                error?.let {
                    println("Failed to schedule iOS notification: ${it.localizedDescription}")
                }
            }
    }
}