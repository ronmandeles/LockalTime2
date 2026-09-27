package com.lockaltime.blocking.di

import com.lockaltime.blocking.AccessibilityBlockingServiceMonitor
import com.lockaltime.core.data.util.BlockingServiceMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BlockingModule {

    @Binds
    internal abstract fun bindsBlockingServiceMonitor(
        monitor: AccessibilityBlockingServiceMonitor,
    ): BlockingServiceMonitor
}
