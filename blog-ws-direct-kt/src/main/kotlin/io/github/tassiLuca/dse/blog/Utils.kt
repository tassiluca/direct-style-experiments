package io.github.tassiLuca.dse.blog

import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/** Simulates a blocking [action] lasting between [minDuration] and [maxDuration] milliseconds. */
suspend fun String.simulates(action: String, minDuration: Int = 0, maxDuration: Int = 3_000) {
    println("[$this - ${Thread.currentThread().name} @ ${LocalTime.now()}] $action")
    delay((Random.nextInt(maxDuration) + minDuration).milliseconds)
    println("[$this - ${Thread.currentThread().name} @ ${LocalTime.now()}] ended $action")
}
