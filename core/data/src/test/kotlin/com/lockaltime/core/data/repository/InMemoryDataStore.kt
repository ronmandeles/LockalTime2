package com.lockaltime.core.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [DataStore] without a file. Repository tests only need its update semantics; the on-disk
 * format is covered by the serializer tests in core:datastore.
 */
class InMemoryDataStore<T>(initialValue: T) : DataStore<T> {

    private val mutex = Mutex()

    override val data = MutableStateFlow(initialValue)

    override suspend fun updateData(transform: suspend (t: T) -> T): T = mutex.withLock {
        transform(data.value).also { data.value = it }
    }
}
