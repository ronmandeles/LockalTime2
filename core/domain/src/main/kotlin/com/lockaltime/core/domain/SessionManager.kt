package com.lockaltime.core.domain

import com.lockaltime.core.common.Clock
import com.lockaltime.core.data.repository.ActiveSessionRepository
import com.lockaltime.core.data.repository.SessionRepository
import com.lockaltime.core.model.ActiveSession
import com.lockaltime.core.model.SessionInvite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Entry point for starting and stopping sessions.
 *
 * Every way of entering a session (starting a local one, or joining one shared from another
 * device) ends in an [ActiveSessionRepository] write, which is what the blocking engine observes.
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

    /**
     * Copies a session shared from another device into this one. It starts now and ends at the
     * host's end time, but stays independent afterwards: stopping either side doesn't affect the
     * other.
     */
    suspend fun join(invite: SessionInvite): JoinResult {
        val now = now()
        if (invite.isExpired(now)) return JoinResult.Expired
        var joined = false
        activeSessions.update { current ->
            joined = current == null || current.isOver(now)
            if (!joined) return@update current
            ActiveSession(
                sessionId = invite.sessionId,
                name = invite.name,
                blockedPackages = invite.blockedPackages,
                startedAtMillis = now,
                endsAtMillis = invite.endsAtMillis,
            )
        }
        return if (joined) JoinResult.Joined else JoinResult.SessionRunning
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
