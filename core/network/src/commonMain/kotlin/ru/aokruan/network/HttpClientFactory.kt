package ru.aokruan.network

import io.ktor.client.HttpClient

expect object HttpClientFactory {
    fun create(config: NetworkConfig): HttpClient
}