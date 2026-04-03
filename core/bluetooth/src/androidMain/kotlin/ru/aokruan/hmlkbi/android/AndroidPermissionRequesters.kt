package ru.aokruan.hmlkbi.android

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidMultiplePermissionRequester {

    private var launcher: ((Array<String>, (Boolean) -> Unit) -> Unit)? = null

    fun bind(block: (Array<String>, (Boolean) -> Unit) -> Unit) {
        launcher = block
    }

    suspend fun request(permissions: Array<String>): Boolean {
        val currentLauncher = launcher ?: return false

        return suspendCancellableCoroutine { cont ->
            currentLauncher(permissions) { granted ->
                if (cont.isActive) {
                    cont.resume(granted)
                }
            }
        }
    }
}