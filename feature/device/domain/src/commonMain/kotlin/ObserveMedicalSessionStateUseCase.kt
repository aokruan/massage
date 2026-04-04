package ru.aokruan.hmlkbi.feature.device.domain

class ObserveMedicalSessionStateUseCase(
    private val repository: MedicalDeviceRepository,
) {
    operator fun invoke() = repository.observeSessionState()
}
