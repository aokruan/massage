package ru.aokruan.hmlkbi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MedicalCurrentStatus(
    @SerialName("hr")
    val heartRate: Int,
    @SerialName("spo2")
    val spo2: Int,
    @SerialName("sys")
    val systolic: Int? = null,
    @SerialName("dia")
    val diastolic: Int? = null,
    @SerialName("battery")
    val batteryPercent: Int? = null,
    @SerialName("ts")
    val timestampEpochSec: Long,
    val critical: Boolean,
    val flags: List<String> = emptyList(),
    val state: String,
)