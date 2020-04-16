plugins {
  id("com.github.ben-manes.versions") version "0.28.0"
  id("com.github.johnrengelman.shadow") version "5.2.0" apply false
  id("com.adarshr.test-logger") version "2.0.0" apply false
  id("com.moowork.node") version "1.3.1" apply false
  id("org.gradle.test-retry") version "1.1.4" apply false
}

allprojects {
  extra["vertxVersion"] = if (project.hasProperty("vertxVersion")) project.property("vertxVersion") else "4.0.0-SNAPSHOT"
  extra["junit5Version"] = "5.6.1"
  extra["restAssuredVersion"] = "4.3.0"
  extra["logbackClassicVersion"] = "1.2.3"
  extra["assertjVersion"] = "3.15.0"
  extra["testContainersVersion"] = "1.13.0"
  extra["caffeineVersion"] = "2.8.1"
}

subprojects {

  repositories {
//    mavenLocal {
//      content {
//        includeGroup("io.vertx")
//      }
//    }
    mavenCentral()
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
  }

  apply(plugin = "java")
  apply(plugin = "application")
  apply(plugin = "com.github.johnrengelman.shadow")
  apply(plugin = "com.adarshr.test-logger")

  tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
  }

  tasks.named<Test>("test") {
    reports.html.isEnabled = false
  }
}

tasks.register<TestReport>("testReport") {
  description = "Aggregate all test results as a HTML report"
  group = "Documentation"
  destinationDir = file("$buildDir/reports/allTests")
  reportOn(subprojects.map { it.tasks["test"] })
}

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
  gradleVersion = "6.3"
}
