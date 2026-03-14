package ru.aokruan.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual object HttpClientFactory {
    actual fun create(config: NetworkConfig): HttpClient =
        HttpClient(OkHttp) {
            configureKtor(config)
        }
}