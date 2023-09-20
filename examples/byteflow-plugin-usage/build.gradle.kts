import org.byteflow.gradle.RunAnalyzerTask
import org.byteflow.gradle.analysisConfig
import org.byteflow.gradle.getMethodsForClasses

plugins {
    kotlin("jvm") version "1.9.10"
    id("io.github.UnitTestBot.byteflow") version "0.1.0-SNAPSHOT"
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

byteflow {
    configFile = layout.projectDirectory.file("configs/config.json")
    startClasses = listOf(
        "com.example.NpeExamples",
        "com.example.SqlInjectionSample1",
        "com.example.SqlInjectionSample2",
    )
    classpath = sourceSets["main"].runtimeClasspath.asPath
    dbLocation = "index.db"
}

tasks.runAnalyzer {
    dependsOn(tasks.compileJava)
}

// ------------------------------------------------------------------------------------------------
// Specific analysis tasks.
//

tasks.register<RunAnalyzerTask>("analyzeNpeExamples") {
    dependsOn(tasks.compileJava)
    config = analysisConfig(
        "NPE" to mapOf(
            "UnitResolver" to "singleton",
        ),
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methodsForCp = { cp ->
        getMethodsForClasses(cp, startClasses = listOf("com.example.NpeExamples"))
    }
    outputPath = "report-npe.sarif"
}

tasks.register<RunAnalyzerTask>("analyzeSqlInjectionSample1") {
    dependsOn(tasks.compileJava)
    config = analysisConfig(
        "SQL" to mapOf(
            "UnitResolver" to "singleton",
        ),
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methodsForCp = { cp ->
        getMethodsForClasses(cp, startClasses = listOf("com.example.SqlInjectionSample1"))
    }
    outputPath = "report-sql1.sarif"
}

tasks.register<RunAnalyzerTask>("analyzeSqlInjectionSample2") {
    dependsOn(tasks.compileJava)
    config = analysisConfig(
        "SQL" to mapOf(
            "UnitResolver" to "singleton",
        ),
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methodsForCp = { cp ->
        getMethodsForClasses(cp, startClasses = listOf("com.example.SqlInjectionSample2"))
    }
    outputPath = "report-sql2.sarif"
}

// ------------------------------------------------------------------------------------------------

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
