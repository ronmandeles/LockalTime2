package com.lockaltime.core.invite

import com.lockaltime.core.model.SessionInvite
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The JSON inside an invite code. Kept separate from SessionInvite so the domain model can change
 * without breaking codes printed or shown by other app versions. Keys are short because they end up
 * in a QR code: rename them only with a new version.
 */

@Serializable
internal data class InviteWire(
    @SerialName("v") val version: Int,
    @SerialName("id") val sessionId: String,
    @SerialName("n") val name: String,
    @SerialName("p") val blockedPackages: List<String>,
    @SerialName("e") val endsAtMillis: Long? = null,
)

// Sorted so the same session always produces the same code.
internal fun SessionInvite.asWire(version: Int) = InviteWire(
    version = version,
    sessionId = sessionId,
    name = name,
    blockedPackages = blockedPackages.sorted(),
    endsAtMillis = endsAtMillis,
)

internal fun InviteWire.asExternalModel() = SessionInvite(
    sessionId = sessionId,
    name = name,
    blockedPackages = blockedPackages.toSet(),
    endsAtMillis = endsAtMillis,
)
