package ru.aokruan.service.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RemoteServicePage(
    val info: RemotePageInfo,
    val results: List<RemoteServiceItem>,
)

@Serializable
data class RemotePageInfo(
    val count: Int,
    val pages: Int,
    val prev: String? = null,
)

@Serializable
data class RemoteServiceItem(
    val id: String,
    val category: String,
    val title: String,
    val durationMinutes: Int,
    val priceMinor: Long,
    val description: String,
    val tags: List<String> = emptyList(),
)