plugins {
    `java-test-fixtures`
    id("traveler.java-conventions")
    id("traveler.test-conventions")
    id("traveler.quality-conventions")
}

description = "BuildMyCommand adapter for Traveler commands."

dependencies {
    api(project(":core"))
    api(libs.buildmycommand.api)
    api(libs.buildmycommand.core)
    implementation(libs.buildmycommand.annotations)
    testImplementation(testFixtures(project(":core")))
}
