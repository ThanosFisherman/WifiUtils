object GradlePluginVersion {
    const val ANDROID_GRADLE = "4.0.1"
    const val KOTLIN = CoreVersion.KOTLIN
    const val SAFE_ARGS = CoreVersion.NAVIGATION
    const val DOKKA_VERSION = "0.10.1"
    const val BINTRAY_VERSION = "1.8.5"

    const val KTLINT_GRADLE = "9.2.1"
    const val DETEKT = "1.9.1"
    const val GRADLE_VERSION_PLUGIN = "0.29.0"
    const val GOOGLE_PLAY_SERVICES_VERSION_PLUGIN = "4.3.3"
    const val CRASHLYTICS_VERSION_PLUGIN = "2.1.1"
    const val PERFORMANCE_VERSION_PLUGIN = "1.3.1"

}

object GradlePluginId {
    const val ANDROID_GRADLE_PLUGIN = "com.android.tools.build:gradle:${GradlePluginVersion.ANDROID_GRADLE}"
    const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${GradlePluginVersion.KOTLIN}"
    const val SAFE_ARGS_GRADLE_PLUGIN = "androidx.navigation:navigation-safe-args-gradle-plugin:${GradlePluginVersion.SAFE_ARGS}"
    const val GOOGLE_PLAY_SERVICES_GRADLE_PLUGIN = "com.google.gms:google-services:${GradlePluginVersion.GOOGLE_PLAY_SERVICES_VERSION_PLUGIN}"
    const val FABRIC_GRADLE_PLUGIN = "com.google.firebase:firebase-crashlytics-gradle:${GradlePluginVersion.CRASHLYTICS_VERSION_PLUGIN}"
    const val PERFORMANCE_GRADLE_PLUGIN = "com.google.firebase:perf-plugin:${GradlePluginVersion.PERFORMANCE_VERSION_PLUGIN}"

    const val ANDROID_APPLICATION = "com.android.application"
    const val ANDROID_LIBRARY = "com.android.library"
    const val KOTLIN_ANDROID = "org.jetbrains.kotlin.android"
    const val KOTLIN_ANDROID_EXTENSIONS = "org.jetbrains.kotlin.android.extensions"
    const val DOKKA = "org.jetbrains.dokka"
    const val BINTRAY = "com.jfrog.bintray"
    const val KOTLIN_KAPT = "org.jetbrains.kotlin.kapt"
    const val DETEKT = "io.gitlab.arturbosch.detekt"
    const val KTLINT = "org.jlleitschuh.gradle.ktlint"
    const val DEPENDENCY_UPDATE = "com.github.ben-manes.versions"
    const val SAFE_ARGS = "androidx.navigation.safeargs.kotlin"
    const val GOOGLE_PLAY_SERVICES = "com.google.gms.google-services"
    const val CRASHLYTICS = "com.google.firebase.crashlytics"
    const val PERFORMANCE = "com.google.firebase.firebase-perf"
}

