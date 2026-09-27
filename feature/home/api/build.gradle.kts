plugins {
    alias(libs.plugins.lockaltime.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lockaltime.feature.home.api"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
