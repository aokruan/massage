package ru.aokruan.service.data.repository

import ru.aokruan.network.ApiOperation
import ru.aokruan.service.data.remote.api.KtorServiceApi
import ru.aokruan.service.domain.ServiceRepository
import ru.aokruan.service.domain.model.ServicePage

class DefaultServiceRepository(private val api: KtorServiceApi) : ServiceRepository {

    override suspend fun getMassages(page: Int, pageSize: Int): ApiOperation<ServicePage> =
        api.getMassages(page, pageSize)
}