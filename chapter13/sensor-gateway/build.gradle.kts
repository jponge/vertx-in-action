val vertxVersion = project.extra["vertxVersion"]
val hzVersion = project.extra["hzVersion"]
val logbackClassicVersion = project.extra["logbackClassicVersion"]
val mpromVersion = project.extra["mpromVersion"]

dependencies {
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-hazelcast:$vertxVersion")
  implementation("com.hazelcast:hazelcast-kubernetes:$hzVersion")
  implementation("io.vertx:vertx-micrometer-metrics:$vertxVersion")
  implementation("io.micrometer:micrometer-registry-prometheus:$mpromVersion")
  implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
}

jib {
  from {
    image = "adoptopenjdk/openjdk11:ubi-minimal-jre"
  }
  to {
    image = "vertx-in-action/sensor-gateway"
    tags = setOf("v1", "latest")
  }
  container {
    mainClass = "chapter13.gateway.Gateway"
    jvmFlags = listOf("-noverify", "-Djava.security.egd=file:/dev/./urandom")
    ports = listOf("8080", "5701")
    user = "nobody:nobody"
  }
}
