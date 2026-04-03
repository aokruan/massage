package ru.aokruan.hmlkbi

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ru.aokruan.hmlkbi.model.AlarmEvent

class AlarmHistoryParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(payload: String): List<AlarmEvent> {
        return runCatching {
            json.decodeFromString(ListSerializer(AlarmEvent.serializer()), payload)
        }.getOrDefault(emptyList())
    }
}