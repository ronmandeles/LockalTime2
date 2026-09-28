package com.lockaltime.core.testing.util

import com.lockaltime.core.data.util.BlockingServiceMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestBlockingServiceMonitor(enabled: Boolean = true) : BlockingServiceMonitor {

    private val state = MutableStateFlow(enabled)

    override val isEnabled: Flow<Boolean> = state

    override val serviceComponent: String = "com.lockaltime/com.lockaltime.blocking.AppBlockerService"

    fun setEnabled(enabled: Boolean) {
        state.value = enabled
    }
}
