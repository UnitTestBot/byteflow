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
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.jacodb.analysis.AnalysisConfig
import org.jacodb.analysis.graph.newApplicationGraphForAnalysis
import org.jacodb.analysis.sarif.sarifReportFromVulnerabilities
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcMethod
import org.jacodb.api.cfg.JcInst
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.jacodb
import java.io.File
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

    @get:Input
    abstract val sourceResolver: Property<(JcInst) -> String>

    @get:Input
    abstract val deduplicateThreadFlowLocations: Property<Boolean>

    init {
        description = "ByteFlow analyzer runner"
        group = JavaBasePlugin.VERIFICATION_GROUP

        // Task defaults:
        sourceResolver.convention { defaultSourceFileResolver(it) }
        // sourceResolver.convention(defaultSourceFileResolver) // doesn't work!
        deduplicateThreadFlowLocations.convention(true)
    }

    @TaskAction
    fun analyze() {
        val timeStart = TimeSource.Monotonic.markNow()
        logger.lifecycle("Start analysis at $timeStart")

        val config = config.get()
        logger.lifecycle("config = $config")

        val classpathAsFiles = classpath.get().split(File.pathSeparatorChar).sorted().map { File(it) }
        logger.lifecycle("classpath = $classpathAsFiles")

        logger.lifecycle("Creating db...")
        val timeStartCp = TimeSource.Monotonic.markNow()
        val cp = runBlocking {
            val db = jacodb {
                dbLocation.orNull?.let {
                    val f = project.file(it)
                    logger.lifecycle("Using db location: '$f'")
                    persistent(f.path)
                }
                loadByteCode(classpathAsFiles)
                installFeatures(InMemoryHierarchy, Usages)
            }
            logger.lifecycle("db created")
            logger.lifecycle("Creating cp...")
            db.classpath(classpathAsFiles)
        }
        logger.lifecycle("cp created in ${timeStartCp.elapsedNow()}")

        logger.lifecycle("Forming the list of methods to analyze...")
        val timeStartMethods = TimeSource.Monotonic.markNow()
        val methods = run {
            val m = methods.get().toMutableList()
            if (methodsForCp.isPresent) {
                m += methodsForCp.get()(cp)
            }
            m.distinct()
        }
        logger.lifecycle("Found ${methods.size} methods to analyze in ${timeStartMethods.elapsedNow()}")

        logger.lifecycle("Creating application graph...")
        val timeStartGraph = TimeSource.Monotonic.markNow()
        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }
        logger.lifecycle("Application graph created in ${timeStartGraph.elapsedNow()}")

        logger.lifecycle("Analyzing...")
        val timeStartAnalysis = TimeSource.Monotonic.markNow()
        val vulnerabilities = config.analyses
            .flatMap { (analysis, options) ->
                logger.info("running '$analysis' analysis...")
                runAnalysis(analysis, options, graph, methods)
            }
        logger.lifecycle("Analysis done in ${timeStartAnalysis.elapsedNow()}")
        logger.lifecycle("Found ${vulnerabilities.size} vulnerabilities")
        for (v in vulnerabilities) {
            logger.info("  - v")
        }

        logger.lifecycle("Preparing report...")
        val sarif = sarifReportFromVulnerabilities(vulnerabilities, sourceFileResolver = sourceResolver.get())
            .let {
                if (deduplicateThreadFlowLocations.get()) {
                    logger.lifecycle("Deduplicating thread flow locations...")
                    it.deduplicateThreadFlowLocations()
                } else it
            }
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        val output = project.file(outputPath)
        logger.lifecycle("Writing SARIF to '$output'...")
        output.outputStream().use { stream ->
            json.encodeToStream(sarif, stream)
        }

        logger.lifecycle("All done in %.1f s".format(timeStart.elapsedNow().toDouble(DurationUnit.SECONDS)))
    }
}
