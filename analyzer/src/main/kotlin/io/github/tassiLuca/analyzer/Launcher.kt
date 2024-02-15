package io.github.tassiLuca.analyzer

import io.github.tassiLuca.analyzer.core.Analyzer
import kotlinx.coroutines.runBlocking

/** The main entry point of the application. */
fun main(): Unit = runBlocking {
    val analyzer = Analyzer.github()
    val result = analyzer.analyze("unibo-spe") { report, completed ->
        println("Report: $report")
        if (completed) println("Analysis completed.")
    }
    println("Result: $result")
}
