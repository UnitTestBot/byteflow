import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    // java
    application
    `maven-publish`
    idea
    // `java-test-fixtures`
}

group = "org.usvm"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jacodb:jacodb-api:1.2-SNAPSHOT")
    implementation("org.jacodb:jacodb-core:1.2-SNAPSHOT")
    implementation("org.jacodb:jacodb-analysis:1.2-SNAPSHOT")
    // implementation(testFixtures("org.jacodb:jacodb-core:1.2-SNAPSHOT"))
    implementation("com.github.ajalt.clikt:clikt:4.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.github.oshai:kotlin-logging:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

application {
    mainClass = "org.usvm.analyzer.CliKt"
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8")
}

tasks.startScripts {
    applicationName = rootProject.name
}

tasks.shadowJar {
    archiveBaseName = rootProject.name
    archiveClassifier = ""
    archiveVersion = ""
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

tasks.wrapper {
    gradleVersion = "8.3"
    distributionType = Wrapper.DistributionType.ALL
}
