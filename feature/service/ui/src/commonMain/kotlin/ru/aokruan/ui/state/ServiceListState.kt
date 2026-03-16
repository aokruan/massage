package ru.aokruan.ui.state

import ru.aokruan.service.model.ServiceItem

data class ServiceListState(
    val items: List<ServiceItem> = emptyList(),
    val page: Int = 0,
    val totalPages: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)