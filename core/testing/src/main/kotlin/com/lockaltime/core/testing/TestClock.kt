package com.lockaltime.core.testing

import com.lockaltime.core.common.Clock

class TestClock(var nowMillis: Long = 1_000L) : Clock {
    override fun nowMillis(): Long = nowMillis
}
