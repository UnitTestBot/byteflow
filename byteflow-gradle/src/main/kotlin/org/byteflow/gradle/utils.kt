package org.byteflow.gradle

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.byteflow.AnalysesOptions
import org.byteflow.AnalysisType
import org.jacodb.analysis.AnalysisConfig
import java.io.File

fun analysisConfig(vararg pairs: Pair<AnalysisType, AnalysesOptions>): AnalysisConfig {
    return AnalysisConfig(pairs.toMap())
}

@OptIn(ExperimentalSerializationApi::class)
fun analysisConfigFromFile(path: String): AnalysisConfig {
    return File(path).inputStream().use { input ->
        Json.decodeFromStream<AnalysisConfig>(input)
    }
}
