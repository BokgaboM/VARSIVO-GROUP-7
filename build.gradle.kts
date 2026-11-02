// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // Pins Kotlin 2.4.10 for AGP's built-in Kotlin support. Not applied in :app.
    alias(libs.plugins.kotlin.android) apply false
}
