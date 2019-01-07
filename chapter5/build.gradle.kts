import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  java
  kotlin("jvm") version "1.3.11"
}

repositories {
  jcenter()
}

dependencies {
  implementation("io.vertx:vertx-core:3.6.2")
  implementation("io.vertx:vertx-rx-java2:3.6.2")
  implementation("io.vertx:vertx-web-client:3.6.2")

  implementation("io.vertx:vertx-lang-kotlin:3.6.2")
  implementation("io.vertx:vertx-lang-kotlin-coroutines:3.6.2")
  implementation(kotlin("stdlib-jdk8"))

  implementation("ch.qos.logback:logback-classic:1.2.3")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"

tasks.wrapper {
  gradleVersion = "5.1"
}
