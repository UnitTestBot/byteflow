/*
 * Copyright 2023 UnitTestBot
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.byteflow.gradle

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.byteflow.runAnalysis
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.jacodb.analysis.AnalysisConfig
import org.jacodb.analysis.graph.newApplicationGraphForAnalysis
import org.jacodb.analysis.sarif.sarifReportFromVulnerabilities
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClassProcessingTask
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcMethod
import org.jacodb.api.cfg.JcInst
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.jacodb
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@Suppress("unused")
@OptIn(ExperimentalSerializationApi::class, ExperimentalTime::class)
abstract class RunAnalyzerTask : DefaultTask() {
    @get:Input
    abstract val config: Property<AnalysisConfig>

    @get:Optional
    @get:Input
    abstract val dbLocation: Property<String>

    @get:Input
    abstract val classpath: Property<String>

    @get:Input
    abstract val methods: ListProperty<JcMethod>

    @get:Optional
    @get:Input
    abstract val methodsForCp: Property<(JcClasspath) -> List<JcMethod>>

    @get:Input
    abstract val outputPath: Property<String>

    @get:Optional
    @get:Input
    abstract val resolver: Property<(JcInst) -> String>

    @TaskAction
    fun analyze() {
        val timeStart = TimeSource.Monotonic.markNow()
        logger.quiet("Start analysis at $timeStart")

        val config = config.get()
        logger.quiet("config = $config")

        val classpathAsFiles = classpath.get().split(File.pathSeparatorChar).sorted().map { File(it) }
        logger.quiet("classpath = $classpathAsFiles")

        logger.quiet("Creating db...")
        val timeStartCp = TimeSource.Monotonic.markNow()
        val cp = runBlocking {
            val db = jacodb {
                dbLocation.orNull?.let {
                    logger.quiet("Using db location: '$it'")
                    persistent(it)
                }
                loadByteCode(classpathAsFiles)
                installFeatures(InMemoryHierarchy, Usages)
            }
            logger.quiet("db created")
            logger.quiet("Creating cp...")
            db.classpath(classpathAsFiles)
        }
        logger.quiet("cp created in ${timeStartCp.elapsedNow()}")

        @Suppress("LocalVariableName")
        val _methods = methods.get().toMutableList()
        methodsForCp.orNull?.let {
            _methods += it(cp)
        }
        val methods = _methods.distinct()

        logger.quiet("Creating application graph...")
        val timeStartGraph = TimeSource.Monotonic.markNow()
        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }
        logger.quiet("Application graph created in ${timeStartGraph.elapsedNow()}")

        logger.quiet("Analyzing...")
        val timeStartAnalysis = TimeSource.Monotonic.markNow()
        val vulnerabilities = config.analyses
            .flatMap { (analysis, options) ->
                logger.quiet("running '$analysis' analysis...")
                runAnalysis(analysis, options, graph, methods)
            }
        logger.quiet("Analysis done in ${timeStartAnalysis.elapsedNow()}")
        logger.quiet("Found ${vulnerabilities.size} vulnerabilities")
        // for (v in vulnerabilities) {
        //     logger.quiet("  - $v")
        // }

        val resolver = resolver.orNull
            ?: { inst ->
                val registeredLocation = inst.location.method.declaration.location
                val classFileBaseName = inst.location.method.enclosingClass.name.replace('.', '/')
                if (registeredLocation.path.contains("build/classes/java/main")) {
                    val src = registeredLocation.path.replace("build/classes/java/main", "src/main/java")
                    "file://" + File(src).resolve(classFileBaseName.substringBefore('$')).path + ".java"
                } else {
                    File(registeredLocation.path).resolve(classFileBaseName).path + ".class"
                }
            }

        logger.quiet("Preparing report...")
        val sarif = sarifReportFromVulnerabilities(vulnerabilities) { resolver(it) }
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        val output = File(outputPath.get())
        logger.quiet("Writing SARIF to '$output'...")
        output.outputStream().use { stream ->
            json.encodeToStream(sarif, stream)
        }

        logger.quiet("All done in %.3f s".format(timeStart.elapsedNow().toDouble(DurationUnit.SECONDS)))
    }

    companion object {
        const val NAME: String = "runAnalyzer"
    }
}

fun Project.getMethodsForClasses(
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
