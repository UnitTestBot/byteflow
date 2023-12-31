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

import kotlinx.coroutines.runBlocking
import org.byteflow.examples.NpeExamples
import org.jacodb.analysis.graph.newApplicationGraphForAnalysis
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.jacodb
import java.io.File
import kotlin.test.Test

class NpeExamplesTest {
    @Test
    fun `test NPE analysis of NpeExamples`() {
        val classpath = System.getProperty("java.class.path")
        val classpathAsFiles = classpath.split(File.pathSeparatorChar).sorted().map { File(it) }
        println("classpath = $classpathAsFiles")

        val cp = runBlocking {
            val db = jacodb {
                loadByteCode(classpathAsFiles)
                installFeatures(InMemoryHierarchy, Usages)
            }
            db.classpath(classpathAsFiles)
        }

        val methods = getPublicMethodsForClasses(cp, listOf(NpeExamples::class.java.name))

        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }

        runAnalysis("NPE", mapOf("UnitResolver" to "singleton"), graph, methods)
    }
}
