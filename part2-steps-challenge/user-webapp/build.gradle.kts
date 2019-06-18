import com.moowork.gradle.node.yarn.YarnTask

plugins {
  id("com.moowork.node") version "1.3.1"
}

dependencies {
  val vertxVersion = extra["vertxVersion"]
  val logbackClassicVersion = extra["logbackClassicVersion"]

  implementation("io.vertx:vertx-rx-java2:$vertxVersion")
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
}
application {
  mainClassName = "tenksteps.webapp.users.UserWebAppVerticle"
}

tasks.register<YarnTask>("buildVueApp") {
  inputs.file("package.json")
  inputs.file("yarn.lock")
  inputs.dir("src")
  outputs.dir("dist")
  args = listOf("build")
}

tasks.register<Copy>("copyVueDist") {
  dependsOn("buildVueApp")
  from("$projectDir/dist")
  into("$projectDir/src/main/resources/webroot/assets")
}

val processResources by tasks.named("processResources") {
  dependsOn("copyVueDist")
}

val clean by tasks.named<Delete>("clean") {
  delete("$projectDir/dist")
  delete("$projectDir/src/main/resources/webroot/assets")
}
