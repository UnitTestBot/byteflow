@file:Suppress("PublicApiImplicitType", "MemberVisibilityCanBePrivate", "unused", "ConstPropertyName")

object Versions {
    const val annotations = "24.0.1"
    const val clikt = "4.2.0"
    const val dokka = "1.8.20"
    const val gradle_plugin_publish = "1.2.1"
    const val jacodb = "6a37be4272"
    const val junit = "5.9.2"
    const val kotlin = "1.9.10"
    const val kotlin_logging = "5.1.0"
    const val kotlinx_coroutines = "1.7.3"
    const val kotlinx_serialization = "1.6.0"
    const val sarif4k = "0.4.0"
    const val shadow = "8.1.1"
    const val slf4j = "2.0.9"
}

fun dep(group: String, name: String, version: String): String = "$group:$name:$version"

/**
 * USAGE: in your `build.gradle.kts`:
 *
 * ```
 * dependencies {
 *     implementation(Libs.<NAME>)
 * }
 * ```
 */
object Libs {
    // https://github.com/junit-team/junit5
    val junit_bom = dep(
        group = "org.junit",
        name = "junit-bom",
        version = Versions.junit
    )
    const val junit_jupiter = "org.junit.jupiter:junit-jupiter"

    // https://github.com/MicroUtils/kotlin-logging
    val kotlin_logging = dep(
        group = "io.github.oshai",
        name = "kotlin-logging",
        version = Versions.kotlin_logging
    )

    // https://github.com/qos-ch/slf4j
    val slf4j_simple = dep(
        group = "org.slf4j",
        name = "slf4j-simple",
        version = Versions.slf4j
    )

    // https://github.com/Kotlin/kotlinx.coroutines
    val kotlinx_coroutines_core = dep(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-coroutines-core",
        version = Versions.kotlinx_coroutines
    )

    // https://github.com/Kotlin/kotlinx.serialization
    val kotlinx_serialization_json = dep(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-serialization-json",
        version = Versions.kotlinx_serialization
    )

    // https://github.com/ajalt/clikt
    val clikt = dep(
        group = "com.github.ajalt.clikt",
        name = "clikt",
        version = Versions.clikt
    )

    // https://github.com/UnitTestBot/jacodb
    private const val jacodb = "com.github.Saloed.jacodb"
    val jacodb_api = dep(
        group = jacodb,
        name = "jacodb-api",
        version = Versions.jacodb
    )
    val jacodb_core = dep(
        group = jacodb,
        name = "jacodb-core",
        version = Versions.jacodb
    )
    val jacodb_analysis = dep(
        group = jacodb,
        name = "jacodb-analysis",
        version = Versions.jacodb
    )
    val jacodb_approximations = dep(
        group = jacodb,
        name = "jacodb-approximations",
        version = Versions.jacodb
    )

    // https://github.com/detekt/sarif4k
    val sarif4k = dep(
        group = "io.github.detekt.sarif4k",
        name = "sarif4k",
        version = Versions.sarif4k
    )

    // https://github.com/JetBrains/java-annotations
    val annotations = dep(
        group = "org.jetbrains",
        name = "annotations",
        version = Versions.annotations
    )

    // https://github.com/UnitTestBot/ksmt
    val ksmt_core = dep(
        group = "io.ksmt",
        name = "ksmt-core",
        version = "0.5.7"
    )
    val ksmt_z3 = dep(
        group = "io.ksmt",
        name = "ksmt-z3",
        version = "0.5.7"
    )
}

/**
 * USAGE: in your `build.gradle.kts`:
 *
 * ```
 * plugins {
 *     with (Plugins.<NAME>) { id(id) version (version) }
 * }
 * ```
 */
object Plugins {
    // https://github.com/Kotlin/dokka
    object Dokka {
        const val version = Versions.dokka
        const val id = "org.jetbrains.dokka"
    }

    // https://plugins.gradle.org/docs/publish-plugin
    object PluginPublish {
        const val version = Versions.gradle_plugin_publish
        const val id = "com.gradle.plugin-publish"
    }

    // https://github.com/johnrengelman/shadow
    object Shadow {
        const val version = Versions.shadow
        const val id = "com.github.johnrengelman.shadow"
    }
}
