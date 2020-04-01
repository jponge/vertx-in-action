plugins {
  java
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
  }
}

dependencies {
  implementation("io.vertx:vertx-core:4.0.0-SNAPSHOT")
  implementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks.create<JavaExec>("run") {
  main = project.properties.getOrDefault("mainClass", "chapter2.hello.HelloVerticle") as String
  classpath = sourceSets["main"].runtimeClasspath
  systemProperties["vertx.logger-delegate-factory-class-name"] = "io.vertx.core.logging.SLF4JLogDelegateFactory"
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.wrapper {
  gradleVersion = "6.3"
}
