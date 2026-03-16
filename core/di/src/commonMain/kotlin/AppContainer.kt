package ru.aokruan.core.di

import ru.aokruan.network.HttpClientFactory
import ru.aokruan.network.NetworkConfig
import ru.aokruan.service.data.remote.api.KtorServiceApi
import ru.aokruan.service.data.repository.DefaultServiceRepository
import ru.aokruan.service.domain.GetMassageByIdUseCase
import ru.aokruan.service.domain.GetMassagesPageUseCase
import ru.aokruan.service.domain.ServiceRepository

object Di {

    data class AppGraph(
        val serviceGraph: ServiceGraph,
        // позже:
        // val authGraph: AuthGraph
    )

    data class ServiceGraph(
        val repository: ServiceRepository,
        val getMassagesPage: GetMassagesPageUseCase,
        val getMassageById: GetMassageByIdUseCase
    )

    fun createAppGraph(
        baseUrl: String,
        enableLogging: Boolean
    ): AppGraph {
        return AppGraph(
            serviceGraph = createServiceGraph(
                baseUrl = baseUrl,
                enableLogging = enableLogging
            )
        )
    }

    private fun createServiceGraph(
        baseUrl: String,
        enableLogging: Boolean
    ): ServiceGraph {
        val client = HttpClientFactory.create(
            NetworkConfig(
                baseUrl = baseUrl,
                enableLogging = enableLogging
            )
        )

        val api = KtorServiceApi(client)
        val repository: ServiceRepository = DefaultServiceRepository(api)

        return ServiceGraph(
            repository = repository,
            getMassagesPage = GetMassagesPageUseCase(repository),
            getMassageById = GetMassageByIdUseCase(repository)
        )
    }
}