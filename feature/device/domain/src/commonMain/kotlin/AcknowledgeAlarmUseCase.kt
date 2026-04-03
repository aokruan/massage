package ru.aokruan.hmlkbi.feature.device.domain

class AcknowledgeAlarmUseCase(
    private val repository: MedicalDeviceRepository,
) {
    suspend operator fun invoke(alarmId: Long): Result<Unit> {
        return repository.acknowledgeAlarm(alarmId)
    }
}