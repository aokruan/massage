package ru.aokruan.hmlkbi.feature.device.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.aokruan.hmlkbi.model.BleConnectionState
import ru.aokruan.hmlkbi.feature.device.domain.AcknowledgeAlarmUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ConnectMedicalDeviceByQrUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalAlarmEventsUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalCurrentStatusUseCase
import ru.aokruan.qr.QrCodeGateway

class DeviceMonitorViewModel(
    private val qrCodeGateway: QrCodeGateway,
    private val connectMedicalDeviceByQrUseCase: ConnectMedicalDeviceByQrUseCase,
    private val observeMedicalCurrentStatusUseCase: ObserveMedicalCurrentStatusUseCase,
    private val observeMedicalAlarmEventsUseCase: ObserveMedicalAlarmEventsUseCase,
    private val acknowledgeAlarmUseCase: AcknowledgeAlarmUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(DeviceMonitorUiState())
    val state: StateFlow<DeviceMonitorUiState> = _state

    private var streamsStarted = false

    fun onScanQrClicked() {
        scope.launch {
            val qr = qrCodeGateway.scanQr() ?: return@launch

            _state.update {
                it.copy(
                    isLoading = true,
                    message = "Запуск мониторинга..."
                )
            }

            val result = connectMedicalDeviceByQrUseCase(qr)

            _state.update {
                it.copy(
                    isLoading = false,
                    connectionState = result,
                    message = result.toUiMessage(),
                )
            }

            if (!streamsStarted) {
                streamsStarted = true
                observeStreams()
            }
        }
    }

    fun onAcknowledgeAlarmClicked() {
        val alarmId = _state.value.lastAlarm?.id ?: return
        scope.launch {
            acknowledgeAlarmUseCase(alarmId)
        }
    }

    private fun observeStreams() {
        scope.launch {
            observeMedicalCurrentStatusUseCase().collect { status ->
                _state.update { current ->
                    current.copy(
                        currentStatus = status,
                        message = "Устройство подключено",
                        connectionState = BleConnectionState.Connected,
                    )
                }
            }
        }

        scope.launch {
            observeMedicalAlarmEventsUseCase().collect { alarm ->
                _state.update { current ->
                    current.copy(lastAlarm = alarm)
                }
            }
        }
    }
}

private fun BleConnectionState.toUiMessage(): String {
    return when (this) {
        BleConnectionState.Idle -> "Готово"
        BleConnectionState.CheckingPermissions -> "Проверка разрешений"
        BleConnectionState.Scanning -> "Поиск устройства"
        is BleConnectionState.DeviceFound -> "Найдено устройство: ${name}"
        BleConnectionState.Connecting -> "Подключение запускается..."
        BleConnectionState.DiscoveringServices -> "Поиск сервисов"
        BleConnectionState.Subscribing -> "Подписка на уведомления"
        BleConnectionState.Connected -> "Устройство подключено"
        is BleConnectionState.Failed -> "Ошибка подключения"
    }
}