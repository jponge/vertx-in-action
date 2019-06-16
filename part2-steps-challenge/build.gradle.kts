plugins {
  id("com.github.ben-manes.versions") version "0.21.0"
  id("com.github.johnrengelman.shadow") version "5.0.0" apply false
  id("com.adarshr.test-logger") version "1.6.0" apply false
}

subprojects {

  repositories {
    mavenCentral()
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }

  extra["vertxVersion"] = "3.8.0-SNAPSHOT"
  extra["junit5Version"] = "5.4.2"
  extra["restAssuredVersion"] = "4.0.0"
  extra["logbackClassicVersion"] = "1.2.3"
  extra["assertjVersion"] = "3.11.1"
  extra["testContainersVersion"] = "1.11.3"

  apply(plugin = "java")
  apply(plugin = "application")
  apply(plugin = "com.github.johnrengelman.shadow")
  apply(plugin = "com.adarshr.test-logger")

  tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
  }
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
  gradleVersion = "5.4.1"
}
