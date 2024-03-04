package io.github.tassiLuca.dse

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope

class CoroutinesCancellationTests : FunSpec() {

    init {
        test("in coroutineScope, a failure of a nested coroutine propagates upwards, cancelling all other ones") {
            var job2Completed = false
            val result = runCatching {
                coroutineScope {
                    val job1 = failingJob()
                    val job2 = successfulJob { job2Completed = true }
                    job2.await() // please note we wait first for the successful job
                    job1.await()
                }
            }
            result.isFailure shouldBe true
            job2Completed shouldBe false
        }

        test("supervisorScope does not propagate, allowing other coroutines to complete") {
            var job2Completed = false
            supervisorScope {
                val job1 = failingJob()
                val job2 = successfulJob { job2Completed = true }
                runCatching { job1.await() }.isFailure shouldBe true
                runCatching { job2.await() }.isSuccess shouldBe true
            }
            job2Completed shouldBe true
        }
    }

    private fun CoroutineScope.failingJob() = async<String> {
        delay(1_000)
        error("Error")
    }

    private fun CoroutineScope.successfulJob(action: () -> Unit) = async {
        delay(2_000)
        action()
    }
}
