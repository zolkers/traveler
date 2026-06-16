plugins {
    id("traveler.minecraft-common-conventions")
    id("traveler.test-conventions")
    id("traveler.quality-conventions")
}

description = "Traveler shared Minecraft 1.21.11 common code."

dependencies {
    api(project(":core"))
    api(project(":command_buildmycommand"))
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":command_buildmycommand")))
}
