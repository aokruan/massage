package ru.aokruan.ui.mapper

import ru.aokruan.service.model.ServiceCategory

val ServiceCategory.title: String
    get() = when (this) {
        ServiceCategory.THAI -> "Тайский"
        ServiceCategory.RELAX -> "Расслабляющий"
        ServiceCategory.AROMA -> "Арома"
        ServiceCategory.LYMPH_DRAIN -> "Лимфодренажный"
        ServiceCategory.HOT_STONE -> "Стоун"
        ServiceCategory.CLASSIC -> "Классический"
        ServiceCategory.SPORT -> "Спортивный"
        ServiceCategory.UNKNOWN -> ""
    }

fun formatPrice(priceMinor: Long): String = "${priceMinor / 100} ₽"

fun formatDuration(minutes: Int): String = "$minutes мин"