package ru.aokruan.core.di

import ru.aokruan.network.HttpClientFactory
import ru.aokruan.network.NetworkConfig
import ru.aokruan.service.data.remote.api.KtorServiceApi
import ru.aokruan.service.data.repository.DefaultServiceRepository
import ru.aokruan.service.domain.GetMassagesPageUseCase
import ru.aokruan.service.domain.ServiceRepository

object Di {

    data class ServiceGraph(
        val repository: ServiceRepository,
        val getMassagesPage: GetMassagesPageUseCase
    )

    fun createServiceGraph(baseUrl: String, enableLogging: Boolean): ServiceGraph {
        val client = HttpClientFactory.create(
            NetworkConfig(baseUrl = baseUrl, enableLogging = enableLogging)
        )
        val api = KtorServiceApi(client)
        val repo: ServiceRepository = DefaultServiceRepository(api)
        val useCase = GetMassagesPageUseCase(repo)

        return ServiceGraph(
            repository = repo,
            getMassagesPage = useCase
        )
    }
}