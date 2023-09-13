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
package org.byteflow

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import io.github.detekt.sarif4k.SarifSerializer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jacodb.analysis.graph.newApplicationGraphForAnalysis
import org.jacodb.analysis.sarif.sarifReportFromVulnerabilities
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClassProcessingTask
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.jacodb
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

@Suppress("MemberVisibilityCanBePrivate")
class Cli : CliktCommand("byteflow") {
    init {
        context {
            helpFormatter = {
                MordantHelpFormatter(
                    it,
                    requiredOptionMarker = "*",
                    showDefaultValues = true,
                    showRequiredTag = true
                )
            }
        }
    }

    val configFile: File by option("-c", "--config")
        .help("Path to analysis configuration in JSON format")
        .file(mustExist = true, canBeDir = false)
        .required()

    val dbLocation: String? by option("-db", "--db-location")
        .help("Location of SQLite database for storing bytecode data")

    val startClasses: String by option("-s", "--start")
        .help("Comma-separated list of classes from which to start the analysis")
        // .default("org.byteflow.examples.NpeExamples")
        .required()

    val classpath: String by option("-cp", "--classpath")
        .help("Classpath for analysis (used by JacoDB)")
        .default(System.getProperty("java.class.path"))

    val outputPath: String by option("-o", "--output")
        .help("Path to the resulting file with analysis report")
        .default("report.sarif")

    override fun run() {
        val timeStart = TimeSource.Monotonic.markNow()
        logger.info { "start at $timeStart" }

        val config = Json.decodeFromString<AnalysisConfig>(configFile.readText())

        val classpathAsFiles = classpath.split(File.pathSeparatorChar).sorted().map { File(it) }
        val cp = runBlocking {
            logger.info { "initializing jacodb..." }
            val jacodb = jacodb {
                dbLocation?.let {
                    logger.info { "Using db location: '$it'" }
                    persistent(it)
                }
                loadByteCode(classpathAsFiles)
                installFeatures(InMemoryHierarchy, Usages)
            }
            logger.info { "jacodb created, creating cp..." }
            jacodb.classpath(classpathAsFiles)
        }
        logger.info { "cp created" }

        val startClassesAsList = startClasses.split(",")
        echo("startClasses: (${startClassesAsList.size})")
        for (clazz in startClassesAsList) {
            echo("  - $clazz")
        }

        logger.info { "process classes" }
        val startJcClasses = ConcurrentHashMap.newKeySet<JcClassOrInterface>()
        cp.executeAsync(object : JcClassProcessingTask {
            override fun process(clazz: JcClassOrInterface) {
                if (startClassesAsList.any { clazz.name.startsWith(it) }) {
                    startJcClasses.add(clazz)
                }
            }
        }).get()
        echo("startJcClasses: (${startJcClasses.size})")
        for (clazz in startJcClasses) {
            echo("  - $clazz")
        }
        logger.info { "filter start methods" }
        val startJcMethods = startJcClasses
            .flatMap { it.declaredMethods }
            .filter { !it.isPrivate }
            // .filterNot { it.enclosingClass.name == "java.lang.Object" }
            .distinct()
        echo("startJcMethods: (${startJcMethods.size})")
        for (method in startJcMethods) {
            echo("  - $method")
        }

        logger.info { "create application graph" }
        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }

        logger.info { "Analyzing..." }
        val vulnerabilities = config.analyses
            .mapNotNull { (analysis, options) ->
                runAnalysis(analysis, options, graph, startJcMethods)
            }
            .flatten()
        logger.info { "Analysis done. Found ${vulnerabilities.size} vulnerabilities" }
        // for (vulnerability in vulnerabilities) {
        //     echo(vulnerability)
        // }

        val sarif = sarifReportFromVulnerabilities(vulnerabilities)
        logger.info { "Writing SARIF to '$outputPath'..." }
        File(outputPath).writeText(SarifSerializer.toJson(sarif))

        echo()
        echo("All done in ${timeStart.elapsedNow()}")
    }
}

fun main(args: Array<String>) {
    Cli().main(args)
}