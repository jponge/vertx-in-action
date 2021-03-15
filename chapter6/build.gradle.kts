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
  implementation("io.vertx:vertx-core:4.0.3")
  implementation("io.vertx:vertx-codegen:4.0.3")
  implementation("io.vertx:vertx-service-proxy:4.0.3")

  annotationProcessor("io.vertx:vertx-service-proxy:4.0.3")
  annotationProcessor("io.vertx:vertx-codegen:4.0.3:processor")

  implementation("io.vertx:vertx-rx-java2:4.0.3")
  annotationProcessor("io.vertx:vertx-rx-java2-gen:4.0.3")

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
  testImplementation("io.vertx:vertx-junit5:4.0.3")
  testImplementation("org.assertj:assertj-core:3.11.1")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
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
  gradleVersion = "6.8.3"
}
