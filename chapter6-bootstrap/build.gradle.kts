plugins {
  java
  id("com.github.johnrengelman.shadow") version "5.0.0"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.vertx:vertx-core:3.6.3")
  implementation("io.github.jponge:vertx-boot:1.0.0")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
  manifest {
    attributes(
      "Main-Class" to "io.vertx.core.Launcher",
      "Main-Verticle" to "io.github.jponge.vertx.boot.BootVerticle"
    )
  }
}

tasks.wrapper {
  gradleVersion = "5.2.1"
}
