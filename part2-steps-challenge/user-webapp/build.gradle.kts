import com.moowork.gradle.node.yarn.YarnTask

apply(plugin = "com.moowork.node")

dependencies {
  val vertxVersion = project.extra["vertxVersion"]
  val logbackClassicVersion = project.extra["logbackClassicVersion"]

  implementation("io.vertx:vertx-rx-java2:$vertxVersion")
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
}
application {
  mainClassName = "tenksteps.webapp.users.UserWebAppVerticle"
}

tasks.register<YarnTask>("buildVueApp") {
  dependsOn("yarn_install")
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
