package ru.aokruan.hmlkbi.android

import android.content.Context
import ru.aokruan.hmlkbi.BleConnectParams

class AndroidBleStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("ble_store", Context.MODE_PRIVATE)

    fun saveLastParams(params: BleConnectParams) {
        val targetChanged =
            prefs.getString("service_uuid", null) != params.serviceUuid ||
                prefs.getString("device_name", null) != params.deviceName

        prefs.edit()
            .putString("service_uuid", params.serviceUuid)
            .putString("device_name", params.deviceName)
            .apply {
                if (targetChanged) {
                    remove("device_address")
                }
            }
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

    fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("monitoring_enabled", enabled)
            .apply()
    }

    fun isMonitoringEnabled(): Boolean {
        return prefs.getBoolean("monitoring_enabled", false)
    }

    fun saveLastDeviceAddress(address: String) {
        prefs.edit()
            .putString("device_address", address)
            .apply()
    }

    fun getLastDeviceAddress(): String? {
        return prefs.getString("device_address", null)
    }

    fun clearLastDeviceAddress() {
        prefs.edit()
            .remove("device_address")
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
