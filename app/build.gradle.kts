plugins {
    alias(libs.plugins.lockaltime.android.application)
    alias(libs.plugins.lockaltime.android.application.compose)
    alias(libs.plugins.lockaltime.hilt)
}

android {
    namespace = "com.lockaltime"

    defaultConfig {
        applicationId = "com.lockaltime"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(projects.blocking)
    implementation(projects.core.designsystem)
    implementation(projects.feature.editor.api)
    implementation(projects.feature.editor.impl)
    implementation(projects.feature.home.api)
    implementation(projects.feature.home.impl)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
}
