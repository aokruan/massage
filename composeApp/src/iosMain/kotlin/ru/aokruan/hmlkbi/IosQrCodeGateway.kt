package ru.aokruan.hmlkbi

import ru.aokruan.qr.QrCodeGateway

class IosQrCodeGateway : QrCodeGateway {
    override suspend fun scanQr(): String? {
        return IosQrRuntime.beginScan().await()
    }
}