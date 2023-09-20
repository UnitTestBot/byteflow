package org.byteflow

import org.jacodb.analysis.engine.VulnerabilityInstance
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcMethod
import org.jacodb.api.TypeName
import org.jacodb.api.analysis.JcApplicationGraph
import org.jacodb.api.ext.cfg.callExpr
import org.jacodb.api.ext.findClass
import org.jacodb.api.ext.findDeclaredMethodOrNull
import org.usvm.PathSelectionStrategy
import org.usvm.SolverType
import org.usvm.UMachineOptions
import org.usvm.api.targets.Argument
import org.usvm.api.targets.AssignMark
import org.usvm.api.targets.CallParameterContainsMark
import org.usvm.api.targets.ConstantTrue
import org.usvm.api.targets.JcTarget
import org.usvm.api.targets.Result
import org.usvm.api.targets.SqlInjection
import org.usvm.api.targets.TaintAnalysis
import org.usvm.api.targets.TaintAnalysis.TaintIntermediateTarget
import org.usvm.api.targets.TaintAnalysis.TaintMethodSinkTarget
import org.usvm.api.targets.TaintAnalysis.TaintTarget
import org.usvm.api.targets.TaintConfiguration
import org.usvm.api.targets.TaintMethodSink
import org.usvm.api.targets.TaintMethodSource
import org.usvm.api.targets.TaintPassThrough
import org.usvm.api.targets.ThisArgument
import org.usvm.machine.JcMachine
import org.usvm.targets.UTargetController

fun analyzeVulnerabilitiesWithUsvm(
    analysis: AnalysisType,
    options: AnalysesOptions,
    graph: JcApplicationGraph,
    methods: List<JcMethod>,
    timeoutMillis: Long,
    vulnerabilities: List<VulnerabilityInstance>
): List<VulnerabilityInstance> = when (analysis) {
    "SQL" -> {
        analyzeSqlVulnerabilitiesWithUsvm(graph, methods, timeoutMillis, vulnerabilities)
    }

    else -> {
        vulnerabilities
    }
}

private fun analyzeSqlVulnerabilitiesWithUsvm(
    graph: JcApplicationGraph,
    methods: List<JcMethod>,
    timeoutMillis: Long,
    vulnerabilities: List<VulnerabilityInstance>
): List<VulnerabilityInstance> {
    System.err.println("RUN USVM ANALYSIS")
    System.err.println("Vulnerabilities before: ${vulnerabilities.size}")

    val config = mkSqlInjectionConfig(graph.classpath)

    val vulnTargets = mutableMapOf<TaintTarget, VulnerabilityInstance>()
    val initialTargets = mutableListOf<TaintTarget>()

    for (vuln in vulnerabilities) {
        val traces = vuln.traceGraph.getAllTraces()
        for (trace in traces) {
            val locations = trace.map { it.statement }

            val sinkLocation = locations.last()
            val call = sinkLocation.callExpr ?: continue
            val rules = config.methodSinks[call.method.method] ?: continue
            for (rule in rules) {
                val sink = TaintMethodSinkTarget(sinkLocation, ConstantTrue, rule)
                vulnTargets[sink] = vuln

                val target = locations.dropLast(1).foldRight(sink as TaintTarget) { loc, child ->
                    TaintIntermediateTarget(loc).apply {
                        addChild(child)
                    }
                }
                initialTargets += target
            }
        }
    }

    val options = UMachineOptions(
        pathSelectionStrategies = listOf(PathSelectionStrategy.TARGETED),
        timeoutMs = timeoutMillis,
        stopOnTargetsReached = true,
        solverType = SolverType.Z3
    )
    val analysis = TaintAnalysis(config, initialTargets)

    JcMachine(graph.classpath, options, analysis).use { machine ->
        methods.forEach { machine.analyze(it, initialTargets as List<JcTarget<UTargetController>>) }
    }

    val reachedTargets = analysis.collectedStates.flatMap { it.reachedTerminalTargets }
    val reachedVulnerabilities = reachedTargets.mapNotNull { vulnTargets[it as TaintTarget] }.toSet()

    val resultVulnerabilities = vulnerabilities.filter { it in reachedVulnerabilities }
    return resultVulnerabilities
}

/*
private val sqlSourceMatchers = listOf(
    "java\\.io.+",
    "java\\.lang\\.System\\#getenv",
    "java\\.sql\\.ResultSet#get.+"
)

private val sqlSanitizeMatchers = listOf(
    "java\\.sql\\.Statement#set.*",
    "java\\.sql\\.PreparedStatement#set.*"
)

private val sqlSinkMatchers = listOf(
    "java\\.sql\\.Statement#execute.*",
    "java\\.sql\\.PreparedStatement#execute.*",
)
*/

private fun mkSqlInjectionConfig(cp: JcClasspath): TaintConfiguration {
    val sources = mutableMapOf<JcMethod, List<TaintMethodSource>>()

    cp.findClass("java.lang.System").declaredMethods
        .filter { it.name == "getenv" && it.returnType.isString }
        .forEach { sources[it] = listOf(TaintMethodSource(it, ConstantTrue, AssignMark(Result, SqlInjection))) }

    cp.findClass("java.sql.ResultSet").declaredMethods
        .filter { it.name.startsWith("get") && it.returnType.isString }
        .forEach { sources[it] = listOf(TaintMethodSource(it, ConstantTrue, AssignMark(Result, SqlInjection))) }


    val passThrough = mutableMapOf<JcMethod, List<TaintPassThrough>>()
    val sb = cp.findClass("java.lang.StringBuilder")
    sb.declaredMethods
        .filter { m -> m.name == "append" && m.parameters.singleOrNull()?.type?.isString ?: false }
        .forEach { method ->
            passThrough[method] = listOf(
                TaintPassThrough(
                    method,
                    CallParameterContainsMark(Argument(0u), SqlInjection),
                    AssignMark(ThisArgument, SqlInjection)
                )
            )
        }
    sb.declaredMethods
        .filter { it.name == "toString" && it.returnType.isString }
        .forEach { method ->
            passThrough[method] = listOf(
                TaintPassThrough(
                    method,
                    CallParameterContainsMark(ThisArgument, SqlInjection),
                    AssignMark(Result, SqlInjection)
                )
            )
        }

    val sinks = mutableMapOf<JcMethod, List<TaintMethodSink>>()

    val sqlStatements = listOf(
        cp.findClass("java.sql.Statement"),
        cp.findClass("java.sql.PreparedStatement")
    )
    sqlStatements.flatMap { it.declaredMethods }
        .filter { method -> method.name.startsWith("execute") && method.parameters.any { it.type.isString } }
        .forEach { method ->
            val methodPositions = method.parameters.mapIndexedNotNull { idx, param ->
                Argument(idx.toUInt()).takeIf { param.type.isString }
            }
            sinks[method] = methodPositions.map {
                TaintMethodSink(
                    CallParameterContainsMark(it, SqlInjection),
                    method
                )
            }
        }

    return TaintConfiguration(
        entryPoints = emptyMap(),
        methodSources = sources,
        fieldSources = emptyMap(),
        passThrough = passThrough,
        cleaners = emptyMap(),
        methodSinks = sinks,
        fieldSinks = emptyMap()
    )
}

private val TypeName.isString: Boolean
    get() = typeName == "java.lang.String"
