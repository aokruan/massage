package ru.aokruan.service.domain


import ru.aokruan.network.ApiOperation
import ru.aokruan.service.domain.model.ServicePage

class GetMassagesPageUseCase(
    private val repository: ServiceRepository
) {
    suspend operator fun invoke(page: Int, pageSize: Int): ApiOperation<ServicePage> =
        repository.getMassages(page, pageSize)
}