pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Central plugin version management. Modules apply these plugins without a
    // version; the versions are resolved here. settings.gradle.kts is always
    // evaluated first, so this works regardless of root build script state.
    plugins {
        id("com.android.application") version "8.5.0"
        id("org.jetbrains.kotlin.android") version "1.9.24"
        id("com.google.devtools.ksp") version "1.9.24-1.0.20"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PhotoFinder"
include(":app")
