pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LockalTime"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":blocking")
include(":core:common")
include(":core:data")
include(":core:datastore")
include(":core:designsystem")
include(":core:domain")
include(":core:invite")
include(":core:model")
include(":core:testing")
include(":core:ui")
include(":feature:editor:api")
include(":feature:editor:impl")
include(":feature:home:api")
include(":feature:home:impl")
