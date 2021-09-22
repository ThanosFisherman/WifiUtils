plugins {
    id(GradlePluginId.ANDROID_LIBRARY)
    kotlin("android")
    // Documentation for our code
    id(GradlePluginId.DOKKA) version GradlePluginVersion.DOKKA_VERSION
    // Maven publication
    `maven-publish`
    signing
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

val dokkaTask by tasks.creating(org.jetbrains.dokka.gradle.DokkaTask::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    outputDirectory.set(File("$buildDir/dokka"))
    //documentationFileName.set("README.md")
}

val dokkaJar by tasks.creating(Jar::class) {
    archiveClassifier.set("dokka")
    from("$buildDir/dokka")
    dependsOn(dokkaTask)
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    if (project.plugins.findPlugin("com.android.library") != null) {
        from(android.sourceSets.getByName("main").java.srcDirs)
    } else {
        from(sourceSets.getByName("main").java.srcDirs)
    }
}

artifacts {
    archives(sourcesJar)
    archives(dokkaJar)
}

credentialsMap.forEach { extra.set(it.key, it.value) }

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>(Artifact.ARTIFACT_NAME) {
                groupId = Artifact.ARTIFACT_GROUP
                artifactId = Artifact.ARTIFACT_NAME
                version = Artifact.VERSION_NAME

                if (project.plugins.findPlugin("com.android.library") != null) {
                    artifact("$buildDir/outputs/aar/${project.name}-debug.aar")
                } else {
                    from(components["java"])
                    //artifact("$buildDir/libs/${project.getName()}-${version}.jar")
                }
                artifacts {
                    artifact(sourcesJar)
                    artifact(dokkaJar)
                }

                pom {
                    name.set(Artifact.LIBRARY_NAME)
                    description.set(Artifact.POM_DESC)
                    url.set(Artifact.POM_URL)
                    licenses {
                        license {
                            name.set(Artifact.POM_LICENSE_NAME)
                            url.set(Artifact.POM_LICENSE_URL)
                            distribution.set(Artifact.POM_URL)
                        }
                    }
                    developers {
                        developer {
                            id.set(Artifact.POM_DEVELOPER_ID)
                            name.set(Artifact.POM_DEVELOPER_NAME)
                            email.set(Artifact.DEVELOPER_EMAIL)
                        }
                    }
                    scm {
                        connection.set(Artifact.POM_SCM_CONNECTION)
                        developerConnection.set(Artifact.POM_SCM_DEV_CONNECTION)
                        url.set(Artifact.POM_SCM_URL)
                    }
                    repositories {
                        maven {
                            // change URLs to point to your repos, e.g. http://my.org/repo
                            val releasesRepoUrl = uri(Artifact.RELEASE_REPO_URL)
                            val snapshotsRepoUrl = uri(Artifact.SNAPSHOT_REPO_URL)
                            name = Artifact.REPO_NAME
                            url = if (version.toString()
                                    .endsWith("SNAPSHOT")
                            ) snapshotsRepoUrl else releasesRepoUrl
                            credentials {
                                username = credentialsMap["ossrhUsername"]
                                password = credentialsMap["ossrhPassword"]
                            }
                        }
                    }

                    //  hack if you wanna include any transitive dependencies. I'm a hackur indeed
                    /*            withXml {
                                    asNode().apply {
                                        appendNode("description", Artifact.POM_DESC)
                                        appendNode("name", Artifact.LIBRARY_NAME)
                                        appendNode("url", Artifact.POM_URL)
                                        appendNode("licenses").appendNode("license").apply {
                                            appendNode("name", Artifact.POM_LICENSE_NAME)
                                            appendNode("url", Artifact.POM_LICENSE_URL)
                                            appendNode("distribution", Artifact.POM_LICENSE_DIST)
                                        }
                                        appendNode("developers").appendNode("developer").apply {
                                            appendNode("id", Artifact.POM_DEVELOPER_ID)
                                            appendNode("name", Artifact.POM_DEVELOPER_NAME)
                                        }
                                        appendNode("scm").apply {
                                            appendNode("url", Artifact.POM_SCM_URL)
                                        }
                                    }
                                }*/
                }
            }
        }
    }
    signing {
        sign(publishing.publications[Artifact.ARTIFACT_NAME])
    }

}
