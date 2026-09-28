package com.lockaltime.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.lockaltime.feature.editor.api.navigateToSessionEditor
import com.lockaltime.feature.editor.impl.sessionEditorScreen
import com.lockaltime.feature.home.api.HomeRoute
import com.lockaltime.feature.home.impl.homeScreen

/** Wires the feature screens together; the features themselves don't know about each other. */
@Composable
fun LockalTimeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = HomeRoute(), modifier = modifier) {
        homeScreen(
            onCreateSession = { navController.navigateToSessionEditor() },
            onEditSession = { id -> navController.navigateToSessionEditor(id) },
        )
        sessionEditorScreen(onDone = { navController.popBackStack() })
    }
}
