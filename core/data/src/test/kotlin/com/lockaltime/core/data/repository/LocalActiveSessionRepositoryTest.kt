package com.lockaltime.core.data.repository

import com.lockaltime.core.datastore.ActiveSessionData
import com.lockaltime.core.model.ActiveSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalActiveSessionRepositoryTest {

    private val repository = LocalActiveSessionRepository(InMemoryDataStore(ActiveSessionData()))

    private val session = ActiveSession(
        sessionId = "s1",
        name = "Study",
        blockedPackages = setOf("a.pkg", "b.pkg"),
        startedAtMillis = 1_000,
        endsAtMillis = 2_000,
    )

    @Test
    fun `no session is active initially`() = runTest {
        assertNull(repository.activeSession.first())
    }

    @Test
    fun `setActive stores and clears the session`() = runTest {
        repository.setActive(session)
        assertEquals(session, repository.activeSession.first())

        repository.setActive(null)
        assertNull(repository.activeSession.first())
    }

    @Test
    fun `update transforms the current session`() = runTest {
        repository.setActive(session)

        repository.update { it?.copy(endsAtMillis = null) }

        assertEquals(session.copy(endsAtMillis = null), repository.activeSession.first())
    }
}
