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
  implementation("io.vertx:vertx-infinispan:4.0.0-SNAPSHOT")
  implementation("ch.qos.logback:logback-classic:1.2.3")
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.1")
  testImplementation("io.vertx:vertx-junit5:4.0.0-SNAPSHOT")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.1")
}

tasks.create<JavaExec>("run") {
  main = project.properties.getOrDefault("mainClass", "chapter3.local.Main") as String
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
