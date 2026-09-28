package com.lockaltime.feature.home.api

import kotlinx.serialization.Serializable

/**
 * @param invite the payload of an invite link the app was opened with (the `d` query parameter of
 * `lockaltime://join`), or null when opened normally.
 */
@Serializable
data class HomeRoute(val invite: String? = null)
