package com.lockaltime.core.domain

enum class JoinResult {
    Joined,

    /** The shared session's end time has already passed. */
    Expired,

    /** Another session is still running on this device. */
    SessionRunning,
}
