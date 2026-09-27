package com.lockaltime.core.domain

import com.lockaltime.core.model.ActiveSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPolicyTest {

    private val policy = BlockPolicy(protectedPackages = setOf("com.lockaltime"))
    private val session = ActiveSession(
        sessionId = "s1",
        name = "Study",
        blockedPackages = setOf("com.instagram.android", "com.lockaltime"),
        startedAtMillis = 0,
    )

    @Test
    fun `blocks apps in the active session`() {
        assertTrue(policy.shouldBlock(session, "com.instagram.android", NOW))
    }

    @Test
    fun `allows apps not in the active session`() {
        assertFalse(policy.shouldBlock(session, "com.google.android.apps.maps", NOW))
    }

    @Test
    fun `allows everything when no session is active`() {
        assertFalse(policy.shouldBlock(null, "com.instagram.android", NOW))
    }

    @Test
    fun `never blocks protected packages`() {
        assertFalse(policy.shouldBlock(session, "com.lockaltime", NOW))
    }

    @Test
    fun `blocks a timed session until it ends`() {
        val timed = session.copy(endsAtMillis = 5_000)
        assertTrue(policy.shouldBlock(timed, "com.instagram.android", nowMillis = 4_999))
    }

    @Test
    fun `allows everything once a timed session ends`() {
        val timed = session.copy(endsAtMillis = 5_000)
        assertFalse(policy.shouldBlock(timed, "com.instagram.android", nowMillis = 5_000))
        assertFalse(policy.shouldBlock(timed, "com.instagram.android", nowMillis = 60_000))
    }

    @Test
    fun `open-ended session never expires`() {
        assertTrue(policy.shouldBlock(session, "com.instagram.android", nowMillis = Long.MAX_VALUE))
    }

    private companion object {
        const val NOW = 1_000L
    }
}
