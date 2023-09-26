package org.byteflow

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.jacodb.analysis.AnalysisConfig
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClassProcessingTask
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcMethod
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

fun analysisConfig(vararg pairs: Pair<AnalysisType, AnalysisOptions>): AnalysisConfig {
    return AnalysisConfig(pairs.toMap())
}

@OptIn(ExperimentalSerializationApi::class)
fun analysisConfigFromFile(path: String): AnalysisConfig {
    return File(path).inputStream().use { input ->
        Json.decodeFromStream<AnalysisConfig>(input)
    }
}

fun getClasses(
    cp: JcClasspath,
    predicate: (JcClassOrInterface) -> Boolean,
): List<JcClassOrInterface> {
    val classes = ConcurrentHashMap.newKeySet<JcClassOrInterface>()
    runBlocking {
        cp.execute(object : JcClassProcessingTask {
            override fun process(clazz: JcClassOrInterface) {
                if (predicate(clazz)) {
                    classes.add(clazz)
                }
            }
        })
    }
    return classes.toList()
}

fun getClassesNameEq(
    cp: JcClasspath,
    names: List<String>,
): List<JcClassOrInterface> {
    return getClasses(cp) { clazz ->
        names.any { clazz.name == it }
    }
}

fun getClassesNameStarts(
    cp: JcClasspath,
    names: List<String>,
): List<JcClassOrInterface> {
    return getClasses(cp) { clazz ->
        names.any { clazz.name.startsWith(it) }
    }
}

fun getPublicMethodsForClasses(
    cp: JcClasspath,
    names: List<String>,
): List<JcMethod> {
    logger.info { "Searching classes..." }
    val classes = getClassesNameEq(cp, names)

    logger.info { "classes: (${classes.size})" }
    for (clazz in classes) {
        logger.info { "  - $clazz" }
    }

    logger.info { "Filtering methods..." }
    val methods = classes
        .flatMap { it.declaredMethods }
        .filter { !it.isPrivate }
        .distinct()

    logger.info { "methods: (${methods.size})" }
    for (method in methods) {
        logger.info { "  - $method" }
    }

    return methods
}
