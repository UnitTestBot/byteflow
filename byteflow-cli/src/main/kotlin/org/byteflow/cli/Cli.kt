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

package org.byteflow.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.byteflow.AnalysisOptions
import org.byteflow.AnalysisType
import org.byteflow.getClassesNameEq
import org.byteflow.runAnalysis
import org.jacodb.analysis.graph.newApplicationGraphForAnalysis
import org.jacodb.analysis.sarif.sarifReportFromVulnerabilities
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.jacodb
import java.io.File
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

@Serializable
data class AnalysisConfig(val analyses: Map<AnalysisType, AnalysisOptions>)

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

    @OptIn(ExperimentalSerializationApi::class)
    override fun run() {
        val timeStart = TimeSource.Monotonic.markNow()
        logger.info { "start at $timeStart" }

        val config = Json.decodeFromString<AnalysisConfig>(configFile.readText())
        logger.info { "config = $config" }

        logger.info { "classpath = $classpath" }
        val classpathAsFiles = classpath.split(File.pathSeparatorChar).sorted().map { File(it) }

        val cp = runBlocking {
            logger.info { "initializing db..." }
            val db = jacodb {
                dbLocation?.let {
                    logger.info { "Using db location: '$it'" }
                    persistent(it)
                }
                loadByteCode(classpathAsFiles)
                installFeatures(InMemoryHierarchy, Usages)
            }
            logger.info { "db created, creating cp..." }
            db.classpath(classpathAsFiles)
        }
        logger.info { "cp created" }

        val startClassesAsList = startClasses.split(",")
        logger.info { "startClasses: (${startClassesAsList.size})" }
        for (clazz in startClassesAsList) {
            logger.info { "  - $clazz" }
        }

        logger.info { "Searching classes..." }
        val startJcClasses = getClassesNameEq(cp, startClassesAsList)
        logger.info { "startClasses: (${startJcClasses.size})" }
        for (clazz in startJcClasses) {
            logger.info { "  - $clazz" }
        }

        logger.info { "Filtering methods..." }
        val startJcMethods = startJcClasses
            .flatMap { it.declaredMethods }
            .filter { !it.isPrivate }
            .distinct()

        logger.info { "startMethods: (${startJcMethods.size})" }
        for (method in startJcMethods) {
            logger.info { "  - $method" }
        }

        logger.info { "Creating application graph..." }
        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }

        logger.info { "Analyzing..." }
        val vulnerabilities = config.analyses
            .flatMap { (analysis, options) ->
                runAnalysis(analysis, options, graph, startJcMethods)
            }
        logger.info { "Analysis done. Found ${vulnerabilities.size} vulnerabilities" }
        for (vulnerability in vulnerabilities) {
            logger.info { vulnerability }
        }

        if (outputPath != "") {
            val sarif = sarifReportFromVulnerabilities(vulnerabilities)
            val json = Json {
                prettyPrint = true
                prettyPrintIndent = "  "
            }
            logger.info { "Writing SARIF to '$outputPath'..." }
            File(outputPath).outputStream().use { output ->
                json.encodeToStream(sarif, output)
            }
        } else {
            logger.info { "Not writing SARIF report. Use non-empty '-o/--output' argument" }
        }

        logger.info { "All done in ${timeStart.elapsedNow()}" }
    }
}

fun main(args: Array<String>) {
    Cli().main(args)
}
