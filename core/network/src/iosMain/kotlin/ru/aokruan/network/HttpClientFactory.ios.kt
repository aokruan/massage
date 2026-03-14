package ru.aokruan.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual object HttpClientFactory {
    actual fun create(config: NetworkConfig): HttpClient =
        HttpClient(Darwin) {
            configureKtor(config)
        }
}