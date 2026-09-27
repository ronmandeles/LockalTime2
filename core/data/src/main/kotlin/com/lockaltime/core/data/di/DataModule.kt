package com.lockaltime.core.data.di

import com.lockaltime.core.data.repository.ActiveSessionRepository
import com.lockaltime.core.data.repository.InstalledAppsRepository
import com.lockaltime.core.data.repository.LocalActiveSessionRepository
import com.lockaltime.core.data.repository.LocalSessionRepository
import com.lockaltime.core.data.repository.PackageManagerInstalledAppsRepository
import com.lockaltime.core.data.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Swap implementations here (e.g. a remote-backed [ActiveSessionRepository] for shared sessions)
 * without touching the UI or the blocking service.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    internal abstract fun bindsSessionRepository(
        repository: LocalSessionRepository,
    ): SessionRepository

    @Binds
    internal abstract fun bindsActiveSessionRepository(
        repository: LocalActiveSessionRepository,
    ): ActiveSessionRepository

    @Binds
    internal abstract fun bindsInstalledAppsRepository(
        repository: PackageManagerInstalledAppsRepository,
    ): InstalledAppsRepository
}
