import kotlin.reflect.full.memberProperties

private object LibraryVersion {

    //Core Versions
    const val koinVersion = "2.1.6"
    const val timberVersion = "4.7.1"
    const val coroutinesVersion = "1.3.8"
    const val ktxCore = "1.3.1"
    const val multiDexVersion = "1.0.3"

    const val wifiUtilsVersion = "1.6.3"

    const val androidxVersion = "1.2.0"
    const val lifecycleVersionX = "2.2.0"
    const val constraintLayoutVersion = "2.0.0-rc1"
    const val recyclerViewVersion = "1.1.0"
    const val materialVersion = "1.3.0-alpha02"
    const val coilVersion = "0.11.0"
    const val progressButtonVersion = "2.1.0"
    const val flowBindingsVersion = "0.12.0"

}

// Versions consts that are used across libraries and Gradle plugins
object CoreVersion {
    const val KOTLIN = "1.4.0"
    const val KTLINT = "0.36.0"
    const val NAVIGATION = "2.3.0"
}

object LibDependency {

    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${CoreVersion.KOTLIN}"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${CoreVersion.KOTLIN}"
    const val ktxCore = "androidx.core:core-ktx:${LibraryVersion.ktxCore}"
    const val elvis = "com.thanosfisherman.elvis:elvis:3.0"

    fun getAll() = LibDependency::class.memberProperties
        .filter { it.isConst }
        .map { it.getter.call().toString() }
        .toSet()
}

object AppDependency {

    //const val wifiUtilsOnline = "com.thanosfisherman.wifiutils:wifiutils:${LibraryVersion.wifiUtilsVersion}"
    const val multiDex = "com.android.support:multidex:${LibraryVersion.multiDexVersion}"
    const val androidX = "androidx.appcompat:appcompat:${LibraryVersion.androidxVersion}"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:${LibraryVersion.constraintLayoutVersion}"
    const val ktxCore = "androidx.core:core-ktx:${LibraryVersion.ktxCore}"
    const val viewModelLiveData = "androidx.lifecycle:lifecycle-extensions:${LibraryVersion.lifecycleVersionX}"
    const val reactiveStreamsLiveData = "androidx.lifecycle:lifecycle-reactivestreams:${LibraryVersion.lifecycleVersionX}"
    const val lifecycleLiveData = "androidx.lifecycle:lifecycle-livedata-ktx:${LibraryVersion.lifecycleVersionX}"
    const val recyclerView = "androidx.recyclerview:recyclerview:${LibraryVersion.recyclerViewVersion}"
    const val navigationUI = "androidx.navigation:navigation-fragment-ktx:${CoreVersion.NAVIGATION}"
    const val navigationKtx = "androidx.navigation:navigation-ui-ktx:${CoreVersion.NAVIGATION}"
    const val coil = "io.coil-kt:coil:${LibraryVersion.coilVersion}"
    const val material = "com.google.android.material:material:${LibraryVersion.materialVersion}"
    const val viewFlowBindings = "io.github.reactivecircus.flowbinding:flowbinding-android:${LibraryVersion.flowBindingsVersion}"
    const val progressButton = "com.github.razir.progressbutton:progressbutton:${LibraryVersion.progressButtonVersion}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${CoreVersion.KOTLIN}"
    const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${CoreVersion.KOTLIN}"


    fun getAll() = AppDependency::class.memberProperties
        .filter { it.isConst }
        .map { it.getter.call().toString() }
        .toSet()
}
