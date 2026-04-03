package ru.aokruan.hmlkbi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BleQrPayload(
    @SerialName("v")
    val version: Int,
    val type: String,
    @SerialName("svc")
    val serviceUuid: String,
    val name: String,
) {
    fun isBle(): Boolean = type.lowercase() == "ble"
}