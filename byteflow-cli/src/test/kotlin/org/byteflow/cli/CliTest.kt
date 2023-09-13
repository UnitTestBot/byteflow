package org.byteflow.cli

import org.byteflow.examples.NpeExamples
import kotlin.test.Test

class CliTest {
    @Test
    fun `test basic analysis of NpeExamples via CLI`() {
        val config = object {}.javaClass.getResource("/config.json")?.file
            ?: error("Can't find config file")
        val args = listOf(
            "-c", config,
            "-s", NpeExamples::class.java.name,
            "-o", ""
        )
        Cli().main(args)
    }
}
