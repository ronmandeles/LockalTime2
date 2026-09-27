package com.lockaltime.feature.home.impl

import com.lockaltime.core.domain.SessionManager
import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.BlockedApp
import com.lockaltime.core.testing.MainDispatcherRule
import com.lockaltime.core.testing.TestClock
import com.lockaltime.core.testing.repository.TestActiveSessionRepository
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

    private val study = BlockSession("s1", "Study", listOf(BlockedApp("a.pkg", "A")), 0)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        viewModel = HomeViewModel(sessionRepository, sessionManager, blockingServiceMonitor)
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

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private fun successState() = viewModel.uiState.value as HomeUiState.Success
}
