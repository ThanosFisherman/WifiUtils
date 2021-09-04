import kotlin.reflect.full.memberProperties

private object LibraryVersion {

    //Core Versions
    const val ktxCore = "1.6.0"
    const val androidxVersion = "1.3.1"
    const val constraintLayoutVersion = "2.1.0"
    const val recyclerViewVersion = "1.2.1"
    const val materialVersion = "1.4.0"
    const val annotationsVersion = "1.2.0"

}

// Versions consts that are used across libraries and Gradle plugins
object CoreVersion {
    const val KOTLIN = "1.5.30"
}

object LibDependency {

    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${CoreVersion.KOTLIN}"
    const val ktxCore = "androidx.core:core-ktx:${LibraryVersion.ktxCore}"
    const val androidXAnnotations = "androidx.annotation:annotation:${LibraryVersion.annotationsVersion}"

    fun getAll() = LibDependency::class.memberProperties
        .filter { it.isConst }
        .map { it.getter.call().toString() }
        .toSet()
}

object AppDependency {

    const val androidX = "androidx.appcompat:appcompat:${LibraryVersion.androidxVersion}"
    const val constraintLayout =
        "androidx.constraintlayout:constraintlayout:${LibraryVersion.constraintLayoutVersion}"
    const val ktxCore = "androidx.core:core-ktx:${LibraryVersion.ktxCore}"
    const val recyclerView =
        "androidx.recyclerview:recyclerview:${LibraryVersion.recyclerViewVersion}"
    const val material = "com.google.android.material:material:${LibraryVersion.materialVersion}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${CoreVersion.KOTLIN}"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${CoreVersion.KOTLIN}"


    fun getAll() = AppDependency::class.memberProperties
        .filter { it.isConst }
        .map { it.getter.call().toString() }
        .toSet()
}
