package com.thanosfisherman.mavenpublisher.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.Artifact
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.io.File

open class HelloExtension(
        var greeting: String = "Hello",
        var name: String = "buddy"
)

class MavenPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        }
    }



fun Project.configPlugin() {
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
            from(sourceSets.getByName("main").java.srcDirs)
        } else {
            from(sourceSets.getByName("main").java.srcDirs)
        }
    }

    artifacts {
        archives(sourcesJar)
        archives(dokkaJar)
    }

    credentialsMap.forEach { extra.set(it.key, it.value) }

    publishing {
        publications {
            create<MavenPublication>(Artifact.ARTIFACT_NAME) {
                groupId = Artifact.ARTIFACT_GROUP
                artifactId = Artifact.ARTIFACT_NAME
                version = Artifact.VERSION_NAME

                if (project.plugins.findPlugin("com.android.library") != null) {
                    artifact("$buildDir/outputs/aar/${project.name}-release.aar")
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