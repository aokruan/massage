package ru.aokruan.qr.android

import ru.aokruan.qr.QrCodeGateway

class AndroidQrCodeGateway(
    private val coordinator: AndroidQrScannerCoordinator,
) : QrCodeGateway {
    override suspend fun scanQr(): String? = coordinator.scan()
}