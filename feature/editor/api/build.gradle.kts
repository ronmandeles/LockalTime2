plugins {
    alias(libs.plugins.lockaltime.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lockaltime.feature.editor.api"
}

dependencies {
    api(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
}
