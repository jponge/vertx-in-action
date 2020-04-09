apply(plugin = "org.gradle.test-retry")

dependencies {
  val vertxVersion = project.extra["vertxVersion"]
  val junit5Version = project.extra["junit5Version"]
  val logbackClassicVersion = project.extra["logbackClassicVersion"]
  val restAssuredVersion = project.extra["restAssuredVersion"]
  val assertjVersion = project.extra["assertjVersion"]
  val testContainersVersion = project.extra["testContainersVersion"]

  implementation("io.vertx:vertx-rx-java2:$vertxVersion")
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")

  implementation("io.vertx:vertx-kafka-client:$vertxVersion") {
    exclude("org.slf4j")
    exclude("log4j")
  }
  implementation("io.vertx:vertx-pg-client:$vertxVersion")

  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testImplementation("io.vertx:vertx-junit5-rx-java2:$vertxVersion")
  testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
  testImplementation("org.assertj:assertj-core:$assertjVersion")

  testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
}

application {
  mainClassName = "tenksteps.activities.Main"
}

tasks.test {
  useJUnitPlatform()
  retry {
    maxRetries.set(1)
  }
}
