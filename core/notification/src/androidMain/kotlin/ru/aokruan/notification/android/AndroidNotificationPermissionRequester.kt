package ru.aokruan.notification.android

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidNotificationPermissionRequester {
    private var launcher: (((Boolean) -> Unit) -> Unit)? = null

    fun bind(block: ((Boolean) -> Unit) -> Unit) {
        launcher = block
    }

    suspend fun request(): Boolean {
        val current = launcher ?: return false
        return suspendCancellableCoroutine { cont ->
            current { granted ->
                if (cont.isActive) cont.resume(granted)
            }
        }
    }
}