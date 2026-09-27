package com.lockaltime.core.data.util

import kotlinx.coroutines.flow.Flow

/**
 * Whether the user has enabled the app-blocking service in system settings.
 *
 * The implementation lives in the `:blocking` module, next to the service it checks.
 */
interface BlockingServiceMonitor {
    val isEnabled: Flow<Boolean>
}
