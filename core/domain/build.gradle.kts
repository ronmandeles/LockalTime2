plugins {
    alias(libs.plugins.lockaltime.android.library)
}

android {
    namespace = "com.lockaltime.core.domain"
}

dependencies {
    api(projects.core.common)
    api(projects.core.data)
    api(projects.core.model)

    implementation(libs.javax.inject)

    testImplementation(projects.core.testing)
}
