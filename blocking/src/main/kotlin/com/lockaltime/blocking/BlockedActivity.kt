package com.lockaltime.blocking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lockaltime.core.designsystem.theme.LockalTimeTheme
import com.lockaltime.core.domain.SessionManager
import com.lockaltime.core.ui.AppIcon
import com.lockaltime.core.ui.ThemePreviews
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/** Full-screen cover shown on top of a blocked app. Leaving it always goes to the home screen. */
@AndroidEntryPoint
class BlockedActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    private var blocked by mutableStateOf<BlockedApp?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blocked = readBlockedApp(intent)

        // Going "back" would reveal the blocked app underneath.
        onBackPressedDispatcher.addCallback(this) { goHome() }

        // Once the session is over the app underneath is allowed, so get out of the way.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.activeSession.collectLatest { session ->
                    session?.endsAtMillis?.let { delay(it - sessionManager.now()) }
                    if (session == null || session.isOver(sessionManager.now())) finish()
                }
            }
        }

        setContent {
            LockalTimeTheme {
                blocked?.let {
                    BlockedScreen(
                        packageName = it.packageName,
                        appLabel = it.label,
                        sessionName = it.sessionName,
                        endsAtMillis = it.endsAtMillis,
                        onGoHome = ::goHome,
                    )
                }
            }
        }
    }

    // singleTask: the service re-launches this screen whenever a blocked app surfaces again.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        blocked = readBlockedApp(intent)
    }

    private fun readBlockedApp(intent: Intent): BlockedApp {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val label = runCatching {
            @Suppress("DEPRECATION")
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                .toString()
        }.getOrDefault(packageName)
        return BlockedApp(
            packageName = packageName,
            label = label,
            sessionName = intent.getStringExtra(EXTRA_SESSION_NAME).orEmpty(),
            endsAtMillis = intent.getLongExtra(EXTRA_ENDS_AT, NO_END).takeIf { it != NO_END },
        )
    }

    private data class BlockedApp(
        val packageName: String,
        val label: String,
        val sessionName: String,
        val endsAtMillis: Long?,
    )

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "blocked_package"
        private const val EXTRA_SESSION_NAME = "session_name"
        private const val EXTRA_ENDS_AT = "ends_at"
        private const val NO_END = -1L

        fun intent(
            context: Context,
            blockedPackage: String,
            sessionName: String,
            endsAtMillis: Long?,
        ): Intent =
            Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_PACKAGE, blockedPackage)
                .putExtra(EXTRA_SESSION_NAME, sessionName)
                .putExtra(EXTRA_ENDS_AT, endsAtMillis ?: NO_END)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

@Composable
private fun BlockedScreen(
    packageName: String,
    appLabel: String,
    sessionName: String,
    endsAtMillis: Long?,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(packageName = packageName, size = 72.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.blocked_title, appLabel),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.blocked_message, sessionName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (endsAtMillis != null) {
                val endsAt = DateFormat.getTimeFormat(LocalContext.current).format(Date(endsAtMillis))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.blocked_unblocks_at, endsAt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onGoHome, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.blocked_go_home))
            }
        }
    }
}

@ThemePreviews
@Composable
private fun BlockedScreenPreview() {
    LockalTimeTheme(dynamicColor = false) {
        BlockedScreen(
            packageName = "com.instagram.android",
            appLabel = "Instagram",
            sessionName = "Study",
            endsAtMillis = 0,
            onGoHome = {},
        )
    }
}
