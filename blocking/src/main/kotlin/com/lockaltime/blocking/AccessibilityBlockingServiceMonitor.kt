package com.lockaltime.blocking

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.lockaltime.core.data.util.BlockingServiceMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Watches the system's list of enabled accessibility services, so the UI updates as soon as the
 * user returns from toggling [AppBlockerService] in Settings.
 */
internal class AccessibilityBlockingServiceMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : BlockingServiceMonitor {

    override val serviceComponent: String =
        ComponentName(context, AppBlockerService::class.java).flattenToString()

    override val isEnabled: Flow<Boolean> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(isServiceEnabled())
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            observer,
        )
        trySend(isServiceEnabled())
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
        .distinctUntilChanged()
        .conflate()

    private fun isServiceEnabled(): Boolean {
        val expected = ComponentName(context, AppBlockerService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}
