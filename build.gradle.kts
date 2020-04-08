tasks.register("build-chapters") {
  description = "Build the chapter-specific examples"
  group = "build"
  dependsOn(chapterProjects().map { it.task(":build") })
}

tasks.register("build-part2-steps-challenge") {
  description = "Build the part2 10k steps challenge application"
  group = "build"
  dependsOn(gradle.includedBuild("part2-steps-challenge").task(":build-all"))
}

tasks.register("clean") {
  group = "build"
  description = "Clean all projects"
  dependsOn(chapterProjects().map { it.task(":clean") })
  dependsOn(gradle.includedBuild("part2-steps-challenge").task(":clean-all"))
}

tasks.wrapper {
  gradleVersion = "6.3"
}

fun chapterProjects() = gradle.includedBuilds.filter { it.name.startsWith("chapter") }
