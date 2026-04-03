package ru.aokruan.notification.android

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.aokruan.notification.UserNotificationGateway

class AndroidUserNotificationGateway(
    private val context: Context,
    private val permissionRequester: AndroidNotificationPermissionRequester,
) : UserNotificationGateway {

    private val channelId = "medical_alerts"

    override suspend fun ensureNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        val granted = hasNotificationPermission()
        if (granted) return true

        return permissionRequester.request()
    }

    @SuppressLint("MissingPermission")
    override fun showCriticalAlarmNotification(
        title: String,
        body: String,
    ) {
        createChannelIfNeeded()

        if (!hasNotificationPermission()) return

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Medical alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Critical BLE medical alerts"
        }

        manager.createNotificationChannel(channel)
    }
}