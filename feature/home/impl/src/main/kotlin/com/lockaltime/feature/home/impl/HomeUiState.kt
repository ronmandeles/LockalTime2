package com.lockaltime.feature.home.impl

import com.lockaltime.core.model.ActiveSession
import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.InstalledApp
import com.lockaltime.core.model.SessionInvite
import kotlin.time.Duration

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val sessions: List<BlockSession>,
        /** Null once the session's time is up, even before it's cleared from storage. */
        val activeSession: ActiveSession?,
        /** Time left in a timed session; null when nothing is running or it runs until stopped. */
        val remainingMillis: Long?,
        val isBlockingServiceEnabled: Boolean,
        val dialog: HomeDialog?,
    ) : HomeUiState
}

sealed interface HomeDialog {
    data object BlockingServicePrompt : HomeDialog
    data class PickDuration(val sessionId: String) : HomeDialog
    data object ConfirmStop : HomeDialog
    data object ScanInvite : HomeDialog

    /** @param code the QR content, or null if the session has too many apps to fit in one. */
    data class ShareInvite(val code: String?) : HomeDialog

    /** @param installedApps the invite's apps that are on this phone; the rest can't be opened anyway. */
    data class ConfirmJoin(val invite: SessionInvite, val installedApps: List<InstalledApp>) : HomeDialog

    data class JoinFailed(val reason: JoinFailure) : HomeDialog
}

enum class JoinFailure { NotAnInvite, TooNew, Expired, SessionRunning, NoCameraPermission, CameraUnavailable }

sealed interface HomeAction {
    data class StartSession(val sessionId: String) : HomeAction

    /** @param duration null to run until stopped. */
    data class ConfirmStart(val sessionId: String, val duration: Duration?) : HomeAction

    data object RequestStop : HomeAction
    data object ConfirmStop : HomeAction
    data class DeleteSession(val sessionId: String) : HomeAction
    data object DismissDialog : HomeAction

    data object ShareSession : HomeAction

    /** Opens the scanner; the camera permission must already be granted. */
    data object ScanInvite : HomeAction
    data object CameraPermissionDenied : HomeAction

    /** @param code the raw content of a scanned QR code, which may not be an invite at all. */
    data class InviteScanned(val code: String) : HomeAction
    data object ScanFailed : HomeAction
    data object ConfirmJoin : HomeAction
}
