plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin apply false
    with(Plugins.Shadow) { id(id) version (version) }
}

allprojects {
    group = "org.byteflow"
    version = "0.1.0-SNAPSHOT"

    apply(plugin = "maven-publish")

    repositories {
        mavenLocal {
            content {
                includeGroup("org.jacodb")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
    }
}

tasks.shadowJar {
    archiveBaseName = rootProject.name
    archiveClassifier = ""
    archiveVersion = ""
}

tasks.wrapper {
    gradleVersion = "8.3"
    distributionType = Wrapper.DistributionType.ALL
}
