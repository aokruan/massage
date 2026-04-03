package ru.aokruan.hmlkbi.android

import android.content.Context
import ru.aokruan.hmlkbi.BleConnectParams

class AndroidBleStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("ble_store", Context.MODE_PRIVATE)

    fun saveLastParams(params: BleConnectParams) {
        prefs.edit()
            .putString("service_uuid", params.serviceUuid)
            .putString("device_name", params.deviceName)
            .apply()
    }

    fun getLastParams(): BleConnectParams? {
        val serviceUuid = prefs.getString("service_uuid", null) ?: return null
        val deviceName = prefs.getString("device_name", null) ?: return null
        return BleConnectParams(
            serviceUuid = serviceUuid,
            deviceName = deviceName,
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}