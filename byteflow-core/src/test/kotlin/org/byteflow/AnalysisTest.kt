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
import org.byteflow.examples.SqlInjectionSample
import org.byteflow.examples.SqlInjectionSample2
import org.jacodb.analysis.graph.newApplicationGraphForAnalysis
import org.jacodb.api.ext.findClass
import org.jacodb.approximation.Approximations
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.jacodb
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalysisTest {
    @Test
    fun `test basic analysis of NpeExamples`() {
        val vulnerabilities = runAnalysis<NpeExamples>("NPE", useUsvm = false)
        println(vulnerabilities.size)
    }

    @Test
    fun `test sql injection FP`() {
        val vulnerabilities = runAnalysis<SqlInjectionSample>("SQL", useUsvm = true)
        assertEquals(0, vulnerabilities.size)
    }

    @Test
    fun `test sql injection TP`() {
        val vulnerabilities = runAnalysis<SqlInjectionSample2>("SQL", useUsvm = true)
        assertEquals(1, vulnerabilities.size)
    }

    private inline fun <reified T> runAnalysis(analysis: AnalysisType, useUsvm: Boolean) = runBlocking {
        val classpath = System.getProperty("java.class.path")
        val classpathAsFiles = classpath.split(File.pathSeparatorChar).map { File(it) }

        val db = jacodb {
            useProcessJavaRuntime()
            installFeatures(InMemoryHierarchy, Usages, Approximations)
        }

        val approximationsCp = resolveApproximationsClassPath(File("."))
        val cp = db.classpath(classpathAsFiles + approximationsCp, listOf(Approximations))

        val clazz = cp.findClass<T>()

        val startJcMethods = listOf(clazz)
            .flatMap { it.declaredMethods }
            .filter { !it.isPrivate }
            .distinct()

        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }

        runAnalysis(analysis, mapOf("UnitResolver" to "singleton"), graph, startJcMethods, useUsvmAnalysis = useUsvm)
    }
}
