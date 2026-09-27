package com.lockaltime.feature.home.impl

import com.lockaltime.core.model.ActiveSession
import com.lockaltime.core.model.BlockSession
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
}

sealed interface HomeAction {
    data class StartSession(val sessionId: String) : HomeAction

    /** @param duration null to run until stopped. */
    data class ConfirmStart(val sessionId: String, val duration: Duration?) : HomeAction

    data object RequestStop : HomeAction
    data object ConfirmStop : HomeAction
    data class DeleteSession(val sessionId: String) : HomeAction
    data object DismissDialog : HomeAction
}
