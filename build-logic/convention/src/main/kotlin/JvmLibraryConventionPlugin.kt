import com.lockaltime.convention.configureKotlinJvm
import com.lockaltime.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Pure Kotlin modules with no Android dependencies. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            dependencies {
                add("testImplementation", libs.findLibrary("junit").get())
            }
            // So `./gradlew testDebugUnitTest` also runs the tests of pure Kotlin modules.
            tasks.register("testDebugUnitTest") { dependsOn("test") }
        }
    }
}
