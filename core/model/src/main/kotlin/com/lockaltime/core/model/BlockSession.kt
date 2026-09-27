package com.lockaltime.core.model

/**
 * A reusable blocking configuration the user creates, e.g. "Study" or "Deep work".
 * Creating a session doesn't block anything; starting it produces an [ActiveSession].
 */
data class BlockSession(
    val id: String,
    val name: String,
    val blockedApps: List<BlockedApp>,
    val createdAtMillis: Long,
)

/** An app targeted by a session, identified by its Android package name. */
data class BlockedApp(
    val packageName: String,
    val label: String,
)
