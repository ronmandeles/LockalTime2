plugins {
    alias(libs.plugins.lockaltime.android.library)
    alias(libs.plugins.lockaltime.android.library.compose)
}

android {
    namespace = "com.lockaltime.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.ui)
}
