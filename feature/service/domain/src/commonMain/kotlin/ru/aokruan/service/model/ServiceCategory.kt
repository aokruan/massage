package ru.aokruan.service.model

enum class ServiceCategory {
    THAI, RELAX, AROMA, LYMPH_DRAIN, HOT_STONE, CLASSIC, SPORT, UNKNOWN;

    companion object {
        fun from(raw: String): ServiceCategory =
            runCatching { valueOf(raw) }.getOrElse { UNKNOWN }
    }
}