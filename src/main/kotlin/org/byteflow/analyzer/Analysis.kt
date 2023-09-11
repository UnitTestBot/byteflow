package org.byteflow.analyzer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.jacodb.analysis.engine.MainIfdsUnitManager
import org.jacodb.analysis.engine.UnitResolver
import org.jacodb.analysis.engine.VulnerabilityInstance
import org.jacodb.analysis.library.UnusedVariableRunnerFactory
import org.jacodb.analysis.library.newNpeRunnerFactory
import org.jacodb.analysis.library.newSqlInjectionRunnerFactory
import org.jacodb.api.JcMethod
import org.jacodb.api.analysis.JcApplicationGraph

private val logger = KotlinLogging.logger {}

typealias AnalysisType = String
typealias AnalysesOptions = Map<String, String>

@Serializable
data class AnalysisConfig(val analyses: Map<AnalysisType, AnalysesOptions>)

fun runAnalysis(
    analysis: AnalysisType,
    options: AnalysesOptions,
    graph: JcApplicationGraph,
    methods: List<JcMethod>,
    timeoutMillis: Long = Long.MAX_VALUE,
): List<VulnerabilityInstance>? {
    logger.info { "Launching analysis $analysis" }
    val runner = when (analysis) {
        "NPE" -> {
            newNpeRunnerFactory()
        }

        "Unused" -> {
            UnusedVariableRunnerFactory
        }

        "SQL" -> {
            newSqlInjectionRunnerFactory()
        }

        else -> {
            logger.error { "Unknown analysis type: '$analysis'" }
            return null
        }
    }
    val unitResolverName = options.getOrDefault("UnitResolver", "method")
    logger.info { "Using '$unitResolverName' unit resolver" }
    val unitResolver = UnitResolver.getByName(unitResolverName)
    val manager = MainIfdsUnitManager(graph, unitResolver, runner, methods, timeoutMillis)
    return manager.analyze()
}
