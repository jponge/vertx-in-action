package chapter5.kotlin.coroutines.intro

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun main(args: Array<String>) = coroutineScope {
  val job1 = launch { Thread.sleep(5000) }

  fun fib(n: Long): Long = if (n < 2) n else fib(n - 1) + fib(n - 2)
  val job2 = async { fib(42) }

  job1.join()
  println("job1 has completed")
  println("job2 fib(42) = ${job2.await()}")
}
