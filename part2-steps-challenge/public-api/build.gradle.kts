apply(plugin = "org.gradle.test-retry")

dependencies {
  val vertxVersion = project.extra["vertxVersion"]
  val junit5Version = project.extra["junit5Version"]
  val logbackClassicVersion = project.extra["logbackClassicVersion"]
  val restAssuredVersion = project.extra["restAssuredVersion"]
  val assertjVersion = project.extra["assertjVersion"]
  val testContainersVersion = project.extra["testContainersVersion"]
  val caffeineVersion = project.extra["caffeineVersion"]

  implementation("io.vertx:vertx-rx-java2:$vertxVersion")
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-web-client:$vertxVersion")
  implementation("io.vertx:vertx-auth-jwt:$vertxVersion")
  implementation("io.vertx:vertx-circuit-breaker:$vertxVersion")
  implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
  implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")

  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testImplementation("io.vertx:vertx-junit5-rx-java2:$vertxVersion")
  testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
  testImplementation("org.assertj:assertj-core:$assertjVersion")

  testImplementation(project(":user-profile-service"))
  testImplementation(project(":activity-service"))
  testImplementation("io.vertx:vertx-pg-client:$vertxVersion")
  testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
}

application {
  mainClassName = "tenksteps.publicapi.PublicApiVerticle"
}

tasks.test {
  useJUnitPlatform()
  retry {
    maxRetries.set(1)
  }
}
