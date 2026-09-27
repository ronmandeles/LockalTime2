package com.lockaltime.feature.editor.impl

import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.BlockedApp
import com.lockaltime.core.model.InstalledApp
import com.lockaltime.core.testing.MainDispatcherRule
import com.lockaltime.core.testing.TestClock
import com.lockaltime.core.testing.repository.TestInstalledAppsRepository
import com.lockaltime.core.testing.repository.TestSessionRepository
import com.lockaltime.feature.editor.impl.SessionEditorUiState.Editing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionRepository = TestSessionRepository()
    private val installedAppsRepository = TestInstalledAppsRepository(
        apps = listOf(
            InstalledApp("a.pkg", "Alpha"),
            InstalledApp("b.pkg", "Beta"),
            InstalledApp("c.pkg", "Gamma"),
        ),
    )
    private val clock = TestClock(nowMillis = 5_000L)

    private fun viewModel(sessionId: String? = null) =
        SessionEditorViewModel(sessionId, sessionRepository, installedAppsRepository, clock)

    private fun SessionEditorViewModel.editingState() = uiState.value as Editing

    @Test
    fun `a new session starts empty`() {
        val state = viewModel().editingState()

        assertTrue(state.isNew)
        assertEquals("", state.name)
        assertEquals(emptySet<String>(), state.selectedPackages)
        assertEquals(installedAppsRepository.apps, state.apps)
    }

    @Test
    fun `editing loads the session and lists its apps first`() = runTest {
        sessionRepository.upsert(BlockSession("s1", "Study", listOf(BlockedApp("c.pkg", "Gamma")), 0))

        val state = viewModel("s1").editingState()

        assertFalse(state.isNew)
        assertEquals("Study", state.name)
        assertEquals(setOf("c.pkg"), state.selectedPackages)
        assertEquals("c.pkg", state.apps.first().packageName)
    }

    @Test
    fun `saving needs a name and at least one app`() {
        val viewModel = viewModel()
        assertFalse(viewModel.editingState().canSave)

        viewModel.onAction(SessionEditorAction.NameChanged("Study"))
        assertFalse(viewModel.editingState().canSave)

        viewModel.onAction(SessionEditorAction.AppToggled("a.pkg"))
        assertTrue(viewModel.editingState().canSave)

        viewModel.onAction(SessionEditorAction.AppToggled("a.pkg"))
        assertFalse(viewModel.editingState().canSave)
    }

    @Test
    fun `the query filters apps by label`() {
        val viewModel = viewModel()

        viewModel.onAction(SessionEditorAction.QueryChanged(" gam "))

        assertEquals(listOf("c.pkg"), viewModel.editingState().visibleApps.map { it.packageName })
    }

    @Test
    fun `saving a new session stores it`() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(SessionEditorAction.NameChanged("  Study  "))
        viewModel.onAction(SessionEditorAction.AppToggled("b.pkg"))

        viewModel.onAction(SessionEditorAction.Save)

        val saved = sessionRepository.sessions.first().single()
        assertEquals("Study", saved.name)
        assertEquals(listOf(BlockedApp("b.pkg", "Beta")), saved.blockedApps)
        assertEquals(5_000L, saved.createdAtMillis)
        assertTrue(viewModel.editingState().isSaved)
    }

    @Test
    fun `saving an existing session keeps its id and creation time`() = runTest {
        sessionRepository.upsert(BlockSession("s1", "Study", listOf(BlockedApp("a.pkg", "Alpha")), 42))
        val viewModel = viewModel("s1")

        viewModel.onAction(SessionEditorAction.NameChanged("Exams"))
        viewModel.onAction(SessionEditorAction.Save)

        assertEquals(
            listOf(BlockSession("s1", "Exams", listOf(BlockedApp("a.pkg", "Alpha")), 42)),
            sessionRepository.sessions.first(),
        )
    }
}
