plugins {
    alias(libs.plugins.lockaltime.android.library)
    alias(libs.plugins.lockaltime.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lockaltime.core.datastore"
}

dependencies {
    api(libs.androidx.datastore)
    api(projects.core.model)

    implementation(projects.core.common)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}
