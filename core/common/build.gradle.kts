plugins {
    alias(libs.plugins.lockaltime.android.library)
    alias(libs.plugins.lockaltime.hilt)
}

android {
    namespace = "com.lockaltime.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
