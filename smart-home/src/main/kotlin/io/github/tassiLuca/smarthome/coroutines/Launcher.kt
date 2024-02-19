package io.github.tassiLuca.smarthome.coroutines

import io.github.tassiLuca.smarthome.coroutines.infrastructure.MockedHubManager
import kotlinx.coroutines.runBlocking

/** The application entry point. */
fun main(): Unit = runBlocking {
    println("Main thread is ${Thread.currentThread().name}")
    MockedHubManager(this.coroutineContext).run()
}
