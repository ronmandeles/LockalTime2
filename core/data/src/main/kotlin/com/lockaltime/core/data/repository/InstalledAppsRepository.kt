package com.lockaltime.core.data.repository

import com.lockaltime.core.model.InstalledApp

/**
 * A one-shot read rather than a Flow: PackageManager has no change stream, and the list is only
 * needed while picking apps.
 */
interface InstalledAppsRepository {
    /** Apps that appear in the launcher, excluding this app, sorted by label. */
    suspend fun getLaunchableApps(): List<InstalledApp>
}
