import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("traveler.minecraft-common-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val embeddedBuildMyCommand = listOf(
    "buildmycommand-api",
    "buildmycommand-core",
    "buildmycommand-annotations",
    "buildmycommand-dsl",
    "buildmycommand-adapters-core",
    "buildmycommand-adapters-brigadier",
    "buildmycommand-adapters-minecraft-common",
    "buildmycommand-adapters-minecraft-fabric",
)

dependencies {
    "modImplementation"(libs.findLibrary("fabric-loader").get())
    "modImplementation"(libs.findLibrary("fabric-api").get())
    embeddedBuildMyCommand.forEach { alias ->
        val dependency = libs.findLibrary(alias).get()
        "implementation"(dependency)
        "include"(dependency)
    }
}

loom {
    runs {
        named("client") {
            client()
            configName = "Traveler Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}
