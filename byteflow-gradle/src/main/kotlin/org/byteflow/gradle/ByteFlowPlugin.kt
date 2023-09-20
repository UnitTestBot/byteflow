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
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class ByteFlowPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.quiet("Applying ByteFlow plugin")

        // Example 'hello' task
        project.tasks.register("hello") {
            logger.quiet("Registering '$name' task")
            doLast {
                println("Hello!")
            }
        }

        // 'byteflow {}' extension
        val extension = project.extensions.create<ByteFlowExtension>(ByteFlowExtension.NAME)
        extension.apply {
            // Defaults:
            configFile.convention(project.layout.projectDirectory.file("configs/config.json"))
            outputPath.convention("report.sarif")
        }

        // 'runAnalyzer' task
        project.tasks.register<RunAnalyzerTask>(RunAnalyzerTask.NAME) {
            logger.quiet("Registering '$name' task")
            // Mapping:
            configFile.convention(extension.configFile)
            dbLocation.convention(extension.dbLocation)
            startClasses.convention(extension.startClasses)
            classpath.convention(extension.classpath)
            outputPath.convention(extension.outputPath)
            useUsvmAnalysis.convention(extension.useUsvmAnalysis)
        }
    }
}
