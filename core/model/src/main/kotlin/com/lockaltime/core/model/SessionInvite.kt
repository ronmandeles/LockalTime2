package com.lockaltime.core.model

/**
 * A running session as shared with another device, e.g. through a QR code.
 *
 * Joining copies it into that device's own [ActiveSession], with no link back to the host: the
 * host stopping early doesn't end it there. The end time is absolute, so everyone who joins
 * unblocks at the same moment.
 *
 * @param endsAtMillis when the session ends, or null if each person runs it until they stop it.
 */
data class SessionInvite(
    val sessionId: String,
    val name: String,
    val blockedPackages: Set<String>,
    val endsAtMillis: Long? = null,
) {
    fun isExpired(nowMillis: Long): Boolean = endsAtMillis != null && nowMillis >= endsAtMillis
}

fun ActiveSession.toInvite() = SessionInvite(
    sessionId = sessionId,
    name = name,
    blockedPackages = blockedPackages,
    endsAtMillis = endsAtMillis,
)
