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

import org.gradle.api.Plugin
import org.gradle.api.Project

class ByteFlowPlugin : Plugin<Project> {
    internal val byteflowExtensionName = "byteflow"
    internal val runAnalyzerTaskName = "runAnalyzer"

    override fun apply(project: Project) {
        // Example 'hello' task
        project.tasks.register("hello") {
            println("Hello!")
        }

        // 'byteflow {}' extension
        val extension = project.extensions.create(
            byteflowExtensionName,
            ByteFlowExtension::class.java
        ).also {
            // Defaults:
            it.configFile.convention(project.layout.projectDirectory.file("configs/config.json"))
            it.outputPath.convention("report.sarif")
        }

        // 'runAnalyzer' task
        project.tasks.register(
            runAnalyzerTaskName,
            RunAnalyzerTask::class.java,
        ) {
            // Mapping:
            it.configFile.convention(extension.configFile)
            it.dbLocation.convention(extension.dbLocation)
            it.startClasses.convention(extension.startClasses)
            it.classpath.convention(extension.classpath)
            it.outputPath.convention(extension.outputPath)
        }
    }
}
