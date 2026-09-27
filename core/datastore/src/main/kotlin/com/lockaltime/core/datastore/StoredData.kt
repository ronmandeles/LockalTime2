package com.lockaltime.core.datastore

import com.lockaltime.core.model.ActiveSession
import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.BlockedApp
import kotlinx.serialization.Serializable

/*
 * The on-disk JSON schema. Kept separate from the domain models in core:model so the models can
 * change without breaking stored data. Property names are the JSON keys: rename them only with a
 * migration.
 */

@Serializable
data class SessionsData(val sessions: List<SessionEntity> = emptyList())

@Serializable
data class ActiveSessionData(val session: ActiveSessionEntity? = null)

@Serializable
data class SessionEntity(
    val id: String,
    val name: String,
    val blockedApps: List<BlockedAppEntity>,
    val createdAtMillis: Long,
)

@Serializable
data class BlockedAppEntity(
    val packageName: String,
    val label: String,
)

@Serializable
data class ActiveSessionEntity(
    val sessionId: String,
    val name: String,
    val blockedPackages: Set<String>,
    val startedAtMillis: Long,
    val endsAtMillis: Long? = null,
)

fun SessionEntity.asExternalModel() = BlockSession(
    id = id,
    name = name,
    blockedApps = blockedApps.map { BlockedApp(it.packageName, it.label) },
    createdAtMillis = createdAtMillis,
)

fun BlockSession.asEntity() = SessionEntity(
    id = id,
    name = name,
    blockedApps = blockedApps.map { BlockedAppEntity(it.packageName, it.label) },
    createdAtMillis = createdAtMillis,
)

fun ActiveSessionEntity.asExternalModel() = ActiveSession(
    sessionId = sessionId,
    name = name,
    blockedPackages = blockedPackages,
    startedAtMillis = startedAtMillis,
    endsAtMillis = endsAtMillis,
)

fun ActiveSession.asEntity() = ActiveSessionEntity(
    sessionId = sessionId,
    name = name,
    blockedPackages = blockedPackages,
    startedAtMillis = startedAtMillis,
    endsAtMillis = endsAtMillis,
)
