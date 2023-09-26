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

import io.github.detekt.sarif4k.SarifSchema210
import io.github.detekt.sarif4k.ThreadFlowLocation
import org.jacodb.api.cfg.JcInst
import java.io.File

val defaultSourceFileResolver: (JcInst) -> String = { inst ->
    val registeredLocation = inst.location.method.declaration.location
    val classFileBaseName = inst.location.method.enclosingClass.name.replace('.', '/')
    if (registeredLocation.path.contains("build/classes/")) {
        val src = registeredLocation.path.replace("build/classes/(\\w+)/(\\w+)".toRegex()) {
            val (language, sourceSet) = it.destructured
            "src/${sourceSet}/${language}"
        }
        "file://" + File(src).resolve(classFileBaseName.substringBefore('$')).path + ".java"
    } else {
        File(registeredLocation.path).resolve(classFileBaseName).path + ".class"
    }
}

fun SarifSchema210.deduplicateThreadFlowLocations(): SarifSchema210 {
    return copy(
        runs = runs.map { run ->
            run.copy(
                results = run.results?.map { result ->
                    result.copy(
                        codeFlows = result.codeFlows?.map { codeFlow ->
                            codeFlow.copy(
                                threadFlows = codeFlow.threadFlows.map { threadFlow ->
                                    threadFlow.copy(
                                        locations = threadFlow.locations.deduplicate()
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    )
}

private fun List<ThreadFlowLocation>.deduplicate(): List<ThreadFlowLocation> {
    if (isEmpty()) return emptyList()

    return listOf(first()) + zipWithNext { a, b ->
        val aLine = a.location!!.physicalLocation!!.region!!.startLine!!
        val bLine = b.location!!.physicalLocation!!.region!!.startLine!!
        if (aLine != bLine) {
            b
        } else {
            null
        }
    }.filterNotNull()
}
