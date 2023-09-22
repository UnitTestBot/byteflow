import org.byteflow.gradle.RunAnalyzerTask
import org.byteflow.gradle.analysisConfig
import org.byteflow.gradle.getMethodsForClasses

plugins {
    `java-library`
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

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
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

tasks.register<RunAnalyzerTask>("analyzeSqlInjectionSampleFP") {
    dependsOn(tasks.compileJava)
    config = analysisConfig(
        "SQL" to mapOf(
            "UnitResolver" to "singleton",
        ),
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methodsForCp = { cp ->
        getMethodsForClasses(cp, startClasses = listOf("com.example.SqlInjectionSampleFP"))
    }
    outputPath = "report-sql-fp.sarif"
}

tasks.register<RunAnalyzerTask>("analyzeSqlInjectionSampleTP") {
    dependsOn(tasks.compileJava)
    config = analysisConfig(
        "SQL" to mapOf(
            "UnitResolver" to "singleton",
        ),
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methodsForCp = { cp ->
        getMethodsForClasses(cp, startClasses = listOf("com.example.SqlInjectionSampleTP"))
    }
    outputPath = "report-sql-tp.sarif"
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
