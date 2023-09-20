plugins {
    java
    id("io.github.UnitTestBot.byteflow") version "c09d96f3c5"
    // id("byteflow-gradle") version "..."
}

buildscript {
    // Repositories for plugin dependencies:
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

byteflow {
    configFile = layout.projectDirectory.file("configs/config.json")
    startClasses = listOf(
        // "com.example.NpeExamples",
        "com.example.SqlInjectionSampleFP",
        "com.example.SqlInjectionSampleTP",
    )
    classpath = sourceSets["main"].runtimeClasspath.asPath
    dbLocation = "index.db"

    useUsvmAnalysis = project.findProperty("useUsvmAnalysis")?.toString()?.toBoolean() ?: false
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
