package com.lockaltime.feature.editor.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockaltime.core.common.Clock
import com.lockaltime.core.data.repository.InstalledAppsRepository
import com.lockaltime.core.data.repository.SessionRepository
import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.BlockedApp
import com.lockaltime.feature.editor.impl.SessionEditorUiState.Editing
import com.lockaltime.feature.editor.impl.SessionEditorUiState.Loading
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** @param sessionId the session to edit, or null to create a new one. */
@HiltViewModel(assistedFactory = SessionEditorViewModel.Factory::class)
class SessionEditorViewModel @AssistedInject constructor(
    @Assisted private val sessionId: String?,
    private val sessionRepository: SessionRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val clock: Clock,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sessionId: String?): SessionEditorViewModel
    }

    private var existing: BlockSession? = null

    private val _uiState = MutableStateFlow<SessionEditorUiState>(Loading(isNew = sessionId == null))
    val uiState: StateFlow<SessionEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Loaded once: the form is a draft, so later changes to the stored session are ignored.
            existing = sessionId?.let { sessionRepository.getSession(it).first() }
            val selected = existing?.blockedApps?.mapTo(mutableSetOf()) { it.packageName }.orEmpty()
            // Show already-selected apps first when editing.
            val apps = installedAppsRepository.getLaunchableApps()
                .sortedByDescending { it.packageName in selected }
            _uiState.value = Editing(
                isNew = sessionId == null,
                name = existing?.name.orEmpty(),
                query = "",
                apps = apps,
                selectedPackages = selected,
            )
        }
    }

    fun onAction(action: SessionEditorAction) {
        when (action) {
            is SessionEditorAction.NameChanged -> updateEditing { it.copy(name = action.name) }
            is SessionEditorAction.QueryChanged -> updateEditing { it.copy(query = action.query) }
            is SessionEditorAction.AppToggled -> toggleApp(action.packageName)
            SessionEditorAction.Save -> save()
        }
    }

    private fun toggleApp(packageName: String) = updateEditing {
        val selected = if (packageName in it.selectedPackages) {
            it.selectedPackages - packageName
        } else {
            it.selectedPackages + packageName
        }
        it.copy(selectedPackages = selected)
    }

    private fun save() {
        val state = _uiState.value as? Editing ?: return
        if (!state.canSave) return
        viewModelScope.launch {
            sessionRepository.upsert(
                BlockSession(
                    id = sessionId ?: UUID.randomUUID().toString(),
                    name = state.name.trim(),
                    blockedApps = state.apps
                        .filter { it.packageName in state.selectedPackages }
                        .map { BlockedApp(it.packageName, it.label) },
                    createdAtMillis = existing?.createdAtMillis ?: clock.nowMillis(),
                )
            )
            updateEditing { it.copy(isSaved = true) }
        }
    }

    /** Input events can't arrive before the form is shown, so they're ignored while loading. */
    private fun updateEditing(transform: (Editing) -> Editing) =
        _uiState.update { if (it is Editing) transform(it) else it }
}
