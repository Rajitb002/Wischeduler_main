// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}
buildscript {
    repositories {
        google()  // Ensure this repository is included
        mavenCentral()
    }
    dependencies {
        classpath(libs.google.services)  // Ensure you have the latest version

    }
}