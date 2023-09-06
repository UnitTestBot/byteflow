import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    application
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
    implementation("com.github.ajalt.clikt:clikt:4.2.0")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

application {
    mainClass = "org.usvm.analyzer.cli.CliKt"
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

tasks.wrapper {
    gradleVersion = "8.3"
    distributionType = Wrapper.DistributionType.ALL
}
