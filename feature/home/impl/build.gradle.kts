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
}
