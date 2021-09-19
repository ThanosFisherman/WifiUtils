object GradlePluginVersion {
    const val ANDROID_GRADLE = "7.0.2"
    const val KOTLIN = CoreVersion.KOTLIN
    const val DOKKA_VERSION = "1.5.30"
    const val DEPENDENCY_UPDATE_VERSION = "0.39.0"
}

object GradlePluginId {
    const val ANDROID_GRADLE_PLUGIN = "com.android.tools.build:gradle:${GradlePluginVersion.ANDROID_GRADLE}"
    const val KOTLIN_GRADLE_PLUGIN = "org.jetbrains.kotlin:kotlin-gradle-plugin:${GradlePluginVersion.KOTLIN}"

    const val ANDROID_APPLICATION = "com.android.application"
    const val ANDROID_LIBRARY = "com.android.library"
    const val DOKKA = "org.jetbrains.dokka"

    const val DEPENDENCY_UPDATE = "com.github.ben-manes.versions"

}

