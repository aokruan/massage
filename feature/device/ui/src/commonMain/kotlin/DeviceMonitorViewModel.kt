package ru.aokruan.hmlkbi.feature.device.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.aokruan.hmlkbi.BleSessionState
import ru.aokruan.hmlkbi.model.BleConnectionState
import ru.aokruan.hmlkbi.model.BleConnectError
import ru.aokruan.hmlkbi.model.AlarmEvent
import ru.aokruan.hmlkbi.feature.device.domain.AcknowledgeAlarmUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ConnectMedicalDeviceByQrUseCase
import ru.aokruan.hmlkbi.feature.device.domain.DisconnectMedicalDeviceUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalAlarmEventsUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalAlarmHistoryUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalCurrentStatusUseCase
import ru.aokruan.hmlkbi.feature.device.domain.ObserveMedicalSessionStateUseCase
import ru.aokruan.qr.QrCodeGateway

class DeviceMonitorViewModel(
    private val qrCodeGateway: QrCodeGateway,
    private val connectMedicalDeviceByQrUseCase: ConnectMedicalDeviceByQrUseCase,
    private val observeMedicalCurrentStatusUseCase: ObserveMedicalCurrentStatusUseCase,
    private val observeMedicalAlarmEventsUseCase: ObserveMedicalAlarmEventsUseCase,
    private val observeMedicalAlarmHistoryUseCase: ObserveMedicalAlarmHistoryUseCase,
    private val observeMedicalSessionStateUseCase: ObserveMedicalSessionStateUseCase,
    private val acknowledgeAlarmUseCase: AcknowledgeAlarmUseCase,
    private val disconnectMedicalDeviceUseCase: DisconnectMedicalDeviceUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(DeviceMonitorUiState())
    val state: StateFlow<DeviceMonitorUiState> = _state

    init {
        observeStreams()
    }

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
        }
    }

    fun onAcknowledgeAlarmClicked() {
        val alarmId = _state.value.lastAlarm?.id ?: return
        scope.launch {
            acknowledgeAlarmUseCase(alarmId)
        }
    }

    fun onDisconnectClicked() {
        scope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    message = "Отключение...",
                )
            }

            disconnectMedicalDeviceUseCase()
        }
    }

    private fun observeStreams() {
        scope.launch {
            observeMedicalCurrentStatusUseCase().collect { status ->
                _state.update { current ->
                    current.copy(
                        currentStatus = status,
                    )
                }
            }
        }

        scope.launch {
            observeMedicalAlarmEventsUseCase().collect { alarm ->
                _state.update { current ->
                    current.copy(
                        lastAlarm = alarm,
                        alarmHistory = current.alarmHistory.mergeAlarm(alarm),
                    )
                }
            }
        }

        scope.launch {
            observeMedicalAlarmHistoryUseCase().collect { history ->
                _state.update { current ->
                    current.copy(
                        alarmHistory = history,
                        lastAlarm = current.lastAlarm ?: history.firstOrNull(),
                    )
                }
            }
        }

        scope.launch {
            observeMedicalSessionStateUseCase().collect { sessionState ->
                _state.update { current ->
                    if (sessionState == BleSessionState.Idle) {
                        DeviceMonitorUiState(
                            message = sessionState.toUiMessage(),
                        )
                    } else {
                        current.copy(
                            connectionState = sessionState.toConnectionState(),
                            currentStatus = if (sessionState == BleSessionState.Ready) {
                                current.currentStatus
                            } else {
                                null
                            },
                            isLoading = sessionState.isLoading(),
                            message = sessionState.toUiMessage(),
                        )
                    }
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

private fun BleSessionState.toConnectionState(): BleConnectionState {
    return when (this) {
        BleSessionState.Idle -> BleConnectionState.Idle
        BleSessionState.Scanning -> BleConnectionState.Scanning
        BleSessionState.Connecting -> BleConnectionState.Connecting
        BleSessionState.DiscoveringServices -> BleConnectionState.DiscoveringServices
        BleSessionState.NegotiatingMtu,
        BleSessionState.SubscribingCurrentStatus,
        BleSessionState.SubscribingAlarmEvent,
        BleSessionState.ReadingAlarmHistory,
        BleSessionState.Reconnecting,
        BleSessionState.Ready
        -> BleConnectionState.Connected

        is BleSessionState.Failed -> BleConnectionState.Failed(
            BleConnectError.Platform(message),
        )
    }
}

private fun BleSessionState.toUiMessage(): String {
    return when (this) {
        BleSessionState.Idle -> "Готово к подключению"
        BleSessionState.Scanning -> "Поиск устройства..."
        BleSessionState.Connecting -> "Подключение к устройству..."
        BleSessionState.DiscoveringServices -> "Поиск BLE-сервисов..."
        BleSessionState.NegotiatingMtu -> "Настройка BLE-канала..."
        BleSessionState.SubscribingCurrentStatus -> "Подписка на текущий статус..."
        BleSessionState.SubscribingAlarmEvent -> "Подписка на тревоги..."
        BleSessionState.ReadingAlarmHistory -> "Загрузка истории тревог..."
        BleSessionState.Ready -> "Устройство подключено"
        BleSessionState.Reconnecting -> "Переподключение к устройству..."
        is BleSessionState.Failed -> "Ошибка подключения: $message"
    }
}

private fun BleSessionState.isLoading(): Boolean {
    return when (this) {
        BleSessionState.Idle,
        BleSessionState.Ready,
        is BleSessionState.Failed
        -> false

        else -> true
    }
}

private fun List<AlarmEvent>.mergeAlarm(alarm: AlarmEvent): List<AlarmEvent> {
    val updated = filterNot { it.id == alarm.id }
    return listOf(alarm) + updated
}
