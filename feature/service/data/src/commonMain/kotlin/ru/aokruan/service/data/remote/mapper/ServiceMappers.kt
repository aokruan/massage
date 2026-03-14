package ru.aokruan.service.data.remote.mapper

import ru.aokruan.service.data.remote.dto.RemotePageInfo
import ru.aokruan.service.data.remote.dto.RemoteServiceItem
import ru.aokruan.service.data.remote.dto.RemoteServicePage
import ru.aokruan.service.domain.model.PageInfo
import ru.aokruan.service.domain.model.ServicePage
import ru.aokruan.service.model.ServiceCategory
import ru.aokruan.service.model.ServiceItem

fun RemoteServiceItem.toDomain(): ServiceItem =
    ServiceItem(
        id = id,
        category = ServiceCategory.from(category),
        title = title,
        durationMinutes = durationMinutes,
        priceMinor = priceMinor,
        description = description,
        tags = tags
    )

fun RemotePageInfo.toDomain(): PageInfo =
    PageInfo(
        count = count,
        pages = pages,
        prev = prev
    )

fun RemoteServicePage.toDomain(): ServicePage =
    ServicePage(
        info = info.toDomain(),
        results = results.map { it.toDomain() }
    )