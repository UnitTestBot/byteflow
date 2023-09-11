import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin
    with(Plugins.Shadow) { id(id) version (version) }
    application
    `maven-publish`
}

group = "org.byteflow"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Kotlin
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    // Logging
    implementation(Libs.kotlin_logging)
    implementation(Libs.slf4j_simple)

    // Main dependencies
    implementation(Libs.jacodb_api)
    implementation(Libs.jacodb_core)
    implementation(Libs.jacodb_analysis)
    implementation(Libs.clikt)
    implementation(Libs.kotlinx_serialization_json)
    implementation(Libs.kotlinx_coroutines_core)

    // JUnit
    testImplementation(platform(Libs.junit_bom))
    testImplementation(Libs.junit_jupiter)

    // Test dependencies
    testImplementation(kotlin("test"))
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

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

application {
    mainClass = "org.byteflow.CliKt"
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
