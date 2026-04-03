package ru.aokruan.hmlkbi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlarmEvent(
    val id: Long,
    val severity: AlarmSeverity,
    val code: String,
    val message: String,
    val active: Boolean,
    @SerialName("ts")
    val timestampEpochSec: Long,
)

@Serializable
enum class AlarmSeverity {
    INFO,
    WARNING,
    CRITICAL,
}