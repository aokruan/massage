package ru.aokruan.service.model

data class ServiceItem(
    val id: String,
    val category: ServiceCategory,
    val title: String,
    val durationMinutes: Int,
    val priceMinor: Long,
    val description: String,
    val tags: List<String>,
)