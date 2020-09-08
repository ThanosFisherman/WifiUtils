private object TestLibraryVersion {
    const val JUNIT = "4.13"
    const val KLUENT = "1.61"
    const val TEST_RUNNER = "1.0.2"
    const val ESPRESSO_CORE = "3.0.2"
    const val MOCKITO = "3.5.2"
    const val MOCKITO_KOTLIN = "2.1.0"
    const val ANDROID_X_TEST = "1.2.0"
    const val ANDROID_X_TEST_EXT = "1.1.1"
}

object TestLibraryDependency {
    const val JUNIT = "junit:junit:${TestLibraryVersion.JUNIT}"
    const val KLUENT_ANDROID = "org.amshove.kluent:kluent-android:${TestLibraryVersion.KLUENT}"
    const val KLUENT = "org.amshove.kluent:kluent:${TestLibraryVersion.KLUENT}"
    const val TEST_RUNNER = "com.android.support.test:runner:${TestLibraryVersion.TEST_RUNNER}"
    const val ESPRESSO_CORE = "com.android.support.test.espresso:espresso-core:${TestLibraryVersion.ESPRESSO_CORE}"
    const val MOCKITO_INLINE = "org.mockito:mockito-inline:${TestLibraryVersion.MOCKITO}"
    const val MOCKITO_ANDROID = "org.mockito:mockito-android:${TestLibraryVersion.MOCKITO}"
    const val MOCKITO_KOTLIN = "com.nhaarman.mockitokotlin2:mockito-kotlin:${TestLibraryVersion.MOCKITO_KOTLIN}"
    const val ANDROID_X_CORE_TESTING = "android.arch.core:core-testing:${TestLibraryVersion.ANDROID_X_TEST}"
    const val ANDROID_X_TEST_RULES = "androidx.test:rules:${TestLibraryVersion.ANDROID_X_TEST}"
    const val ANDROID_X_TEST_EXT = "androidx.test.ext:junit:${TestLibraryVersion.ANDROID_X_TEST_EXT}"
    const val KOIN_TEST = "org.koin:koin-test:2.1.6"

//    const val MOCKK = "io.mockk:mockk:1.9.3"
//
//    const val POWER_MOCK_JUNIT = "org.powermock:powermock-module-junit4:2.0.4"
//    const val POWER_MOCK_MOCKITO2 = "org.powermock:powermock-api-mockito2:2.0.4"
}
