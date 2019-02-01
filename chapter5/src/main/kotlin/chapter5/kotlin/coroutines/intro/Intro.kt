package chapter5.kotlin.coroutines.intro

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
  val job1 = launch { delay(500) }

  fun fib(n: Long): Long = if (n < 2) n else fib(n - 1) + fib(n - 2)
  val job2 = async { fib(42) }

  job1.join()
  println("job1 has completed")
  println("job2 fib(42) = ${job2.await()}")
}
