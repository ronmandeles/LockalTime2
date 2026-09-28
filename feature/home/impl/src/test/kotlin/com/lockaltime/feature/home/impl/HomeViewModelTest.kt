package com.lockaltime.feature.home.impl

import com.lockaltime.core.domain.SessionManager
import com.lockaltime.core.invite.DecodedInvite
import com.lockaltime.core.invite.InviteCodec
import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.BlockedApp
import com.lockaltime.core.model.InstalledApp
import com.lockaltime.core.model.SessionInvite
import com.lockaltime.core.testing.MainDispatcherRule
import com.lockaltime.core.testing.TestClock
import com.lockaltime.core.testing.repository.TestActiveSessionRepository
import com.lockaltime.core.testing.repository.TestInstalledAppsRepository
import com.lockaltime.core.testing.repository.TestSessionRepository
import com.lockaltime.core.testing.util.TestBlockingServiceMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionRepository = TestSessionRepository()
    private val activeSessionRepository = TestActiveSessionRepository()
    private val blockingServiceMonitor = TestBlockingServiceMonitor(enabled = true)
    private val clock = TestClock(nowMillis = 1_000L)
    private val sessionManager = SessionManager(sessionRepository, activeSessionRepository, clock)
    private val installedAppsRepository = TestInstalledAppsRepository(listOf(InstalledApp("a.pkg", "A")))

    private val study = BlockSession("s1", "Study", listOf(BlockedApp("a.pkg", "A")), 0)

    // b.pkg isn't installed on this phone.
    private val groupStudy = SessionInvite(
        sessionId = "host1",
        name = "Group study",
        blockedPackages = setOf("a.pkg", "b.pkg"),
        endsAtMillis = 1_000L + 30.minutes.inWholeMilliseconds,
    )

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        viewModel = HomeViewModel(sessionRepository, sessionManager, installedAppsRepository, blockingServiceMonitor)
    }

    @Test
    fun `uiState is Loading before it is collected`() {
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `uiState lists saved sessions`() = runTest {
        sessionRepository.upsert(study)
        collectUiState()

        assertEquals(listOf(study), successState().sessions)
        assertNull(successState().activeSession)
    }

    @Test
    fun `uiState shows the time left in a timed session`() = runTest {
        sessionRepository.upsert(study)
        sessionManager.start("s1", 25.minutes)
        clock.nowMillis += 10.minutes.inWholeMilliseconds
        collectUiState()

        assertEquals("s1", successState().activeSession?.sessionId)
        assertEquals(15.minutes.inWholeMilliseconds, successState().remainingMillis)
    }

    @Test
    fun `uiState hides a session whose time is up`() = runTest {
        sessionRepository.upsert(study)
        sessionManager.start("s1", 25.minutes)
        clock.nowMillis += 25.minutes.inWholeMilliseconds
        collectUiState()

        assertNull(successState().activeSession)
    }

    @Test
    fun `uiState follows the blocking service setting`() = runTest {
        collectUiState()

        blockingServiceMonitor.setEnabled(false)

        assertFalse(successState().isBlockingServiceEnabled)
    }

    @Test
    fun `opening the app without the blocking service asks to enable it`() = runTest {
        blockingServiceMonitor.setEnabled(false)
        viewModel = HomeViewModel(sessionRepository, sessionManager, installedAppsRepository, blockingServiceMonitor)
        collectUiState()

        assertEquals(HomeDialog.BlockingServicePrompt, successState().dialog)
    }

    @Test
    fun `opening the app with the blocking service asks nothing`() = runTest {
        collectUiState()

        assertNull(successState().dialog)
    }

    @Test
    fun `starting a session asks for a duration`() = runTest {
        sessionRepository.upsert(study)
        collectUiState()

        viewModel.onAction(HomeAction.StartSession("s1"))

        assertEquals(HomeDialog.PickDuration("s1"), successState().dialog)
    }

    @Test
    fun `starting a session without the blocking service asks to enable it`() = runTest {
        blockingServiceMonitor.setEnabled(false)
        sessionRepository.upsert(study)
        collectUiState()

        viewModel.onAction(HomeAction.StartSession("s1"))

        assertEquals(HomeDialog.BlockingServicePrompt, successState().dialog)
    }

    @Test
    fun `confirming a duration starts the session`() = runTest {
        sessionRepository.upsert(study)
        collectUiState()
        viewModel.onAction(HomeAction.StartSession("s1"))

        viewModel.onAction(HomeAction.ConfirmStart("s1", 25.minutes))

        assertNull(successState().dialog)
        assertEquals(clock.nowMillis + 25.minutes.inWholeMilliseconds, successState().activeSession?.endsAtMillis)
    }

    @Test
    fun `stopping a timed session asks for confirmation`() = runTest {
        sessionRepository.upsert(study)
        sessionManager.start("s1", 25.minutes)
        collectUiState()

        viewModel.onAction(HomeAction.RequestStop)
        assertEquals(HomeDialog.ConfirmStop, successState().dialog)

        viewModel.onAction(HomeAction.ConfirmStop)
        assertNull(activeSessionRepository.activeSession.first())
    }

    @Test
    fun `stopping an open-ended session needs no confirmation`() = runTest {
        sessionRepository.upsert(study)
        sessionManager.start("s1", duration = null)
        collectUiState()

        viewModel.onAction(HomeAction.RequestStop)

        assertNull(successState().dialog)
        assertNull(activeSessionRepository.activeSession.first())
    }

    @Test
    fun `deleting a session removes it`() = runTest {
        sessionRepository.upsert(study)
        collectUiState()

        viewModel.onAction(HomeAction.DeleteSession("s1"))

        assertEquals(emptyList<BlockSession>(), successState().sessions)
    }

    @Test
    fun `sharing shows a code for the running session`() = runTest {
        sessionRepository.upsert(study)
        sessionManager.start("s1", 25.minutes)
        collectUiState()

        viewModel.onAction(HomeAction.ShareSession)

        val code = (successState().dialog as HomeDialog.ShareInvite).code!!
        assertEquals(
            DecodedInvite.Valid(SessionInvite("s1", "Study", setOf("a.pkg"), 1_000L + 25.minutes.inWholeMilliseconds)),
            InviteCodec.decode(code),
        )
    }

    @Test
    fun `the share dialog closes when the session ends`() = runTest {
        sessionRepository.upsert(study)
        sessionManager.start("s1", 25.minutes)
        collectUiState()
        viewModel.onAction(HomeAction.ShareSession)

        sessionManager.stop()

        assertNull(successState().dialog)
    }

    @Test
    fun `scanning an invite asks to confirm, listing the apps on this phone`() = runTest {
        collectUiState()

        viewModel.onAction(HomeAction.InviteScanned(InviteCodec.encode(groupStudy)!!))

        assertEquals(HomeDialog.ConfirmJoin(groupStudy, listOf(InstalledApp("a.pkg", "A"))), successState().dialog)
    }

    @Test
    fun `confirming joins the session`() = runTest {
        collectUiState()
        viewModel.onAction(HomeAction.InviteScanned(InviteCodec.encode(groupStudy)!!))

        viewModel.onAction(HomeAction.ConfirmJoin)

        assertNull(successState().dialog)
        assertEquals("host1", successState().activeSession?.sessionId)
    }

    @Test
    fun `confirming after the session ended reports it`() = runTest {
        collectUiState()
        viewModel.onAction(HomeAction.InviteScanned(InviteCodec.encode(groupStudy)!!))
        clock.nowMillis = groupStudy.endsAtMillis!!

        viewModel.onAction(HomeAction.ConfirmJoin)

        assertEquals(HomeDialog.JoinFailed(JoinFailure.Expired), successState().dialog)
        assertNull(activeSessionRepository.activeSession.first())
    }

    @Test
    fun `scanning something that isn't an invite reports it`() = runTest {
        collectUiState()

        viewModel.onAction(HomeAction.InviteScanned("https://example.com"))

        assertEquals(HomeDialog.JoinFailed(JoinFailure.NotAnInvite), successState().dialog)
    }

    @Test
    fun `scanning an invite whose time is up reports it`() = runTest {
        collectUiState()

        viewModel.onAction(HomeAction.InviteScanned(InviteCodec.encode(groupStudy.copy(endsAtMillis = 1_000L))!!))

        assertEquals(HomeDialog.JoinFailed(JoinFailure.Expired), successState().dialog)
    }

    @Test
    fun `scanning while a session is running reports it`() = runTest {
        sessionRepository.upsert(study)
        sessionManager.start("s1", duration = null)
        collectUiState()

        viewModel.onAction(HomeAction.InviteScanned(InviteCodec.encode(groupStudy)!!))

        assertEquals(HomeDialog.JoinFailed(JoinFailure.SessionRunning), successState().dialog)
    }

    @Test
    fun `scanning without the blocking service asks to enable it`() = runTest {
        blockingServiceMonitor.setEnabled(false)
        collectUiState()

        viewModel.onAction(HomeAction.InviteScanned(InviteCodec.encode(groupStudy)!!))

        assertEquals(HomeDialog.BlockingServicePrompt, successState().dialog)
    }

    @Test
    fun `scanning an invite closes the scanner`() = runTest {
        collectUiState()
        viewModel.onAction(HomeAction.ScanInvite)
        assertEquals(HomeDialog.ScanInvite, successState().dialog)

        viewModel.onAction(HomeAction.InviteScanned(InviteCodec.encode(groupStudy)!!))

        assertEquals(HomeDialog.ConfirmJoin(groupStudy, listOf(InstalledApp("a.pkg", "A"))), successState().dialog)
    }

    @Test
    fun `denying the camera permission is reported`() = runTest {
        collectUiState()

        viewModel.onAction(HomeAction.CameraPermissionDenied)

        assertEquals(HomeDialog.JoinFailed(JoinFailure.NoCameraPermission), successState().dialog)
    }

    @Test
    fun `a camera failure is reported`() = runTest {
        collectUiState()
        viewModel.onAction(HomeAction.ScanInvite)

        viewModel.onAction(HomeAction.ScanFailed)

        assertEquals(HomeDialog.JoinFailed(JoinFailure.CameraUnavailable), successState().dialog)
    }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun successState() = viewModel.uiState.value as HomeUiState.Success
}
