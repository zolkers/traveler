plugins {
    id("traveler.fabric-client-conventions")
    id("traveler.test-conventions")
    id("traveler.quality-conventions")
}

description = "Traveler Fabric client runtime for Minecraft 1.21.11."

dependencies {
    implementation(project(path = ":mc_1_21_11_common", configuration = "namedElements"))
}
