package com.lockaltime.blocking

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.lockaltime.core.domain.BlockPolicy
import com.lockaltime.core.domain.SessionManager
import com.lockaltime.core.model.ActiveSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Watches which app comes to the foreground and covers blocked apps with [BlockedActivity].
 *
 * It only reacts to the active session, so it doesn't care whether that session was started
 * locally or joined from another device.
 */
@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject
    lateinit var sessionManager: SessionManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var policy: BlockPolicy

    // Only touched on the main thread: collected on Main, and events are delivered on Main.
    private var activeSession: ActiveSession? = null

    override fun onServiceConnected() {
        policy = BlockPolicy(protectedPackages = setOf(packageName))
        scope.launch {
            // collectLatest cancels the pending expiry when the session is stopped or replaced.
            sessionManager.activeSession.collectLatest { session ->
                activeSession = session
                val endsAt = session?.endsAtMillis ?: return@collectLatest
                // May fire late under Doze; shouldBlock already stops blocking on time. This only
                // clears the stored session.
                delay(endsAt - sessionManager.now())
                sessionManager.endIfExpired()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString() ?: return
        val session = activeSession ?: return
        if (!policy.shouldBlock(session, foregroundPackage, sessionManager.now())) return

        // Deliberately not debounced: apps often chain activities (splash -> main, first-run
        // screens), and each one can land on top of the block screen. BlockedActivity is
        // singleTask, so re-launching it just brings it back to the front.
        startActivity(
            BlockedActivity.intent(this, foregroundPackage, session.name, session.endsAtMillis)
        )
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
