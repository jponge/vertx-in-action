plugins {
  java
  application
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.vertx:vertx-core:3.6.3")
  implementation("io.vertx:vertx-codegen:3.6.3")
  implementation("io.vertx:vertx-service-proxy:3.6.3")

  annotationProcessor("io.vertx:vertx-service-proxy:3.6.3")
  annotationProcessor("io.vertx:vertx-codegen:3.6.3:processor")

  implementation("io.vertx:vertx-rx-java2:3.6.3")
  annotationProcessor("io.vertx:vertx-rx-java2-gen:3.6.3")

  testCompile("org.junit.jupiter:junit-jupiter-api:5.4.0")
  testCompile("io.vertx:vertx-junit5:3.6.3")
  testCompile("org.assertj:assertj-core:3.11.1")

  testRuntime("org.junit.jupiter:junit-jupiter-engine:5.4.0")
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}

tasks.getByName<JavaCompile>("compileJava") {
  options.annotationProcessorGeneratedSourcesDirectory = File("$projectDir/src/main/generated")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
  mainClassName = "chapter6.RxProxyClient"
}

tasks.wrapper {
  gradleVersion = "5.2.1"
}
