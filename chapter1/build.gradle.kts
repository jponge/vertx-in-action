plugins {
  java
  application
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
  }
}

dependencies {
  implementation("io.vertx:vertx-core:4.0.0-SNAPSHOT")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
  mainClassName = project.properties.getOrDefault("mainClass", "chapter1.firstapp.VertxEcho") as String
}

tasks.wrapper {
  gradleVersion = "6.3"
}
