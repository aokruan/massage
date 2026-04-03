package ru.aokruan.qr

import kotlinx.serialization.json.Json
import ru.aokruan.hmlkbi.model.BleQrPayload

class BleQrParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(raw: String): Result<BleQrPayload> {
        return runCatching {
            json.decodeFromString(BleQrPayload.serializer(), raw)
        }.mapCatching { payload ->
            require(payload.isBle()) { "QR type is not ble" }
            require(payload.serviceUuid.isNotBlank()) { "svc is blank" }
            require(payload.name.isNotBlank()) { "name is blank" }
            payload
        }
    }
}