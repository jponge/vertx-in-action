import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA

plugins {
  id("com.github.ben-manes.versions") version "0.33.0"
  id("com.adarshr.test-logger") version "2.1.0"
  id("com.github.johnrengelman.shadow") version "6.1.0" apply false
  id("com.moowork.node") version "1.3.1" apply false
  id("org.gradle.test-retry") version "1.1.9" apply false
}

allprojects {
  extra["vertxVersion"] = if (project.hasProperty("vertxVersion")) project.property("vertxVersion") else "4.0.0.Beta3"
  extra["junit5Version"] = "5.7.0"
  extra["restAssuredVersion"] = "4.3.1"
  extra["logbackClassicVersion"] = "1.2.3"
  extra["assertjVersion"] = "3.17.2"
  extra["testContainersVersion"] = "1.15.0-rc2"
}

subprojects {

  repositories {
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

  testlogger {
    theme = MOCHA
    slowThreshold = 5000
    showStandardStreams = true
    showFullStackTraces = true
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
  gradleVersion = "6.6.1"
}
