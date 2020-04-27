plugins {
  id("com.google.cloud.tools.jib") version "2.2.0" apply false
}

allprojects {
  extra["vertxVersion"] = "4.0.0-SNAPSHOT"
  extra["hzVersion"] = "1.5.3"
  extra["logbackClassicVersion"] = "1.2.3"

  repositories {
    mavenCentral()
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }
}

subprojects {
  apply(plugin="java")
  apply(plugin="com.google.cloud.tools.jib")

  tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
  }
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
  gradleVersion = "6.3"
}
