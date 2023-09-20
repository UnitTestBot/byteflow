@file:OptIn(ExperimentalTime::class)

import kotlinx.coroutines.runBlocking
import org.byteflow.gradle.RunAnalyzerExtendedTask
import org.jacodb.analysis.AnalysisConfig
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcMethod
import org.jacodb.api.cfg.JcInst
import org.jacodb.api.ext.findClass
import org.jacodb.impl.features.hierarchyExt
import kotlin.time.ExperimentalTime

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

dependencies {
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")
    implementation("javax.mail:mail:1.4.7")
}

sourceSets {
    main {
        java {
            // Support library
            srcDir("juliet-java-test-suite/juliet-support/src/main/java")

            // NPE
            srcDir("juliet-java-test-suite/juliet-cwe476/src/main/java")
            srcDir("juliet-java-test-suite/juliet-cwe690/src/main/java")

            // Unused
            srcDir("juliet-java-test-suite/juliet-cwe563/src/main/java")

            // SQL
            srcDir("juliet-java-test-suite/juliet-cwe89/src/main/java")
        }
    }
}

tasks.runAnalyzer {
    dependsOn(tasks.compileJava)
}

private val commonJulietBans = listOf(
    // TODO: containers not supported
    "_72", "_73", "_74",

    // TODO/Won't fix(?): dead parts of switches shouldn't be analyzed
    "_15",

    // TODO/Won't fix(?): passing through channels not supported
    "_75",

    // TODO/Won't fix(?): constant private/static methods not analyzed
    "_11", "_08",

    // TODO/Won't fix(?): unmodified non-final private variables not analyzed
    "_05", "_07",

    // TODO/Won't fix(?): unmodified non-final static variables not analyzed
    "_10", "_14",
)

fun julietClasses(
    cp: JcClasspath,
): Sequence<JcClassOrInterface> = runBlocking {
    val hierarchyExt = cp.hierarchyExt()
    val baseClass = cp.findClass("juliet.support.AbstractTestCase")
    hierarchyExt.findSubClasses(baseClass, false)
}

fun julietMethods(classes: () -> List<JcClassOrInterface>): List<JcMethod> {
    logger.quiet("Searching classes...")
    val startClasses = classes()

    logger.quiet("startClasses: (${startClasses.size})")
    for (clazz in startClasses) {
        logger.quiet("  - $clazz")
    }

    logger.quiet("Filtering methods...")
    val startMethods = startClasses
        .flatMap { clazz ->
            listOf(
                clazz.declaredMethods.single { it.name == "good" },
                clazz.declaredMethods.single { it.name == "bad" }
            )
        }
        .distinct()

    logger.quiet("Methods: (${startMethods.size})")
    for (m in startMethods) {
        logger.quiet("  - $m")
    }

    return startMethods
}

fun julietResolver(cweNum: Int): (JcInst) -> String = { inst ->
    val registeredLocation = inst.location.method.declaration.location
    val classFileBaseName = inst.location.method.enclosingClass.name.replace('.', '/')
    if (registeredLocation.path.contains("build/classes/java/main")) {
        val src = registeredLocation.path.replace("build/classes/java/main", "juliet-java-test-suite/juliet-cwe${cweNum}/src/main/java")
        "file://" + File(src).resolve(classFileBaseName.substringBefore('$')).path + ".java"
    } else {
        File(registeredLocation.path).resolve(classFileBaseName).path + ".class"
    }
}

tasks.register<RunAnalyzerExtendedTask>("analyzeJulietCwe476") {
    println("Registering '${this.name}' task")
    dependsOn(tasks.compileJava)

    config = AnalysisConfig(
        mapOf(
            "NPE" to emptyMap(),
            // "Unused" to mapOf(
            //     "UnitResolver" to "class",
            // ),
            // "SQL" to emptyMap(),
        )
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methods = { cp ->
        julietMethods {
            val specificBans = listOf(
                "null_check_after_deref",
            )
            julietClasses(cp)
                .filter { it.name.contains("CWE476") }
                .filterNot { clazz -> (commonJulietBans + specificBans).any { ban -> clazz.name.contains(ban) } }
                .sortedBy { it.name }
                .toList()
        }
    }
    outputPath = "report-cwe476.sarif"
    resolver = julietResolver(476)
}


tasks.register<RunAnalyzerExtendedTask>("analyzeJulietCwe690") {
    println("Registering '${this.name}' task")
    dependsOn(tasks.compileJava)

    config = AnalysisConfig(
        mapOf(
            "NPE" to emptyMap(),
            // "Unused" to mapOf(
            //     "UnitResolver" to "class",
            // ),
            // "SQL" to emptyMap(),
        )
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methods = { cp ->
        julietMethods {
            val specificBans = listOf<String>()
            julietClasses(cp)
                .filter { it.name.contains("CWE690") }
                .filterNot { clazz -> (commonJulietBans + specificBans).any { ban -> clazz.name.contains(ban) } }
                .sortedBy { it.name }
                .toList()
        }
    }
    outputPath = "report-cwe690.sarif"
    resolver = julietResolver(690)
}


tasks.register<RunAnalyzerExtendedTask>("analyzeJulietCwe563") {
    println("Registering '${this.name}' task")
    dependsOn(tasks.compileJava)

    config = AnalysisConfig(
        mapOf(
            // "NPE" to emptyMap(),
            "Unused" to mapOf(
                "UnitResolver" to "class",
            ),
            // "SQL" to emptyMap(),
        )
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methods = { cp ->
        julietMethods {
            val specificBans = listOf(
                // Unused variables are already optimized out by cfg
                "unused_uninit_variable_",
                "unused_init_variable_int",
                "unused_init_variable_long",
                "unused_init_variable_String_",

                // Unused variable is generated by cfg (!!)
                "unused_value_StringBuilder_17",

                // Expected answers are strange, seems to be problem in tests
                "_12",

                // The variable isn't expected to be detected as unused actually
                "_81"
            )
            julietClasses(cp)
                .filter { it.name.contains("CWE563") }
                .filterNot { clazz -> (commonJulietBans + specificBans).any { ban -> clazz.name.contains(ban) } }
                .sortedBy { it.name }
                .toList()
        }
    }
    outputPath = "report-cwe563.sarif"
    resolver = julietResolver(563)
}

tasks.register<RunAnalyzerExtendedTask>("analyzeJulietCwe89") {
    println("Registering '${this.name}' task")
    dependsOn(tasks.compileJava)

    config = AnalysisConfig(
        mapOf(
            // "NPE" to emptyMap(),
            // "Unused" to mapOf(
            //     "UnitResolver" to "class",
            // ),
            "SQL" to emptyMap(),
        )
    )
    dbLocation = "index.db"
    classpath = sourceSets["main"].runtimeClasspath.asPath
    methods = { cp ->
        julietMethods {
            val specificBans = listOf(
                // Not working yet (#156)
                "s03", "s04"
            )
            julietClasses(cp)
                .filter { it.name.contains("CWE89") }
                .filterNot { clazz -> (commonJulietBans + specificBans).any { ban -> clazz.name.contains(ban) } }
                .sortedBy { it.name }
                .toList()
        }
    }
    outputPath = "report-cwe89.sarif"
    resolver = julietResolver(89)
}

tasks.wrapper {
    gradleVersion = "8.3"
    distributionType = Wrapper.DistributionType.ALL
}
