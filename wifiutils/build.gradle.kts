import org.jetbrains.kotlin.konan.properties.hasProperty
import java.util.*

plugins {
    id(GradlePluginId.ANDROID_LIBRARY)
    id(GradlePluginId.KOTLIN_ANDROID)
    id(GradlePluginId.KOTLIN_ANDROID_EXTENSIONS)
    // Documentation for our code
    id(GradlePluginId.DOKKA) version GradlePluginVersion.DOKKA_VERSION
    // Publication to bintray
    id(GradlePluginId.BINTRAY) version GradlePluginVersion.BINTRAY_VERSION
    // Maven publication
    `maven-publish`
}

android {
    compileSdkVersion(AndroidConfig.COMPILE_SDK_VERSION)
    //buildToolsVersion("29.0.3")


    defaultConfig {
        minSdkVersion(AndroidConfig.MIN_SDK_VERSION)
        targetSdkVersion(AndroidConfig.TARGET_SDK_VERSION)
        versionCode = Artifact.VERSION_CODE
        versionName = Artifact.VERSION_NAME
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        getByName(BuildType.DEBUG) {
            isMinifyEnabled = BuildTypeDebug.isMinifyEnabled
            isDebuggable = BuildTypeDebug.isDebuggable
        }

        getByName(BuildType.RELEASE) {
            isMinifyEnabled = BuildTypeRelease.isMinifyEnabled
            isDebuggable = BuildTypeRelease.isDebuggable
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    getLibModuleDependencies().forEach {
        if (it.contains("elvis", true)) {
            api(it) { isTransitive = true }
        } else {
            implementation(it)
        }
    }
    addTestDependencies()
}


val dokkaTask by tasks.creating(org.jetbrains.dokka.gradle.DokkaTask::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/dokka"
}

val dokkaJar by tasks.creating(Jar::class) {
    archiveClassifier.set("dokka")
    from("$buildDir/dokka")
    dependsOn(dokkaTask)
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

val artifactDir = "$buildDir/outputs/aar/${project.name}-release.aar"

publishing {
    publications {
        create<MavenPublication>(Artifact.ARTIFACT_NAME) {
            //from(components["java"]) kinda broken on android
            groupId = Artifact.ARTIFACT_GROUP
            artifactId = Artifact.ARTIFACT_NAME
            version = Artifact.VERSION_NAME
            artifacts {
                artifact(sourcesJar)
                artifact(dokkaJar)
                artifact(artifactDir)
            }

            pom.withXml {
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
                val dependenciesNode = asNode().appendNode("dependencies")
                val configurationNames = arrayOf("implementation")
                configurationNames.forEach { configurationName ->
                    configurations[configurationName].allDependencies.distinct().forEach {
                        if (it.group != null && it.name != null) {
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", it.group)
                            dependencyNode.appendNode("artifactId", it.name)
                            dependencyNode.appendNode("version", it.version)
                            dependencyNode.appendNode("scope", "runtime")
                        }
                    }
                }
            }
        }
    }
}

bintray {
    val properties = Properties()
    properties.load(project.rootProject.file("local.properties").inputStream())

    // Getting bintray user and key from properties file or command line
    user =
        if (properties.hasProperty("bintrayUser")) properties.getProperty("bintrayUser") as String else "thanosfisherman"
    key =
        if (properties.hasProperty("bintrayKey")) properties.getProperty("bintrayKey") as String else ""

    // Automatic publication enabled
    publish = true
    dryRun = false

    // Set maven publication onto bintray plugin
    setPublications(Artifact.ARTIFACT_NAME)

    // Configure package
    pkg.apply {
        repo = "maven"
        name = Artifact.BINTRAY_NAME
        setLicenses("Apache-2.0")
        setLabels("Kotlin", "android", "wifi", "utils")
        vcsUrl = Artifact.POM_SCM_URL
        websiteUrl = Artifact.POM_URL
        issueTrackerUrl = Artifact.POM_ISSUE_URL
        githubRepo = Artifact.GITHUB_REPO
        githubReleaseNotesFile = Artifact.GITHUB_README

        // Configure version
        version.apply {
            name = Artifact.VERSION_NAME
            desc = Artifact.POM_DESC
            released = Date().toString()
            vcsTag = Artifact.VERSION_NAME
        }
    }
}
