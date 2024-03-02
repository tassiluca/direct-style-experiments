import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun simple(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(500)
        println("Emitting $i")
        emit(i)
    }
}

fun main(): Unit = runBlocking {
    println("Calling simple function...")
    val flow = simple()
    launch {
        println("Calling collect...")
        flow.collect { value -> println(value) }
    }
    launch {
        println("Calling collect again...")
        flow.collect { value -> println(value) }
    }
}