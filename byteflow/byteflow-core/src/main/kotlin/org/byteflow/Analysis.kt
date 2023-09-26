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

import io.github.oshai.kotlinlogging.KotlinLogging
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
typealias AnalysisOptions = Map<String, String>

fun runAnalysis(
    analysis: AnalysisType,
    options: AnalysisOptions,
    graph: JcApplicationGraph,
    methods: List<JcMethod>,
    timeoutMillis: Long = Long.MAX_VALUE,
): List<VulnerabilityInstance> {
    logger.info { "Launching analysis: '$analysis'" }
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
            error("Unknown analysis type: '$analysis'")
        }
    }
    val unitResolverName = options.getOrDefault("UnitResolver", "method")
    logger.info { "Using unit resolver: '$unitResolverName'" }
    val unitResolver = UnitResolver.getByName(unitResolverName)
    val manager = MainIfdsUnitManager(graph, unitResolver, runner, methods, timeoutMillis)
    return manager.analyze()
}
