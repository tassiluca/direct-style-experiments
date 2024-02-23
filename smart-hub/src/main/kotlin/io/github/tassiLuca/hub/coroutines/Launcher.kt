package io.github.tassiLuca.hub.coroutines

import io.github.tassiLuca.hub.coroutines.infrastructure.MockedHubManager
import kotlinx.coroutines.runBlocking

/** The application entry point. */
fun main(): Unit = runBlocking {
    println("Main thread is ${Thread.currentThread().name}")
    MockedHubManager(this.coroutineContext).run()
}
