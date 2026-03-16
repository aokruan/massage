package ru.aokruan.service.data.repository

import ru.aokruan.network.ApiOperation
import ru.aokruan.service.data.remote.api.KtorServiceApi
import ru.aokruan.service.domain.ServiceRepository
import ru.aokruan.service.domain.model.ServicePage
import ru.aokruan.service.model.ServiceItem

class DefaultServiceRepository(private val api: KtorServiceApi) : ServiceRepository {

    override suspend fun getMassages(page: Int, pageSize: Int): ApiOperation<ServicePage> =
        api.getMassages(page, pageSize)

    override suspend fun getMassageById(id: String): ApiOperation<ServiceItem> =
        api.getMassageById(id)
}