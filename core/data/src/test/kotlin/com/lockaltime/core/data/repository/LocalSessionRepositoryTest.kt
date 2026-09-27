package com.lockaltime.core.data.repository

import com.lockaltime.core.datastore.SessionsData
import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.BlockedApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalSessionRepositoryTest {

    private val repository = LocalSessionRepository(InMemoryDataStore(SessionsData()))

    private val study = BlockSession("s1", "Study", listOf(BlockedApp("a.pkg", "A")), 0)
    private val work = BlockSession("s2", "Work", listOf(BlockedApp("b.pkg", "B")), 0)

    @Test
    fun `upsert adds new sessions in order`() = runTest {
        repository.upsert(study)
        repository.upsert(work)

        assertEquals(listOf(study, work), repository.sessions.first())
    }

    @Test
    fun `upsert replaces an existing session in place`() = runTest {
        repository.upsert(study)
        repository.upsert(work)

        val renamed = study.copy(name = "Exams")
        repository.upsert(renamed)

        assertEquals(listOf(renamed, work), repository.sessions.first())
    }

    @Test
    fun `getSession emits null once the session is deleted`() = runTest {
        repository.upsert(study)
        assertEquals(study, repository.getSession("s1").first())

        repository.delete("s1")

        assertNull(repository.getSession("s1").first())
    }
}
