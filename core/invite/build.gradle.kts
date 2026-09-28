plugins {
    alias(libs.plugins.lockaltime.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.serialization.json)
}
