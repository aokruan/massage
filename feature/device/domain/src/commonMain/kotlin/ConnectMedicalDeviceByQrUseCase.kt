package ru.aokruan.hmlkbi.feature.device.domain

import ru.aokruan.hmlkbi.model.BleConnectionState

class ConnectMedicalDeviceByQrUseCase(
    private val repository: MedicalDeviceRepository,
) {
    suspend operator fun invoke(rawQr: String): BleConnectionState {
        return repository.connectByQr(rawQr)
    }
}