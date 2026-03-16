package ru.aokruan.service.domain

import ru.aokruan.network.ApiOperation
import ru.aokruan.service.domain.model.ServicePage
import ru.aokruan.service.model.ServiceItem

interface ServiceRepository {
    suspend fun getMassages(page: Int, pageSize: Int): ApiOperation<ServicePage>
    suspend fun getMassageById(id: String): ApiOperation<ServiceItem>
}