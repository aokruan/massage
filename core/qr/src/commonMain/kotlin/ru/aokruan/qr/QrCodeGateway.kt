package ru.aokruan.qr

interface QrCodeGateway {
    suspend fun scanQr(): String?
}