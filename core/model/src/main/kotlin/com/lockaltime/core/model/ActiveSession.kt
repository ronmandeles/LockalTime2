package com.lockaltime.core.model

/**
 * The session currently enforced on this device.
 *
 * This is a snapshot, deliberately decoupled from [BlockSession]: editing a saved session doesn't
 * change a running one, and a session joined from another device (e.g. by scanning a QR code)
 * will not exist in the local session list at all. The blocking engine only ever reads this.
 *
 * @param endsAtMillis when the session ends by itself, or null if it runs until stopped.
 */
data class ActiveSession(
    val sessionId: String,
    val name: String,
    val blockedPackages: Set<String>,
    val startedAtMillis: Long,
    val endsAtMillis: Long? = null,
) {
    /**
     * Expiry is decided by comparing with the clock, not by a timer firing, so a session is over
     * on time even if nothing has cleared it from storage yet.
     */
    fun isOver(nowMillis: Long): Boolean = endsAtMillis != null && nowMillis >= endsAtMillis
}
