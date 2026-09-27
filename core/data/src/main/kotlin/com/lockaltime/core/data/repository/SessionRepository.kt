package com.lockaltime.core.data.repository

import com.lockaltime.core.model.BlockSession
import kotlinx.coroutines.flow.Flow

/** The user's saved session configurations. */
interface SessionRepository {
    val sessions: Flow<List<BlockSession>>

    /** Emits null if there is no session with this [id], e.g. after it's deleted. */
    fun getSession(id: String): Flow<BlockSession?>

    suspend fun upsert(session: BlockSession)
    suspend fun delete(id: String)
}
