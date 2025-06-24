// Plugin declarations for use in module-level build.gradle.kts files
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}

// Equivalent to Groovy's `buildscript` block
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")
        // Add more classpaths here only if not using version catalogs
        classpath("com.google.gms:google-services:4.4.1")

    }
}

// Project-wide repository settings
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

// Clean task equivalent in Kotlin DSL
tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
