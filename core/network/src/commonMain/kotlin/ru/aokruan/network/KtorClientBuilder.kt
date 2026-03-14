package ru.aokruan.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun HttpClientConfig<*>.configureKtor(config: NetworkConfig) {
    defaultRequest {
        // https://api.example.com/
        url.takeFrom(config.baseUrl.ensureTrailingSlash())
    }

    install(HttpTimeout) {
        connectTimeoutMillis = config.connectTimeoutMs
        requestTimeoutMillis = config.requestTimeoutMs
        socketTimeoutMillis = config.socketTimeoutMs
    }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
                encodeDefaults = true
            }
        )
    }

    if (config.enableLogging) {
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.SIMPLE
        }
    }

    expectSuccess = false // важно: сами обрабатываем HTTP коды
}