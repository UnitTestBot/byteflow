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

/**
 * Example configuration:
 *
 * ```
 * byteflow {
 *     // configFile = File("configs/config.json")
 *     startClasses = listOf("org.byteflow.examples.NpeExamples")
 *     classpath = System.getProperty("java.class.path")
 *     // outputFile = File("report.sarif")
 * }
 * ```
 */
interface ByteFlowExtension {
    val configFile: RegularFileProperty
    val dbLocation: Property<String>
    val startClasses: ListProperty<String>
    val classpath: Property<String>
    val outputPath: Property<String>

    companion object {
        const val NAME: String = "byteflow"
    }
}
