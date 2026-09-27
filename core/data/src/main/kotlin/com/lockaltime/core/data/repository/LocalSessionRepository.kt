package com.lockaltime.core.data.repository

import androidx.datastore.core.DataStore
import com.lockaltime.core.datastore.SessionEntity
import com.lockaltime.core.datastore.SessionsData
import com.lockaltime.core.datastore.asEntity
import com.lockaltime.core.datastore.asExternalModel
import com.lockaltime.core.model.BlockSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class LocalSessionRepository @Inject constructor(
    private val store: DataStore<SessionsData>,
) : SessionRepository {

    override val sessions: Flow<List<BlockSession>> =
        store.data.map { data -> data.sessions.map(SessionEntity::asExternalModel) }

    override fun getSession(id: String): Flow<BlockSession?> =
        store.data.map { data -> data.sessions.find { it.id == id }?.asExternalModel() }

    override suspend fun upsert(session: BlockSession) {
        val entity = session.asEntity()
        store.updateData { data ->
            val index = data.sessions.indexOfFirst { it.id == entity.id }
            val updated = if (index == -1) {
                data.sessions + entity
            } else {
                data.sessions.toMutableList().apply { set(index, entity) }
            }
            data.copy(sessions = updated)
        }
    }

    override suspend fun delete(id: String) {
        store.updateData { data -> data.copy(sessions = data.sessions.filterNot { it.id == id }) }
    }
}
