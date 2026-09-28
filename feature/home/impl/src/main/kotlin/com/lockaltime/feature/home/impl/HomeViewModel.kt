package com.lockaltime.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockaltime.core.data.repository.InstalledAppsRepository
import com.lockaltime.core.data.repository.SessionRepository
import com.lockaltime.core.data.util.BlockingServiceMonitor
import com.lockaltime.core.domain.JoinResult
import com.lockaltime.core.domain.SessionManager
import com.lockaltime.core.invite.DecodedInvite
import com.lockaltime.core.invite.InviteCodec
import com.lockaltime.core.model.toInvite
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager,
    private val installedAppsRepository: InstalledAppsRepository,
    private val blockingServiceMonitor: BlockingServiceMonitor,
) : ViewModel() {

    /** For opening Accessibility settings at this app's entry. */
    val blockingServiceComponent: String get() = blockingServiceMonitor.serviceComponent

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
            // The session can end while the stop confirmation or its QR code is open.
            dialog = dialog.takeUnless {
                (it == HomeDialog.ConfirmStop || it is HomeDialog.ShareInvite) && running == null
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    init {
        // The blocker service normally does this, but it may be off or its timer may be late.
        viewModelScope.launch { sessionManager.endIfExpired() }
        // Nothing works without the blocker, so ask as soon as the app opens rather than on first
        // start. Android offers no permission popup for accessibility services; only Settings.
        viewModelScope.launch {
            if (!blockingServiceMonitor.isEnabled.first()) {
                dialog.compareAndSet(null, HomeDialog.BlockingServicePrompt)
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.StartSession -> startSession(action.sessionId)
            is HomeAction.ConfirmStart -> confirmStart(action.sessionId, action.duration)
            HomeAction.RequestStop -> requestStop()
            HomeAction.ConfirmStop -> confirmStop()
            is HomeAction.DeleteSession -> viewModelScope.launch { sessionRepository.delete(action.sessionId) }
            HomeAction.DismissDialog -> dialog.value = null
            HomeAction.ShareSession -> shareSession()
            HomeAction.ScanInvite -> dialog.value = HomeDialog.ScanInvite
            HomeAction.CameraPermissionDenied -> joinFailed(JoinFailure.NoCameraPermission)
            is HomeAction.InviteScanned -> inviteScanned(action.code)
            HomeAction.ScanFailed -> joinFailed(JoinFailure.CameraUnavailable)
            HomeAction.ConfirmJoin -> confirmJoin()
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

    private fun shareSession() {
        val running = (uiState.value as? HomeUiState.Success)?.activeSession ?: return
        dialog.value = HomeDialog.ShareInvite(InviteCodec.encode(running.toInvite()))
    }

    private fun inviteScanned(code: String) {
        // Closes the scanner, which would otherwise stay open while installed apps load.
        dialog.value = null
        val state = uiState.value as? HomeUiState.Success ?: return
        val invite = when (val decoded = InviteCodec.decode(code)) {
            is DecodedInvite.Valid -> decoded.invite
            DecodedInvite.TooNew -> return joinFailed(JoinFailure.TooNew)
            DecodedInvite.NotAnInvite -> return joinFailed(JoinFailure.NotAnInvite)
        }
        when {
            invite.isExpired(sessionManager.now()) -> joinFailed(JoinFailure.Expired)
            state.activeSession != null -> joinFailed(JoinFailure.SessionRunning)
            !state.isBlockingServiceEnabled -> dialog.value = HomeDialog.BlockingServicePrompt
            else -> viewModelScope.launch {
                val installed = installedAppsRepository.getLaunchableApps()
                    .filter { it.packageName in invite.blockedPackages }
                dialog.value = HomeDialog.ConfirmJoin(invite, installed)
            }
        }
    }

    /** Re-checked on confirm: time passes and a session may start while the dialog is open. */
    private fun confirmJoin() {
        val invite = (dialog.value as? HomeDialog.ConfirmJoin)?.invite ?: return
        dialog.value = null
        viewModelScope.launch {
            when (sessionManager.join(invite)) {
                JoinResult.Joined -> Unit
                JoinResult.Expired -> joinFailed(JoinFailure.Expired)
                JoinResult.SessionRunning -> joinFailed(JoinFailure.SessionRunning)
            }
        }
    }

    private fun joinFailed(reason: JoinFailure) {
        dialog.value = HomeDialog.JoinFailed(reason)
    }
}
