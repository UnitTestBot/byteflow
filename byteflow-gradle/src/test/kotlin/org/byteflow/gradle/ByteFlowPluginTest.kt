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

import kotlin.test.Test
import kotlin.test.assertNotNull

class ByteFlowPluginTest {
    @Test
    fun `project should contain the byteflow plugin`() {
        val project = buildProject()
        val plugin = project.byteflowPlugin
        assertNotNull(plugin)
    }

    @Test
    fun `plugin should register runAnalyzer task`() {
        val project = buildProject()
        val plugin = project.byteflowPlugin
        val task = project.tasks.getByName(plugin.runAnalyzerTaskName)
        assertNotNull(task)
    }

    @Test
    fun `plugin should register byteflow extension`() {
        val project = buildProject()
        val plugin = project.byteflowPlugin
        val extension = project.extensions.getByName(plugin.byteflowExtensionName)
        assertNotNull(extension)
    }
}
