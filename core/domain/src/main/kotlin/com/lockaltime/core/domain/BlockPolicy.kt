package com.lockaltime.core.domain

import com.lockaltime.core.model.ActiveSession

/**
 * Decides whether a foreground app must be blocked. Kept free of Android types so the rules can
 * grow (schedules, allow-lists, host-controlled rules) and stay unit-testable.
 *
 * @param protectedPackages packages that are never blocked, so the user can't lock themselves
 * out of the app that ends the session.
 */
class BlockPolicy(private val protectedPackages: Set<String>) {

    fun shouldBlock(session: ActiveSession?, packageName: String, nowMillis: Long): Boolean {
        if (session == null || session.isOver(nowMillis)) return false
        if (packageName in protectedPackages) return false
        return packageName in session.blockedPackages
    }
}
