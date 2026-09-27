package com.lockaltime.core.ui

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Renders a preview in both light and dark theme. */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews
