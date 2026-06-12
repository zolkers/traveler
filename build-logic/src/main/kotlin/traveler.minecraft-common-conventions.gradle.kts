import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("fabric-loom")
    id("traveler.java-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "minecraft"(libs.findLibrary("minecraft").get())
    "mappings"(loom.officialMojangMappings())
}
