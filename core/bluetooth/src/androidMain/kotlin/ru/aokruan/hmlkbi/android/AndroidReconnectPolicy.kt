package ru.aokruan.hmlkbi.android

class AndroidReconnectPolicy {
    private val delaysMs = listOf(2_000L, 5_000L, 10_000L, 15_000L)

    fun delayForAttempt(attempt: Int): Long {
        return delaysMs.getOrElse(attempt.coerceAtLeast(0)) { delaysMs.last() }
    }
}
