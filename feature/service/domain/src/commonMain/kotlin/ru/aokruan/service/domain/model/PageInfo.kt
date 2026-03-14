package ru.aokruan.service.domain.model

import ru.aokruan.service.model.ServiceItem

data class PageInfo(
    val count: Int,
    val pages: Int,
    val prev: String?,
)

data class ServicePage(
    val info: PageInfo,
    val results: List<ServiceItem>
)