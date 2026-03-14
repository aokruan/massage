package ru.aokruan.network

data class NetworkConfig(
    val baseUrl: String,
    val enableLogging: Boolean = false,
    val connectTimeoutMs: Long = 15_000,
    val requestTimeoutMs: Long = 30_000,
    val socketTimeoutMs: Long = 30_000,
)

internal fun String.ensureTrailingSlash(): String =
    if (endsWith("/")) this else "$this/"