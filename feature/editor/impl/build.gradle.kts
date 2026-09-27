plugins {
    alias(libs.plugins.lockaltime.android.feature)
}

android {
    namespace = "com.lockaltime.feature.editor.impl"
}

dependencies {
    implementation(projects.feature.editor.api)
    implementation(projects.core.common)
    implementation(projects.core.data)
}
