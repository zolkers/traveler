plugins {
    id("traveler.minecraft-common-conventions")
    id("traveler.test-conventions")
    id("traveler.quality-conventions")
}

description = "Traveler shared Minecraft 1.21.11 common code."

dependencies {
    api(project(":core"))
    implementation(libs.buildmycommand.api)
    implementation(libs.buildmycommand.core)
    implementation(libs.buildmycommand.annotations)
}
