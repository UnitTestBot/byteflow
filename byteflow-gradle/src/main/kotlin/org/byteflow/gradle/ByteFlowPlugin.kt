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

import org.byteflow.analysisConfigFromFile
import org.byteflow.getPublicMethodsForClasses
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class ByteFlowPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.lifecycle("Applying ByteFlow plugin")

        // Example 'hello' task
        project.tasks.register("hello") {
            logger.lifecycle("Registering '$name' task")
            doLast {
                println("Hello!")
            }
        }

        // 'byteflow {...}' extension
        val extension = project.extensions.create<ByteFlowExtension>(EXTENSION_NAME)
        extension.apply {
            // Extension defaults:
            configFile.convention(project.layout.projectDirectory.file("configs/config.json"))
            outputPath.convention("report.sarif")
        }

        // 'runAnalyzer' task
        project.tasks.register<RunAnalyzerTask>(TASK_NAME) {
            logger.lifecycle("Registering '$name' task")

            // Note: task defaults are configured in the task's `init {...}` block.

            // Mapping of options from the extension:
            config.convention(project.provider {
                if (extension.config.isPresent) {
                    // TODO: throw GradleException if 'configFile' is present.
                    extension.config.get()
                } else {
                    analysisConfigFromFile(extension.configFile.get().asFile.path)
                }
            })
            dbLocation.convention(extension.dbLocation)
            methodsForCp.convention { cp ->
                // Default behavior is to extract all public methods from the given classes:
                getPublicMethodsForClasses(cp, extension.startClasses.get())
            }
            classpath.convention(extension.classpath)
            outputPath.convention(extension.outputPath)
        }
    }

    companion object {
        const val TASK_NAME: String = "runAnalyzer"
        const val EXTENSION_NAME: String = "byteflow"
    }
}
