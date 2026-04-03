package ru.aokruan.hmlkbi

import kotlinx.coroutines.CompletableDeferred

internal object IosQrRuntime {
    private var pendingScan: CompletableDeferred<String?>? = null
    var launchScanner: (() -> Unit)? = null

    fun beginScan(): CompletableDeferred<String?> {
        val deferred = CompletableDeferred<String?>()
        pendingScan = deferred
        launchScanner?.invoke()
        return deferred
    }

    fun completeScan(result: String?) {
        pendingScan?.complete(result)
        pendingScan = null
    }

    fun cancelScan() {
        pendingScan?.complete(null)
        pendingScan = null
    }
}

class IosQrInterop {
    fun registerLaunchScanner(block: () -> Unit) {
        IosQrRuntime.launchScanner = block
    }

    fun completeScan(result: String?) {
        IosQrRuntime.completeScan(result)
    }

    fun cancelScan() {
        IosQrRuntime.cancelScan()
    }
}