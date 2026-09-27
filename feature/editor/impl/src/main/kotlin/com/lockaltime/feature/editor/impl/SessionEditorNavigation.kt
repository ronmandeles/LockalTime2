package com.lockaltime.feature.editor.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.lockaltime.feature.editor.api.SessionEditorRoute

/** @param onDone called when the user leaves the editor, whether or not they saved. */
fun NavGraphBuilder.sessionEditorScreen(onDone: () -> Unit) {
    composable<SessionEditorRoute> { entry ->
        SessionEditorScreen(
            sessionId = entry.toRoute<SessionEditorRoute>().sessionId,
            onDone = onDone,
        )
    }
}
