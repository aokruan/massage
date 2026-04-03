package ru.aokruan.hmlkbi.feature.device.ui

import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.model.BleConnectionState
import ru.aokruan.hmlkbi.model.MedicalCurrentStatus

data class DeviceMonitorUiState(
    val connectionState: BleConnectionState = BleConnectionState.Idle,
    val currentStatus: MedicalCurrentStatus? = null,
    val lastAlarm: AlarmEvent? = null,
    val alarmHistory: List<AlarmEvent> = emptyList(),
    val isLoading: Boolean = false,
    val message: String = "Готово к подключению",
)