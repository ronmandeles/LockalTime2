plugins {
    alias(libs.plugins.lockaltime.android.feature)
}

android {
    namespace = "com.lockaltime.feature.home.impl"
}

dependencies {
    implementation(projects.feature.home.api)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.invite)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.zxing.core)
}
