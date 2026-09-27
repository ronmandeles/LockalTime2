package com.lockaltime.feature.home.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lockaltime.feature.home.api.HomeRoute

fun NavGraphBuilder.homeScreen(
    onCreateSession: () -> Unit,
    onEditSession: (sessionId: String) -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(onCreateSession = onCreateSession, onEditSession = onEditSession)
    }
}
