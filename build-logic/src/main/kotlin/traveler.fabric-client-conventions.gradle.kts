import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("traveler.minecraft-common-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "modImplementation"(libs.findLibrary("fabric-loader").get())
    "modImplementation"(libs.findLibrary("fabric-api").get())
    "implementation"(libs.findLibrary("buildmycommand-api").get())
    "implementation"(libs.findLibrary("buildmycommand-core").get())
    "implementation"(libs.findLibrary("buildmycommand-annotations").get())
    "implementation"(libs.findLibrary("buildmycommand-adapters-minecraft-fabric").get())
}

loom {
    runs {
        named("client") {
            client()
            configName = "Traveler Fabric Client"
            ideConfigGenerated(true)
            runDir("run")
        }
        named("server") {
            server()
            configName = "Traveler Fabric Server"
            ideConfigGenerated(false)
            runDir("run-server")
        }
    }
}
