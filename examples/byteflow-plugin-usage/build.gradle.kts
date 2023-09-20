import kotlinx.coroutines.runBlocking
import org.byteflow.AnalysesOptions
import org.byteflow.AnalysisType
import org.byteflow.gradle.RunAnalyzerExtendedTask
import org.jacodb.analysis.AnalysisConfig
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClassProcessingTask
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcMethod
import java.util.concurrent.ConcurrentHashMap

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
// Utilities for specific analysis tasks.
//

fun getMethods(
    cp: JcClasspath,
    startClasses: List<String>,
): List<JcMethod> {
    logger.quiet("Searching classes...")
    val startJcClasses = ConcurrentHashMap.newKeySet<JcClassOrInterface>()
    runBlocking {
        cp.execute(object : JcClassProcessingTask {
            override fun process(clazz: JcClassOrInterface) {
                if (startClasses.any { clazz.name.startsWith(it) }) {
                    startJcClasses.add(clazz)
                }
            }
        })
    }

    logger.quiet("startJcClasses: (${startJcClasses.size})")
    for (clazz in startJcClasses) {
        logger.quiet("  - $clazz")
    }

    logger.quiet("Filtering methods...")
    val startJcMethods = startJcClasses
        .flatMap { it.declaredMethods }
        .filter { !it.isPrivate }
        .distinct()

    logger.quiet("startJcMethods: (${startJcMethods.size})")
    for (method in startJcMethods) {
        logger.quiet("  - $method")
    }

    return startJcMethods
}

fun analysisConfig(vararg pairs: Pair<AnalysisType, AnalysesOptions>): AnalysisConfig {
    return AnalysisConfig(pairs.toMap())
}

// ------------------------------------------------------------------------------------------------
// Specific analysis tasks.
//

tasks.register<RunAnalyzerExtendedTask>("analyzeNpeExamples") {
    dependsOn(tasks.compileJava)
    config = analysisConfig(
        "NPE" to mapOf(
            "UnitResolver" to "singleton",
        ),
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methods = { cp -> getMethods(cp, startClasses = listOf("com.example.NpeExamples")) }
    outputPath = "report-npe.sarif"
}

tasks.register<RunAnalyzerExtendedTask>("analyzeSqlInjectionSample1") {
    dependsOn(tasks.compileJava)
    config = analysisConfig(
        "SQL" to mapOf(
            "UnitResolver" to "singleton",
        ),
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methods = { cp -> getMethods(cp, startClasses = listOf("com.example.SqlInjectionSample1")) }
    outputPath = "report-sql1.sarif"
}

tasks.register<RunAnalyzerExtendedTask>("analyzeSqlInjectionSample2") {
    dependsOn(tasks.compileJava)
    config = analysisConfig(
        "SQL" to mapOf(
            "UnitResolver" to "singleton",
        ),
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methods = { cp -> getMethods(cp, startClasses = listOf("com.example.SqlInjectionSample2")) }
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
