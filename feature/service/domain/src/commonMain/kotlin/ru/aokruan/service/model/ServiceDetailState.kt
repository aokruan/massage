package ru.aokruan.service.model

data class ServiceDetailState(
    val isLoading: Boolean = false,
    val item: ServiceItem? = null,
    val error: String? = null
)