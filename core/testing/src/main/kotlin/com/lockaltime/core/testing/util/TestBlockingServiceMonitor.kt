package com.lockaltime.core.testing.util

import com.lockaltime.core.data.util.BlockingServiceMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestBlockingServiceMonitor(enabled: Boolean = true) : BlockingServiceMonitor {

    private val state = MutableStateFlow(enabled)

    override val isEnabled: Flow<Boolean> = state

    fun setEnabled(enabled: Boolean) {
        state.value = enabled
    }
}
