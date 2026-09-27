package com.lockaltime.core.model

/** An app installed on this device that the user can choose to block. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
