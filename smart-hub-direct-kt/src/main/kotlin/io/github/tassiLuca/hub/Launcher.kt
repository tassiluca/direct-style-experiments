package io.github.tassiLuca.hub

import io.github.tassiLuca.hub.adapters.MockedHubManager
import kotlinx.coroutines.runBlocking

/** The application entry point. */
fun main(): Unit = runBlocking {
    MockedHubManager(this.coroutineContext).run()
}
