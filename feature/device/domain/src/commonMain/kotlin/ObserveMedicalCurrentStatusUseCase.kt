package ru.aokruan.hmlkbi.feature.device.domain

class ObserveMedicalCurrentStatusUseCase(
    private val repository: MedicalDeviceRepository,
) {
    operator fun invoke() = repository.observeCurrentStatus()
}