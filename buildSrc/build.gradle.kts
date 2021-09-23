plugins {
    base
    java
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
    id("com.android.library")

}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    //google()
    gradlePluginPortal()
    mavenCentral()
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.5.30")
        classpath("com.android.tools.build:gradle:7.0.2")
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:7.0.2")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.5.30")
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")

}

tasks.withType<Test>().configureEach {
    testLogging { showStandardStreams = true }
}
gradlePlugin {
    plugins {
        create("maven-publisher") {
            id = "com.thanosfisherman.mavenpublisher"
            implementationClass = "com.thanosfisherman.mavenpublisher.plugin.MavenPublisherPlugin"
        }
    }
}
