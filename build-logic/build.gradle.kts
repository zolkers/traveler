plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.versions.spotless.get()}")
    implementation("net.fabricmc:fabric-loom:${libs.versions.fabricLoom.get()}")
}
