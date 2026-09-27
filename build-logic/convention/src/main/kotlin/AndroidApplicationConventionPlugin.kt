import com.android.build.api.dsl.ApplicationExtension
import com.lockaltime.convention.TARGET_SDK
import com.lockaltime.convention.configureKotlinAndroid
import com.lockaltime.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = TARGET_SDK
            }
            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
            }
        }
    }
}
