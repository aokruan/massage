package ru.aokruan.hmlkbi.feature.device.domain

class DisconnectMedicalDeviceUseCase(
    private val repository: MedicalDeviceRepository,
) {
    suspend operator fun invoke() {
        repository.disconnect()
    }
}
