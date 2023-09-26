import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin apply false
    with(Plugins.PluginPublish) { id(id) version (version) } apply false
    with(Plugins.Shadow) { id(id) version (version) } apply false
}

group = "org.byteflow"
version = "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "kotlin")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
    }

    dependencies {
        implementation(platform(kotlin("bom")))

        testImplementation(platform(Libs.junit_bom))
        testImplementation(Libs.junit_jupiter)
        testImplementation(kotlin("test"))
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))

        withSourcesJar()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

repositories {
    mavenCentral()
}

tasks.wrapper {
    gradleVersion = "8.3"
    distributionType = Wrapper.DistributionType.ALL
}
