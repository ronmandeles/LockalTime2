package com.lockaltime.core.data.repository

import androidx.datastore.core.DataStore
import com.lockaltime.core.datastore.ActiveSessionData
import com.lockaltime.core.datastore.asEntity
import com.lockaltime.core.datastore.asExternalModel
import com.lockaltime.core.model.ActiveSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Persisted so blocking survives the app process being killed and the service restarting. */
internal class LocalActiveSessionRepository @Inject constructor(
    private val store: DataStore<ActiveSessionData>,
) : ActiveSessionRepository {

    override val activeSession: Flow<ActiveSession?> =
        store.data.map { it.session?.asExternalModel() }

    override suspend fun setActive(session: ActiveSession?) {
        store.updateData { it.copy(session = session?.asEntity()) }
    }

    // DataStore runs updates one at a time, so the transform sees the latest value.
    override suspend fun update(transform: (ActiveSession?) -> ActiveSession?) {
        store.updateData { data ->
            data.copy(session = transform(data.session?.asExternalModel())?.asEntity())
        }
    }
}
