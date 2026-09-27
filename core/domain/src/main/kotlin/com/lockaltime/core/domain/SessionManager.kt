package com.lockaltime.core.domain

import com.lockaltime.core.common.Clock
import com.lockaltime.core.data.repository.ActiveSessionRepository
import com.lockaltime.core.data.repository.SessionRepository
import com.lockaltime.core.model.ActiveSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Entry point for starting and stopping sessions.
 *
 * Every way of entering a session (starting a local one today, joining a remote one via QR later)
 * should end in an [ActiveSessionRepository] write, which is what the blocking engine observes.
 *
 * The stored session may be over (see [ActiveSession.isOver]) before [endIfExpired] clears it, so
 * readers should check it against [now].
 */
class SessionManager @Inject constructor(
    private val sessions: SessionRepository,
    private val activeSessions: ActiveSessionRepository,
    private val clock: Clock,
) {
    val activeSession: Flow<ActiveSession?> = activeSessions.activeSession

    fun now(): Long = clock.nowMillis()

    /**
     * @param duration how long to block for, or null to run until [stop] is called.
     * @return false if the session no longer exists or another session is still running.
     */
    suspend fun start(sessionId: String, duration: Duration?): Boolean {
        val session = sessions.getSession(sessionId).first() ?: return false
        val now = now()
        var started = false
        activeSessions.update { current ->
            started = current == null || current.isOver(now)
            if (!started) return@update current
            ActiveSession(
                sessionId = session.id,
                name = session.name,
                blockedPackages = session.blockedApps.mapTo(mutableSetOf()) { it.packageName },
                startedAtMillis = now,
                endsAtMillis = duration?.let { now + it.inWholeMilliseconds },
            )
        }
        return started
    }

    suspend fun stop() {
        activeSessions.setActive(null)
    }

    /** Clears the active session only if its time is up, so it never ends a newer session. */
    suspend fun endIfExpired() {
        val now = now()
        activeSessions.update { current -> current?.takeUnless { it.isOver(now) } }
    }
}
