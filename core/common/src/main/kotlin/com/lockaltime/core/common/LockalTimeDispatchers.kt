package com.lockaltime.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: LockalTimeDispatchers)

enum class LockalTimeDispatchers {
    Default,
    IO,
}

/** A scope that lives as long as the application, for work that must outlive any screen. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
