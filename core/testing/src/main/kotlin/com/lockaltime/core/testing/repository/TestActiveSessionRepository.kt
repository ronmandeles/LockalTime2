package com.lockaltime.core.testing.repository

import com.lockaltime.core.data.repository.ActiveSessionRepository
import com.lockaltime.core.model.ActiveSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class TestActiveSessionRepository : ActiveSessionRepository {

    private val state = MutableStateFlow<ActiveSession?>(null)

    override val activeSession: Flow<ActiveSession?> = state

    override suspend fun setActive(session: ActiveSession?) {
        state.value = session
    }

    override suspend fun update(transform: (ActiveSession?) -> ActiveSession?) = state.update(transform)
}
