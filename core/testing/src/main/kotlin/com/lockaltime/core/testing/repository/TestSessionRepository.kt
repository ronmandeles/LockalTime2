package com.lockaltime.core.testing.repository

import com.lockaltime.core.data.repository.SessionRepository
import com.lockaltime.core.model.BlockSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class TestSessionRepository : SessionRepository {

    private val state = MutableStateFlow<List<BlockSession>>(emptyList())

    override val sessions: Flow<List<BlockSession>> = state

    override fun getSession(id: String): Flow<BlockSession?> =
        state.map { sessions -> sessions.find { it.id == id } }

    override suspend fun upsert(session: BlockSession) = state.update { sessions ->
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index == -1) sessions + session else sessions.toMutableList().apply { set(index, session) }
    }

    override suspend fun delete(id: String) = state.update { sessions -> sessions.filterNot { it.id == id } }
}
