// Top-level build file. Plugin versions are declared centrally in
// settings.gradle.kts (pluginManagement.plugins); here the plugins are only
// registered for the build without being applied to the root project.
plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("com.google.devtools.ksp") apply false
}
