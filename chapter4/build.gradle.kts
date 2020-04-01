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
  main = project.properties.getOrDefault("mainClass", "chapter4.jukebox.Main") as String
  classpath = sourceSets["main"].runtimeClasspath
  systemProperties["vertx.logger-delegate-factory-class-name"] = "io.vertx.core.logging.SLF4JLogDelegateFactory"
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.wrapper {
  gradleVersion = "6.3"
}
