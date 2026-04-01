package ru.aokruan.hmlkbi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ru.aokruan.androidapp.BuildConfig
import ru.aokruan.hmlkbi.notification.AndroidUserNotifier
import ru.aokruan.hmlkbi.notification.ServiceDetailsNotifications

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Можно залогировать/показать snackbar, если нужно.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ServiceDetailsNotifications.createChannel(applicationContext)
        requestNotificationPermissionIfNeeded()

        val notifier = AndroidUserNotifier(applicationContext)

        setContent {
            App(
                baseUrl = BuildConfig.BASE_URL,
                enableLogging = BuildConfig.DEBUG,
                notifier = notifier,
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
