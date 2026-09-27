package com.lockaltime.core.common

/** Wall-clock time, injected so time-dependent logic can be tested with a fake. */
fun interface Clock {
    fun nowMillis(): Long
}
