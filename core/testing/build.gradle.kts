plugins {
    alias(libs.plugins.lockaltime.android.library)
}

android {
    namespace = "com.lockaltime.core.testing"
}

dependencies {
    api(projects.core.common)
    api(projects.core.data)
    api(projects.core.model)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
