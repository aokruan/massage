package ru.aokruan.service.domain

import ru.aokruan.network.ApiOperation
import ru.aokruan.service.model.ServiceItem

class GetMassageByIdUseCase(
    private val repository: ServiceRepository
) {
    suspend operator fun invoke(id: String): ApiOperation<ServiceItem> =
        repository.getMassageById(id)
}