package com.lockaltime.feature.editor.impl

import com.lockaltime.core.model.InstalledApp

sealed interface SessionEditorUiState {
    /** Whether a new session is being created, as opposed to editing an existing one. */
    val isNew: Boolean

    data class Loading(override val isNew: Boolean) : SessionEditorUiState

    data class Editing(
        override val isNew: Boolean,
        val name: String,
        val query: String,
        val apps: List<InstalledApp>,
        val selectedPackages: Set<String>,
        /** Set once the session is stored; the screen then closes itself. */
        val isSaved: Boolean = false,
    ) : SessionEditorUiState {
        val visibleApps: List<InstalledApp>
            get() = if (query.isBlank()) {
                apps
            } else {
                apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
            }

        val canSave: Boolean
            get() = name.isNotBlank() && selectedPackages.isNotEmpty()
    }
}

sealed interface SessionEditorAction {
    data class NameChanged(val name: String) : SessionEditorAction
    data class QueryChanged(val query: String) : SessionEditorAction
    data class AppToggled(val packageName: String) : SessionEditorAction
    data object Save : SessionEditorAction
}
