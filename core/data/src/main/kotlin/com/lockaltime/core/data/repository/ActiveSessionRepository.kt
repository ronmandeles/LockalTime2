package com.lockaltime.core.data.repository

import com.lockaltime.core.model.ActiveSession
import kotlinx.coroutines.flow.Flow

/** The single session currently enforced on this device, if any. */
interface ActiveSessionRepository {
    val activeSession: Flow<ActiveSession?>
    suspend fun setActive(session: ActiveSession?)

    /** Reads and replaces the active session as one atomic step. */
    suspend fun update(transform: (ActiveSession?) -> ActiveSession?)
}
