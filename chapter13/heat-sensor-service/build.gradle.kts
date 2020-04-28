val vertxVersion = project.extra["vertxVersion"]
val hzVersion = project.extra["hzVersion"]
val logbackClassicVersion = project.extra["logbackClassicVersion"]

dependencies {
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-hazelcast:$vertxVersion")
  implementation("com.hazelcast:hazelcast-kubernetes:$hzVersion")
  implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
}

jib {
  from {
    image = "adoptopenjdk/openjdk11:ubi-minimal-jre"
  }
  to {
    image = "vertx-in-action/heat-sensor"
    tags = setOf("v1", "latest")
  }
  container {
    mainClass = "chapter13.sensor.HeatSensor"
    jvmFlags = listOf("-noverify", "-Djava.security.egd=file:/dev/./urandom")
    ports = listOf("8080", "5701")
    user = "nobody:nobody"
  }
}
