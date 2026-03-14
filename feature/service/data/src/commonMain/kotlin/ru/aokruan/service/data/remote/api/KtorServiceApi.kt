package ru.aokruan.service.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ru.aokruan.network.ApiOperation
import ru.aokruan.network.mapSuccess
import ru.aokruan.network.safeRequest
import ru.aokruan.service.data.remote.dto.RemoteServicePage
import ru.aokruan.service.data.remote.mapper.toDomain
import ru.aokruan.service.domain.model.ServicePage

class KtorServiceApi(
    private val client: HttpClient
) {
    suspend fun getMassages(page: Int, pageSize: Int): ApiOperation<ServicePage> {
        return safeRequest<RemoteServicePage> {
            client.get("api/massages") {
                parameter("page", page)
                parameter("pageSize", pageSize)
            }
        }.mapSuccess { it.toDomain() }
    }
}