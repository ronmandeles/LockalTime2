plugins {
    alias(libs.plugins.lockaltime.android.library)
    alias(libs.plugins.lockaltime.android.library.compose)
}

android {
    namespace = "com.lockaltime.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)

    implementation(libs.androidx.core.ktx)
}
