import java.util.*

plugins {
    id(GradlePluginId.ANDROID_LIBRARY)
    kotlin("android")
    // Documentation for our code
    id(GradlePluginId.DOKKA) version GradlePluginVersion.DOKKA_VERSION
    // Maven publication
    `maven-publish`
}

android {
    compileSdk = AndroidConfig.COMPILE_SDK_VERSION
    //buildToolsVersion("30.0.3")


    defaultConfig {
        minSdk = AndroidConfig.MIN_SDK_VERSION
        targetSdk = AndroidConfig.TARGET_SDK_VERSION

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            isMinifyEnabled = BuildTypeDebug.isMinifyEnabled
            isDefault = true
        }

        release {
            isMinifyEnabled = BuildTypeRelease.isMinifyEnabled
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    addLibModuleDependencies()
    //addTestDependencies()
}

