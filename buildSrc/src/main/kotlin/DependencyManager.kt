import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

fun DependencyHandler.addAppModuleDependencies() {
    implementation(project(ModuleDependency.LIB))
    AppDependency.getAll().forEach { implementation(it) }
}

fun DependencyHandler.getLibModuleDependencies() = LibDependency.getAll()


fun DependencyHandler.addTestDependencies() {

    androidTestImplementation(TestLibraryDependency.TEST_RUNNER)
    androidTestImplementation(TestLibraryDependency.ESPRESSO_CORE)
    androidTestImplementation(TestLibraryDependency.KLUENT_ANDROID)
    androidTestImplementation(TestLibraryDependency.MOCKITO_ANDROID)
    androidTestImplementation(TestLibraryDependency.ANDROID_X_TEST_EXT)

    //testImplementation(project(ModuleDependency.LIBRARY_TEST_UTILS))
    testImplementation(TestLibraryDependency.JUNIT)
    testImplementation(TestLibraryDependency.KLUENT)
    testImplementation(TestLibraryDependency.MOCKITO_INLINE)
    testImplementation(TestLibraryDependency.MOCKITO_KOTLIN)
    testImplementation(TestLibraryDependency.ANDROID_X_CORE_TESTING)
    testImplementation(TestLibraryDependency.ANDROID_X_TEST_RULES)

    testImplementation(TestLibraryDependency.KOIN_TEST)

//    testImplementation(TestLibraryDependency.MOCKK)
//    testImplementation(TestLibraryDependency.POWER_MOCK_JUNIT)
//    testImplementation(TestLibraryDependency.POWER_MOCK_MOCKITO2)

}

/*
 * These extensions mimic the extensions that are generated on the fly by Gradle.
 * They are used here to provide above dependency syntax that mimics Gradle Kotlin DSL
 * syntax in module\build.gradle.kts files.
 */
@Suppress("detekt.UnusedPrivateMember")
private fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? =
    add("implementation", dependencyNotation)

@Suppress("detekt.UnusedPrivateMember")
private fun DependencyHandler.api(dependencyNotation: Any): Dependency? =
    add("api", dependencyNotation)

@Suppress("detekt.UnusedPrivateMember")
private fun DependencyHandler.kapt(dependencyNotation: Any): Dependency? =
    add("kapt", dependencyNotation)

private fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency? =
    add("testImplementation", dependencyNotation)

private fun DependencyHandler.androidTestImplementation(dependencyNotation: Any): Dependency? =
    add("androidTestImplementation", dependencyNotation)

private fun DependencyHandler.project(
    path: String,
    configuration: String? = null
): ProjectDependency {
    val notation = if (configuration != null) {
        mapOf("path" to path, "configuration" to configuration)
    } else {
        mapOf("path" to path)
    }

    return uncheckedCast(project(notation))
}

@Suppress("unchecked_cast", "nothing_to_inline", "detekt.UnsafeCast")
private inline fun <T> uncheckedCast(obj: Any?): T = obj as T