import org.gradle.api.Project
import java.util.*
import kotlin.collections.LinkedHashMap

object Artifact {

    val ARTIFACT_NAME = "wifiutils"
    val ARTIFACT_GROUP = "com.thanosfisherman.wifiutils"
    val VERSION_NAME = "1.6.5"
    val VERSION_CODE = 22
    val POM_URL = "https://github.com/ThanosFisherman/WifiUtils"
    val POM_SCM_URL = "https://github.com/ThanosFisherman/WifiUtils"
    val POM_SCM_CONNECTION = "scm:git:github.com/ThanosFisherman/WifiUtils.git"
    val POM_SCM_DEV_CONNECTION = "scm:git:ssh://github.com/ThanosFisherman/WifiUtils.git"
    val POM_ISSUE_URL = "https://github.com/ThanosFisherman/WifiUtils/issues"
    val POM_DESC = "Library that makes it easy to Connect to WiFi hotspots using ssid and/or bssid"

    val BINTRAY_REPO = "maven"
    val BINTRAY_NAME = "wifiutils"   // Has to be same as your bintray project name
    val GITHUB_REPO = "ThanosFisherman/WifiUtils"
    val GITHUB_README = "README.md"
    val LIBRARY_NAME = "WifiUtils"

    val POM_LICENSE_NAME = "The Apache Software License, Version 2.0"
    val POM_LICENSE_URL = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    val POM_LICENSE_DIST = "http://www.apache.org/licenses/LICENSE-2.0.txt"

    val POM_DEVELOPER_ID = "thanosfisherman"
    val POM_DEVELOPER_NAME = "Thanos Psaridis"
    val DEVELOPER_EMAIL = "psaridis@gmail.com"

    val RELEASE_REPO_URL = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    val SNAPSHOT_REPO_URL = "https://oss.sonatype.org/content/repositories/snapshots"
    val REPO_NAME = "sonatype"
}


val Project.credentialsMap: Map<String, String>
    inline get() =
        LinkedHashMap<String, String>().apply {

            val propertiesFile = rootProject.file("local.properties")
            if (propertiesFile.exists() && propertiesFile.canRead()) {
               try {
                   val properties = Properties()
                   properties.load(propertiesFile.inputStream())
                   this["signing.keyId"] = properties.getProperty("signing.keyId")
                   this["signing.password"] = properties.getProperty("signing.password")
                   this["signing.secretKeyRingFile"] =
                       properties.getProperty("signing.secretKeyRingFile")
                   this["ossrhUsername"] = properties.getProperty("ossrhUsername")
                   this["ossrhPassword"] = properties.getProperty("ossrhPassword")
                   this["sonatypeStagingProfileId"] =
                       properties.getProperty("sonatypeStagingProfileId")
               } catch (e: Exception) {}
            } else {
                this["signing.keyId"] = System.getenv("signing.keyId")
                this["signing.password"] = System.getenv("signing.password")
                this["signing.secretKeyRingFile"] = System.getenv("signing.secretKeyRingFile")
                this["ossrhUsername"] = System.getenv("ossrhUsername")
                this["ossrhPassword"] = System.getenv("ossrhPassword")
                this["sonatypeStagingProfileId"] = System.getenv("sonatypeStagingProfileId")
                //val keystoreFile = project.rootProject.file(rootDir.path + File.separator + System.getenv("keystore_name"))
            }
        }

