package com.lockaltime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                LockalTimeNavHost()
            }
        }
    }
}
