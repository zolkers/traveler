pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
    }
}

rootProject.name = "traveler"

include(":core")
project(":core").projectDir = file("modules/core")

include(":mc_1_21_11_common")
project(":mc_1_21_11_common").projectDir = file("modules/mc/1_21_11/common")

include(":mc_1_21_11_fabric")
project(":mc_1_21_11_fabric").projectDir = file("modules/mc/1_21_11/fabric")
