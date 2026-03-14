package ru.aokruan.service.domain

import ru.aokruan.network.ApiOperation
import ru.aokruan.service.domain.model.ServicePage

interface ServiceRepository {
    suspend fun getMassages(page: Int, pageSize: Int): ApiOperation<ServicePage>
}