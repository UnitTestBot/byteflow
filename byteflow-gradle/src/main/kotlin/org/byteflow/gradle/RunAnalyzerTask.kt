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
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.byteflow.runAnalysis
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
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
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@OptIn(ExperimentalSerializationApi::class, ExperimentalTime::class)
abstract class RunAnalyzerTask : DefaultTask() {
    @get:InputFile
    abstract val configFile: RegularFileProperty

    @get:Optional
    @get:Input
    abstract val dbLocation: Property<String>

    @get:Input
    abstract val startClasses: ListProperty<String>

    @get:Input
    abstract val classpath: Property<String>

    @get:Input
    abstract val outputPath: Property<String>

    @get:Optional
    @get:Input
    abstract val useUsvmAnalysis: Property<Boolean>

    @TaskAction
    fun analyze() {
        val timeStart = TimeSource.Monotonic.markNow()
        logger.quiet("start at $timeStart")

        val config = configFile.get().asFile.inputStream().use { input ->
            Json.decodeFromStream<AnalysisConfig>(input)
        }

        val classpathAsFiles = classpath.get().split(File.pathSeparatorChar).sorted().map { File(it) }
        logger.quiet("classpath = $classpathAsFiles")
        val cp = runBlocking {
            logger.quiet("initializing jacodb...")
            val jacodb = jacodb {
                dbLocation.orNull?.let {
                    logger.quiet("Using db location: '$it'")
                    persistent(it)
                }
                loadByteCode(classpathAsFiles)
                installFeatures(InMemoryHierarchy, Usages)
            }
            logger.quiet("jacodb created, creating cp...")
            jacodb.classpath(classpathAsFiles)
        }
        logger.quiet("cp created")

        val startClassesAsList = startClasses.get()
        logger.quiet("startClasses: (${startClassesAsList.size})")
        for (clazz in startClassesAsList) {
            logger.quiet("  - $clazz")
        }

        logger.quiet("process classes")
        val startJcClasses = ConcurrentHashMap.newKeySet<JcClassOrInterface>()
        runBlocking {
            cp.execute(object : JcClassProcessingTask {
                override fun process(clazz: JcClassOrInterface) {
                    if (startClassesAsList.any { clazz.name.startsWith(it) }) {
                        startJcClasses.add(clazz)
                    }
                }
            })
        }
        logger.quiet("startJcClasses: (${startJcClasses.size})")
        for (clazz in startJcClasses) {
            logger.quiet("  - $clazz")
        }
        logger.quiet("filter start methods")
        val startJcMethods = startJcClasses
            .flatMap { it.declaredMethods }
            .filter { !it.isPrivate }
            // .filterNot { it.enclosingClass.name == "java.lang.Object" }
            .distinct()
        logger.quiet("startJcMethods: (${startJcMethods.size})")
        for (method in startJcMethods) {
            logger.quiet("  - $method")
        }

        logger.quiet("create application graph")
        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }

        logger.quiet("Analyzing...")
        val useUsvm = useUsvmAnalysis.orNull ?: false
        val vulnerabilities = config.analyses
            .mapNotNull { (analysis, options) ->
                runAnalysis(analysis, options, graph, startJcMethods, useUsvmAnalysis = useUsvm)
            }
            .flatten()
        logger.quiet("Analysis done. Found ${vulnerabilities.size} vulnerabilities")
        // for (vulnerability in vulnerabilities) {
        //     echo(vulnerability)
        // }

        val sarif = sarifReportFromVulnerabilities(vulnerabilities) { inst ->
            val registeredLocation = inst.location.method.declaration.location
            val classFileBaseName = inst.location.method.enclosingClass.name.replace('.', '/')
            if (registeredLocation.path.contains("build/classes/java/main")) {
                val src = registeredLocation.path.replace("build/classes/java/main", "src/main/java")
                "file://" + File(src).resolve(classFileBaseName.substringBefore('$')).path + ".java"
            } else {
                File(registeredLocation.path).resolve(classFileBaseName).path + ".class"
            }
        }
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        val output = File(outputPath.get())
        logger.quiet("Writing SARIF to '$output'...")
        output.outputStream().use { stream ->
            json.encodeToStream(sarif, stream)
        }

        logger.quiet("All done in ${timeStart.elapsedNow()}")
    }

    companion object {
        const val NAME: String = "runAnalyzer"
    }
}

@Suppress("unused")
@OptIn(ExperimentalSerializationApi::class, ExperimentalTime::class)
abstract class RunAnalyzerExtendedTask : DefaultTask() {
    @get:Input
    abstract val config: Property<AnalysisConfig>

    @get:Optional
    @get:Input
    abstract val dbLocation: Property<String>

    @get:Input
    abstract val classpath: Property<String>

    @get:Input
    abstract val methods: Property<(JcClasspath) -> List<JcMethod>>

    @get:Input
    abstract val outputPath: Property<String>

    @get:Optional
    @get:Input
    abstract val resolver: Property<(JcInst) -> String>

    @get:Optional
    @get:Input
    abstract val useUsvmAnalysis: Property<Boolean>

    @TaskAction
    fun analyze() {
        val timeStart = TimeSource.Monotonic.markNow()
        logger.quiet("start at $timeStart")

        val config = config.get()

        val classpathAsFiles = classpath.get().split(File.pathSeparatorChar).sorted().map { File(it) }
        logger.quiet("classpath = $classpathAsFiles")
        val cp = runBlocking {
            logger.quiet("Creating db...")
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
        logger.quiet("cp created")

        val methods = methods.get()(cp)

        logger.quiet("Creating application graph...")
        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }
        logger.quiet("Application graph created")

        logger.quiet("Analyzing...")
        val useUsvm = useUsvmAnalysis.orNull ?: false
        val vulnerabilities = config.analyses
            .flatMap { (analysis, options) ->
                logger.quiet("running '$analysis' analysis...")
                runAnalysis(analysis, options, graph, methods, useUsvmAnalysis = useUsvm)
            }
        logger.quiet("Analysis done. Found ${vulnerabilities.size} vulnerabilities")
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

        logger.quiet("All done in ${timeStart.elapsedNow()}")
    }

    companion object {
        const val NAME: String = "runAnalyzerEx"
    }
}
