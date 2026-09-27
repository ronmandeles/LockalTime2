package com.lockaltime.core.domain

import com.lockaltime.core.model.ActiveSession
import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.BlockedApp
import com.lockaltime.core.testing.TestClock
import com.lockaltime.core.testing.repository.TestActiveSessionRepository
import com.lockaltime.core.testing.repository.TestSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class SessionManagerTest {

    private val sessions = TestSessionRepository()
    private val active = TestActiveSessionRepository()
    private val clock = TestClock(nowMillis = 1_000L)
    private val manager = SessionManager(sessions, active, clock)

    private val study = BlockSession("s1", "Study", listOf(BlockedApp("a.pkg", "A")), 0)
    private val work = BlockSession("s2", "Work", listOf(BlockedApp("b.pkg", "B")), 0)

    @Test
    fun `start snapshots the session into the active session`() = runTest {
        sessions.upsert(
            BlockSession(
                id = "s1",
                name = "Study",
                blockedApps = listOf(BlockedApp("a.pkg", "A"), BlockedApp("b.pkg", "B")),
                createdAtMillis = 0,
            )
        )

        assertTrue(manager.start("s1", duration = null))

        assertEquals(
            ActiveSession("s1", "Study", setOf("a.pkg", "b.pkg"), startedAtMillis = 1_000L),
            manager.activeSession.first(),
        )
    }

    @Test
    fun `start with a duration sets the end time`() = runTest {
        sessions.upsert(study)

        manager.start("s1", 25.minutes)

        assertEquals(1_000L + 25 * 60_000L, manager.activeSession.first()?.endsAtMillis)
    }

    @Test
    fun `start returns false for an unknown session`() = runTest {
        assertFalse(manager.start("missing", duration = null))
        assertNull(manager.activeSession.first())
    }

    @Test
    fun `start does not replace a running session`() = runTest {
        sessions.upsert(study)
        sessions.upsert(work)
        manager.start("s1", 25.minutes)

        assertFalse(manager.start("s2", duration = null))

        assertEquals("s1", manager.activeSession.first()?.sessionId)
    }

    @Test
    fun `start replaces a session whose time is up`() = runTest {
        sessions.upsert(study)
        sessions.upsert(work)
        manager.start("s1", 25.minutes)
        clock.nowMillis += 25 * 60_000L

        assertTrue(manager.start("s2", duration = null))

        assertEquals("s2", manager.activeSession.first()?.sessionId)
    }

    @Test
    fun `stop clears the active session`() = runTest {
        sessions.upsert(study)
        manager.start("s1", duration = null)

        manager.stop()

        assertNull(manager.activeSession.first())
    }

    @Test
    fun `endIfExpired clears a session whose time is up`() = runTest {
        sessions.upsert(study)
        manager.start("s1", 25.minutes)
        clock.nowMillis += 25 * 60_000L

        manager.endIfExpired()

        assertNull(manager.activeSession.first())
    }

    @Test
    fun `endIfExpired keeps a running session`() = runTest {
        sessions.upsert(study)
        manager.start("s1", 25.minutes)
        clock.nowMillis += 25 * 60_000L - 1

        manager.endIfExpired()

        assertEquals("s1", manager.activeSession.first()?.sessionId)
    }

    @Test
    fun `endIfExpired keeps an open-ended session`() = runTest {
        sessions.upsert(study)
        manager.start("s1", duration = null)
        clock.nowMillis = Long.MAX_VALUE

        manager.endIfExpired()

        assertEquals("s1", manager.activeSession.first()?.sessionId)
    }
}
