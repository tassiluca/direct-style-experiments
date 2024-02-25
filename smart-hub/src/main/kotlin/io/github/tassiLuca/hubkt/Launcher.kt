package io.github.tassiLuca.hubkt

import io.github.tassiLuca.hubkt.adapters.MockedHubManager
import kotlinx.coroutines.runBlocking

/** The application entry point. */
fun main(): Unit = runBlocking {
    MockedHubManager(this.coroutineContext).run()
}
