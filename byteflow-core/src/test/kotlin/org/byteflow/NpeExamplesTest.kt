package org.byteflow

import kotlinx.coroutines.runBlocking
import org.byteflow.examples.NpeExamples
import org.jacodb.analysis.graph.newApplicationGraphForAnalysis
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClassProcessingTask
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.jacodb
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertTrue

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

        val startClasses = listOf(NpeExamples::class.java.name)
        val startJcClasses = ConcurrentHashMap.newKeySet<JcClassOrInterface>()
        runBlocking {
            cp.execute(object : JcClassProcessingTask {
                override fun process(clazz: JcClassOrInterface) {
                    if (startClasses.contains(clazz.name)) {
                        startJcClasses.add(clazz)
                    }
                }
            })
        }
        println("startJcClasses: (${startJcClasses.size})")
        for (clazz in startJcClasses) {
            println("  - $clazz")
        }

        val startJcMethods = startJcClasses
            .flatMap { it.declaredMethods }
            .filter { !it.isPrivate }
            .distinct()
        println("startJcMethods: (${startJcMethods.size})")
        for (method in startJcMethods) {
            println("  - $method")
        }

        val graph = runBlocking {
            cp.newApplicationGraphForAnalysis()
        }

        val options = mapOf(
            "UnitResolver" to "singleton"
        )
        val vulnerabilities = runAnalysis("NPE", options, graph, startJcMethods)
        assertTrue(vulnerabilities.isNotEmpty())
    }
}
