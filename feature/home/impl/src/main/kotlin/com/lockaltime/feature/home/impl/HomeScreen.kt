package com.lockaltime.feature.home.impl

import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockaltime.core.designsystem.icon.LockalTimeIcons
import com.lockaltime.core.designsystem.theme.LockalTimeTheme
import com.lockaltime.core.model.ActiveSession
import com.lockaltime.core.model.BlockSession
import com.lockaltime.core.model.BlockedApp
import com.lockaltime.core.ui.AppIcon
import com.lockaltime.core.ui.ThemePreviews
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun HomeScreen(
    onCreateSession: () -> Unit,
    onEditSession: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onCreateSession = onCreateSession,
        onEditSession = onEditSession,
        onOpenBlockingSettings = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onCreateSession: () -> Unit,
    onEditSession: (String) -> Unit,
    onOpenBlockingSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateSession,
                icon = { Icon(LockalTimeIcons.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_session)) },
            )
        },
    ) { padding ->
        when (uiState) {
            HomeUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            is HomeUiState.Success -> HomeContent(
                uiState = uiState,
                onAction = onAction,
                onEditSession = onEditSession,
                onOpenBlockingSettings = onOpenBlockingSettings,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (uiState is HomeUiState.Success) {
        HomeDialogs(
            uiState = uiState,
            onAction = onAction,
            onOpenBlockingSettings = onOpenBlockingSettings,
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState.Success,
    onAction: (HomeAction) -> Unit,
    onEditSession: (String) -> Unit,
    onOpenBlockingSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!uiState.isBlockingServiceEnabled) {
            item { BlockingServiceCard(onEnable = onOpenBlockingSettings) }
        }
        uiState.activeSession?.let { active ->
            item {
                ActiveSessionCard(
                    session = active,
                    remainingMillis = uiState.remainingMillis,
                    onStop = { onAction(HomeAction.RequestStop) },
                )
            }
        }
        if (uiState.sessions.isEmpty()) {
            item { EmptyState() }
        }
        items(uiState.sessions, key = { it.id }) { session ->
            SessionCard(
                session = session,
                isActive = session.id == uiState.activeSession?.sessionId,
                canStart = uiState.activeSession == null,
                onStart = { onAction(HomeAction.StartSession(session.id)) },
                onEdit = { onEditSession(session.id) },
                onDelete = { onAction(HomeAction.DeleteSession(session.id)) },
            )
        }
    }
}

@Composable
private fun HomeDialogs(
    uiState: HomeUiState.Success,
    onAction: (HomeAction) -> Unit,
    onOpenBlockingSettings: () -> Unit,
) {
    val dismiss = { onAction(HomeAction.DismissDialog) }
    when (val dialog = uiState.dialog) {
        HomeDialog.BlockingServicePrompt -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    dismiss()
                    onOpenBlockingSettings()
                }) { Text(stringResource(R.string.permission_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) }
            },
        )
        is HomeDialog.PickDuration -> DurationDialog(
            onPick = { onAction(HomeAction.ConfirmStart(dialog.sessionId, it)) },
            onDismiss = dismiss,
        )
        HomeDialog.ConfirmStop -> AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.end_early_title)) },
            text = {
                Text(stringResource(R.string.end_early_message, formatRemaining(uiState.remainingMillis ?: 0)))
            },
            confirmButton = {
                TextButton(onClick = { onAction(HomeAction.ConfirmStop) }) {
                    Text(stringResource(R.string.end_early_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(R.string.keep_going)) }
            },
        )
        null -> Unit
    }
}

@Composable
private fun DurationDialog(onPick: (Duration?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_duration)) },
        text = {
            Column {
                DURATION_PRESETS.forEach { duration ->
                    TextButton(onClick = { onPick(duration) }, modifier = Modifier.fillMaxWidth()) {
                        Text(durationLabel(duration))
                    }
                }
                TextButton(onClick = { onPick(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.until_stopped))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun durationLabel(duration: Duration): String {
    val minutes = duration.inWholeMinutes.toInt()
    return if (minutes % 60 == 0) {
        pluralStringResource(R.plurals.duration_hours, minutes / 60, minutes / 60)
    } else {
        pluralStringResource(R.plurals.duration_minutes, minutes, minutes)
    }
}

/** Rounds up, so a running session never shows 0:00. */
private fun formatRemaining(remainingMillis: Long): String =
    DateUtils.formatElapsedTime((remainingMillis + 999) / 1_000)

@Composable
private fun BlockingServiceCard(onEnable: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(LockalTimeIcons.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.permission_message), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onEnable) { Text(stringResource(R.string.permission_open_settings)) }
        }
    }
}

@Composable
private fun ActiveSessionCard(
    session: ActiveSession,
    remainingMillis: Long?,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormat = DateFormat.getTimeFormat(LocalContext.current)
    val startedAt = timeFormat.format(Date(session.startedAtMillis))
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(LockalTimeIcons.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.active_session_title, session.name),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                pluralStringResource(
                    R.plurals.active_session_summary,
                    session.blockedPackages.size,
                    session.blockedPackages.size,
                    startedAt,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            val endsAtMillis = session.endsAtMillis
            if (endsAtMillis != null && remainingMillis != null) {
                Text(
                    stringResource(
                        R.string.ends_at_remaining,
                        timeFormat.format(Date(endsAtMillis)),
                        formatRemaining(remainingMillis),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stop_session))
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: BlockSession,
    isActive: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A running session can't be edited or deleted until it's stopped.
    Card(onClick = onEdit, enabled = !isActive, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(session.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        pluralStringResource(
                            R.plurals.apps_count,
                            session.blockedApps.size,
                            session.blockedApps.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete, enabled = !isActive) {
                    Icon(LockalTimeIcons.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                session.blockedApps.take(MAX_PREVIEW_ICONS).forEach { AppIcon(it.packageName, size = 28.dp) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onStart, enabled = canStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (isActive) R.string.session_running else R.string.start_session))
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.empty_sessions),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val MAX_PREVIEW_ICONS = 8

private val DURATION_PRESETS = listOf(25.minutes, 50.minutes, 1.hours, 2.hours)

@ThemePreviews
@Composable
private fun HomeScreenPreview() {
    val study = BlockSession(
        id = "s1",
        name = "Study",
        blockedApps = listOf(BlockedApp("com.instagram.android", "Instagram")),
        createdAtMillis = 0,
    )
    val work = study.copy(id = "s2", name = "Deep work")
    LockalTimeTheme(dynamicColor = false) {
        HomeScreen(
            uiState = HomeUiState.Success(
                sessions = listOf(study, work),
                activeSession = ActiveSession(
                    sessionId = "s1",
                    name = "Study",
                    blockedPackages = setOf("com.instagram.android"),
                    startedAtMillis = 0,
                    endsAtMillis = 25 * 60_000L,
                ),
                remainingMillis = 12 * 60_000L,
                isBlockingServiceEnabled = false,
                dialog = null,
            ),
            onAction = {},
            onCreateSession = {},
            onEditSession = {},
            onOpenBlockingSettings = {},
        )
    }
}

@ThemePreviews
@Composable
private fun HomeScreenEmptyPreview() {
    LockalTimeTheme(dynamicColor = false) {
        HomeScreen(
            uiState = HomeUiState.Success(
                sessions = emptyList(),
                activeSession = null,
                remainingMillis = null,
                isBlockingServiceEnabled = true,
                dialog = null,
            ),
            onAction = {},
            onCreateSession = {},
            onEditSession = {},
            onOpenBlockingSettings = {},
        )
    }
}
