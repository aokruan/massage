package ru.aokruan.hmlkbi.device.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.aokruan.hmlkbi.BleConnectParams
import ru.aokruan.hmlkbi.android.AndroidBleStore
import ru.aokruan.hmlkbi.android.AndroidReconnectPolicy
import ru.aokruan.hmlkbi.android.BleSessionManager

class BleForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var sessionManager: BleSessionManager

    override fun onCreate() {
        super.onCreate()
        Log.d("BleForegroundService", "onCreate")
        createChannel()

        sessionManager = BleSessionManager(
            context = applicationContext,
            bleStore = AndroidBleStore(applicationContext),
            reconnectPolicy = AndroidReconnectPolicy(),
        )

        startForeground(
            NOTIFICATION_ID,
            buildNotification("BLE monitoring active"),
        )
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder()
    }

    override fun onDestroy() {
        scope.launch {
            sessionManager.disconnect()
        }
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun getService(): BleForegroundService = this@BleForegroundService
    }

    fun sessionManager(): BleSessionManager = sessionManager

    fun startMonitoring(params: BleConnectParams) {

        Log.d("BleForegroundService", "startMonitoring params=$params")
        scope.launch {
            val result = sessionManager.connect(params)
            Log.d("BleForegroundService", "connect result=$result")
        }
    }

    fun stopMonitoring() {
        scope.launch {
            sessionManager.disconnect()
            stopSelf()
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BLE Monitoring",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Medical device monitoring")
            .setContentText(text)
            .setSmallIcon(R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "ble_monitoring"
        private const val NOTIFICATION_ID = 1001
    }
}