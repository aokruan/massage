package ru.aokruan.hmlkbi.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.aokruan.hmlkbi.core.notification.NotificationPayload
import ru.aokruan.hmlkbi.core.notification.UserNotifier

object ServiceDetailsNotifications {

    private const val CHANNEL_ID = "service_details_channel"
    private const val CHANNEL_NAME = "Service details"
    private const val CHANNEL_DESCRIPTION = "Notifications about opened service details"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = CHANNEL_DESCRIPTION
        }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun channelId(): String = CHANNEL_ID
}

class AndroidUserNotifier(
    private val context: Context,
) : UserNotifier {

    @SuppressLint("MissingPermission")
    override fun show(payload: NotificationPayload) {
        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(
            context,
            ServiceDetailsNotifications.channelId(),
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(payload.id.hashCode(), notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}