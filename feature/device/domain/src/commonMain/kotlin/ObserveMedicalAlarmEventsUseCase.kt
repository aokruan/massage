package ru.aokruan.hmlkbi.feature.device.domain

class ObserveMedicalAlarmEventsUseCase(
    private val repository: MedicalDeviceRepository,
) {
    operator fun invoke() = repository.observeAlarmEvents()
}