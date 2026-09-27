package com.lockaltime.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockaltime.core.data.repository.SessionRepository
import com.lockaltime.core.data.util.BlockingServiceMonitor
import com.lockaltime.core.domain.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager,
    blockingServiceMonitor: BlockingServiceMonitor,
) : ViewModel() {

    private val dialog = MutableStateFlow<HomeDialog?>(null)

    // Drives the countdown; stops with the rest of uiState when the screen isn't observed.
    private val now = flow {
        while (true) {
            emit(sessionManager.now())
            delay(1_000)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.sessions,
        sessionManager.activeSession,
        blockingServiceMonitor.isEnabled,
        dialog,
        now,
    ) { sessions, active, serviceEnabled, dialog, now ->
        val running = active?.takeUnless { it.isOver(now) }
        HomeUiState.Success(
            sessions = sessions,
            activeSession = running,
            remainingMillis = running?.endsAtMillis?.let { it - now },
            isBlockingServiceEnabled = serviceEnabled,
            // The session can end while the stop confirmation is open.
            dialog = dialog.takeUnless { it == HomeDialog.ConfirmStop && running == null },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    init {
        // The blocker service normally does this, but it may be off or its timer may be late.
        viewModelScope.launch { sessionManager.endIfExpired() }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.StartSession -> startSession(action.sessionId)
            is HomeAction.ConfirmStart -> confirmStart(action.sessionId, action.duration)
            HomeAction.RequestStop -> requestStop()
            HomeAction.ConfirmStop -> confirmStop()
            is HomeAction.DeleteSession -> viewModelScope.launch { sessionRepository.delete(action.sessionId) }
            HomeAction.DismissDialog -> dialog.value = null
        }
    }

    private fun startSession(id: String) {
        val state = uiState.value as? HomeUiState.Success ?: return
        dialog.value = if (state.isBlockingServiceEnabled) {
            HomeDialog.PickDuration(id)
        } else {
            HomeDialog.BlockingServicePrompt
        }
    }

    private fun confirmStart(id: String, duration: Duration?) {
        dialog.value = null
        viewModelScope.launch { sessionManager.start(id, duration) }
    }

    /** Timed sessions ask for confirmation before ending early. */
    private fun requestStop() {
        val state = uiState.value as? HomeUiState.Success ?: return
        if (state.remainingMillis != null) {
            dialog.value = HomeDialog.ConfirmStop
        } else {
            confirmStop()
        }
    }

    private fun confirmStop() {
        dialog.value = null
        viewModelScope.launch { sessionManager.stop() }
    }
}
