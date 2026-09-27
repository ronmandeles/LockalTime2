plugins {
    alias(libs.plugins.lockaltime.android.library)
    alias(libs.plugins.lockaltime.hilt)
}

android {
    namespace = "com.lockaltime.core.data"
}

dependencies {
    api(projects.core.model)

    implementation(projects.core.common)
    implementation(projects.core.datastore)

    testImplementation(libs.kotlinx.coroutines.test)
}
