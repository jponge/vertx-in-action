val vertxVersion = project.extra["vertxVersion"]
val logbackClassicVersion = project.extra["logbackClassicVersion"]

dependencies {
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-web-client:$vertxVersion")
  implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
}

jib {
  from {
    image = "adoptopenjdk/openjdk11:ubi-minimal-jre"
  }
  to {
    image = "vertx-in-action/heat-api"
    tags = setOf("v1", "latest")
  }
  container {
    mainClass = "chapter13.api.HeatApi"
    jvmFlags = listOf("-noverify", "-Djava.security.egd=file:/dev/./urandom")
    ports = listOf("8080")
    user = "nobody:nobody"
  }
}
