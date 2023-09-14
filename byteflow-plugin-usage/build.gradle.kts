plugins {
    kotlin("jvm") version "1.9.10"
    id("io.github.UnitTestBot.byteflow") version "0.1.0-SNAPSHOT"
}

buildscript {
    // Repositories for plugin dependencies:
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

repositories {
    mavenCentral()
}

byteflow {
    configFile = layout.projectDirectory.file("configs/config.json")
    startClasses = listOf("com.example.NpeExamples")
    classpath = sourceSets["main"].runtimeClasspath.asPath
}

tasks.runAnalyzer {
    dependsOn(tasks.compileJava)
}

// Debug task
tasks.register("yeet") {
    println("-".repeat(42))

    println("project.configurations: {")
    for ((k, v) in project.configurations.asMap) {
        println("  $k = $v")
    }
    println("}")

    println("project.sourceSets: {")
    for ((k, v) in project.sourceSets.asMap) {
        println("  $k = $v")
    }
    println("}")

    println("runtimeClasspath = ${sourceSets["main"].runtimeClasspath.asPath}")

    println("-".repeat(42))
}

tasks.wrapper {
    gradleVersion = "8.3"
    distributionType = Wrapper.DistributionType.ALL
}
