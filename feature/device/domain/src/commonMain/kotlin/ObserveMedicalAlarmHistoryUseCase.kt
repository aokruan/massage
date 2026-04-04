package ru.aokruan.hmlkbi.feature.device.domain

class ObserveMedicalAlarmHistoryUseCase(
    private val repository: MedicalDeviceRepository,
) {
    operator fun invoke() = repository.observeAlarmHistory()
}
