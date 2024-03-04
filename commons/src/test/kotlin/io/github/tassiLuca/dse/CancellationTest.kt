package io.github.tassiLuca.dse

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CancellationTest : FunSpec({
    test("In a coroutineScope, if a coroutine throw an exception, all other coroutines are cancelled") {
        var terminated = false
        val result = runCatching {
            runBlocking {
                val f1 = launch { delay(5_000); terminated = true }
                val f2 = async { delay(1_000); throw Exception("Error") }
                // despite joining f1 first, this will not complete because f2 throws an exception before
                f1.join()
                f2.await()
            }
        }
        result.isFailure shouldBe true
        terminated shouldBe false
    }
})
