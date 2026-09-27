package com.lockaltime.feature.editor.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockaltime.core.designsystem.icon.LockalTimeIcons
import com.lockaltime.core.designsystem.theme.LockalTimeTheme
import com.lockaltime.core.model.InstalledApp
import com.lockaltime.core.ui.AppIcon
import com.lockaltime.core.ui.ThemePreviews
import com.lockaltime.feature.editor.impl.SessionEditorUiState.Editing

@Composable
internal fun SessionEditorScreen(
    sessionId: String?,
    onDone: () -> Unit,
    viewModel: SessionEditorViewModel =
        hiltViewModel<SessionEditorViewModel, SessionEditorViewModel.Factory> { it.create(sessionId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaved = (uiState as? Editing)?.isSaved == true

    LaunchedEffect(isSaved) {
        if (isSaved) onDone()
    }

    SessionEditorScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onDone,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionEditorScreen(
    uiState: SessionEditorUiState,
    onAction: (SessionEditorAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.new_session else R.string.edit_session))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(LockalTimeIcons.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(SessionEditorAction.Save) },
                        enabled = uiState is Editing && uiState.canSave,
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        when (uiState) {
            is SessionEditorUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            is Editing -> EditorContent(
                uiState = uiState,
                onAction = onAction,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun EditorContent(
    uiState: Editing,
    onAction: (SessionEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onAction(SessionEditorAction.NameChanged(it)) },
            label = { Text(stringResource(R.string.session_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { onAction(SessionEditorAction.QueryChanged(it)) },
            placeholder = { Text(stringResource(R.string.search_apps)) },
            leadingIcon = { Icon(LockalTimeIcons.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = pluralStringResource(
                R.plurals.apps_selected,
                uiState.selectedPackages.size,
                uiState.selectedPackages.size,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(uiState.visibleApps, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    checked = app.packageName in uiState.selectedPackages,
                    onToggle = { onAction(SessionEditorAction.AppToggled(app.packageName)) },
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName)
        Spacer(Modifier.width(16.dp))
        Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

@ThemePreviews
@Composable
private fun SessionEditorScreenPreview() {
    LockalTimeTheme(dynamicColor = false) {
        SessionEditorScreen(
            uiState = Editing(
                isNew = false,
                name = "Study",
                query = "",
                apps = listOf(
                    InstalledApp("com.instagram.android", "Instagram"),
                    InstalledApp("com.google.android.youtube", "YouTube"),
                    InstalledApp("com.google.android.apps.maps", "Maps"),
                ),
                selectedPackages = setOf("com.instagram.android", "com.google.android.youtube"),
            ),
            onAction = {},
            onBack = {},
        )
    }
}
