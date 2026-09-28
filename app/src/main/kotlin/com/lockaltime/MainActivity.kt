package com.lockaltime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import com.lockaltime.core.designsystem.theme.LockalTimeTheme
import com.lockaltime.navigation.LockalTimeNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LockalTimeTheme {
                val navController = rememberNavController()
                // NavHost reads the launching intent itself; invite links that arrive while the
                // app is already open come through onNewIntent.
                DisposableEffect(navController) {
                    val listener = Consumer<Intent> { intent ->
                        // singleTask makes the system add NEW_TASK to every delivered intent, and
                        // handleDeepLink answers NEW_TASK by restarting the activity. The task is
                        // this app's own, so navigate in place instead.
                        navController.handleDeepLink(
                            Intent(intent).apply { removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                        )
                    }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }
                LockalTimeNavHost(navController = navController)
            }
        }
    }
}
