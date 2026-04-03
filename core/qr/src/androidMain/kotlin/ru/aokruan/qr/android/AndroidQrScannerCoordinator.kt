package ru.aokruan.qr.android

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidQrScannerCoordinator {
    private var launcher: (((String?) -> Unit) -> Unit)? = null

    fun bind(block: ((String?) -> Unit) -> Unit) {
        launcher = block
    }

    suspend fun scan(): String? {
        val current = launcher ?: return null

        return suspendCancellableCoroutine { cont ->
            current { result ->
                if (cont.isActive) {
                    cont.resume(result)
                }
            }
        }
    }
}