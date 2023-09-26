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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jacodb.analysis.AnalysisConfig

/**
 * Example configuration:
 *
 * ```
 * byteflow {
 *     // config = analysisConfig("NPE" to mapOf("UnitResolver" to "singleton")) // (specific)
 *     //  OR
 *     // configFile = layout.projectDirectory.file("configs/config.json") // (default)
 *     classpath = System.getProperty("java.class.path")
 *     startClasses = listOf("org.byteflow.examples.NpeExamples")
 *     // dbLocation = "index.db" // (optional)
 *     // outputPath = "report.sarif" // (default)
 * }
 * ```
 */
interface ByteFlowExtension {
    val config: Property<AnalysisConfig>
    val configFile: RegularFileProperty
    val classpath: Property<String>
    val startClasses: ListProperty<String>
    val dbLocation: Property<String>
    val outputPath: Property<String>
}
