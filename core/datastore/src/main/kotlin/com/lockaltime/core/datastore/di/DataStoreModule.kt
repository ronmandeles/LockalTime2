package com.lockaltime.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.lockaltime.core.common.ApplicationScope
import com.lockaltime.core.common.Dispatcher
import com.lockaltime.core.common.LockalTimeDispatchers.IO
import com.lockaltime.core.datastore.ActiveSessionData
import com.lockaltime.core.datastore.ActiveSessionDataSerializer
import com.lockaltime.core.datastore.SessionsData
import com.lockaltime.core.datastore.SessionsDataSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * One DataStore per file for the whole process: the UI and the blocking service share these
 * instances, so they always see the same data.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providesSessionsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<SessionsData> = DataStoreFactory.create(
        serializer = SessionsDataSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { SessionsData() },
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        produceFile = { context.dataStoreFile("sessions.json") },
    )

    @Provides
    @Singleton
    fun providesActiveSessionDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<ActiveSessionData> = DataStoreFactory.create(
        serializer = ActiveSessionDataSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { ActiveSessionData() },
        scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        produceFile = { context.dataStoreFile("active_session.json") },
    )
}
