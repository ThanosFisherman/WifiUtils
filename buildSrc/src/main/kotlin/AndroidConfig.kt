object AndroidConfig {
    const val COMPILE_SDK_VERSION = 29
    const val MIN_SDK_VERSION = 15
    const val TARGET_SDK_VERSION = 29
    const val BUILD_TOOLS_VERSION = "29.0.0"

    const val TEST_INSTRUMENTATION_RUNNER = "android.support.test.runner.AndroidJUnitRunner"
}

interface BuildType {

    companion object {
        const val RELEASE = "release"
        const val DEBUG = "debug"
    }

    val isMinifyEnabled: Boolean
    val isShrinkResources: Boolean
    val manifestPlaceholders: Map<String, String>
    val isDebuggable: Boolean
}

object BuildTypeDebug : BuildType {
    override val isMinifyEnabled = false
    override val isShrinkResources = false
    override val isDebuggable = true
    override val manifestPlaceholders = mapOf("crashlyticsEnabled" to "false", "performanceEnabled" to "false")
}

object BuildTypeRelease : BuildType {
    override val isMinifyEnabled = false
    override val isShrinkResources = false
    override val isDebuggable = false
    override val manifestPlaceholders = mapOf("crashlyticsEnabled" to "true", "performanceEnabled" to "true")
}


