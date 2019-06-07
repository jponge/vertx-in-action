dependencies {
  val vertxVersion = extra["vertxVersion"]
  val junit5Version = extra["junit5Version"]
  val logbackClassicVersion = extra["logbackClassicVersion"]
  val restAssuredVersion = extra["restAssuredVersion"]
  val assertjVersion = extra["assertjVersion"]
  val testContainersVersion = extra["testContainersVersion"]

  implementation("io.vertx:vertx-rx-java2:$vertxVersion")
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-web-client:$vertxVersion")
  implementation("io.vertx:vertx-auth-jwt:$vertxVersion")
  implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")

  testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
  testImplementation("io.vertx:vertx-junit5:$vertxVersion")
  testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
  testImplementation("org.assertj:assertj-core:$assertjVersion")

  testImplementation(project(":user-profile-service"))
  testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")

  testRuntime("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
}

application {
  mainClassName = "tenksteps.userprofiles.UserProfileApiVerticle"
}

tasks.test {
  useJUnitPlatform()
}
