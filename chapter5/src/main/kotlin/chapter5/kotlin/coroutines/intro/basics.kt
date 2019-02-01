package chapter5.kotlin.coroutines.intro

import kotlinx.coroutines.*

suspend fun hello(): String {
  delay(1000)
  return "Hello!"
}

fun main() {
  runBlocking {
    println(hello())
  }
}
