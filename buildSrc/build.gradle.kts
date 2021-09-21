plugins {
    java
    `java-gradle-plugin`
    `kotlin-dsl`
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

dependencies {
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.13.2")

}

tasks.withType<Test>().configureEach {
    testLogging { showStandardStreams = true }
}
/*
gradlePlugin {
    plugins {
        create("maven-publisher") {
            id = "maven-publishero"
            implementationClass = "MavenPublisherPlugin"
        }
    }
}*/
