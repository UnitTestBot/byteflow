@file:Suppress("MemberVisibilityCanBePrivate")

package org.usvm.analyzer.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument

class Hello : CliktCommand() {
    val name: String by argument()

    override fun run() {
        echo("Hello $name!")
    }
}

fun main(args: Array<String>) {
    Hello().main(args)
}
