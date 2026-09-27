package com.lockaltime.feature.editor.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

/** @param sessionId the session to edit, or null to create a new one. */
@Serializable
data class SessionEditorRoute(val sessionId: String? = null)

fun NavController.navigateToSessionEditor(sessionId: String? = null) =
    navigate(SessionEditorRoute(sessionId))
