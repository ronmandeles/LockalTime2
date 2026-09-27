package com.lockaltime.core.testing.repository

import com.lockaltime.core.data.repository.InstalledAppsRepository
import com.lockaltime.core.model.InstalledApp

class TestInstalledAppsRepository(
    var apps: List<InstalledApp> = emptyList(),
) : InstalledAppsRepository {

    override suspend fun getLaunchableApps(): List<InstalledApp> = apps
}
