plugins {
    alias(libs.plugins.lockaltime.android.library)
    alias(libs.plugins.lockaltime.android.library.compose)
    alias(libs.plugins.lockaltime.hilt)
}

android {
    namespace = "com.lockaltime.blocking"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.ui)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
