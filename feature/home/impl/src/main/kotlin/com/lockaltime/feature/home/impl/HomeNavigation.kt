package com.lockaltime.feature.home.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.lockaltime.core.invite.InviteCodec
import com.lockaltime.feature.home.api.HomeRoute

fun NavGraphBuilder.homeScreen(
    onCreateSession: () -> Unit,
    onEditSession: (sessionId: String) -> Unit,
) {
    composable<HomeRoute>(
        // An invite opened from outside the app, e.g. its QR code scanned with the system camera.
        // The query key is the codec's; the placeholder names the route field it fills.
        deepLinks = listOf(navDeepLink { uriPattern = InviteCodec.URI_PREFIX + "{invite}" }),
    ) { entry ->
        HomeScreen(
            // The link carries only the payload; the ViewModel expects a whole code, as scanned.
            inviteCode = entry.toRoute<HomeRoute>().invite?.let { InviteCodec.URI_PREFIX + it },
            onCreateSession = onCreateSession,
            onEditSession = onEditSession,
        )
    }
}
