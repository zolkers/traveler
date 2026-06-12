import org.gradle.api.artifacts.VersionCatalogsExtension
import com.diffplug.spotless.LineEnding

plugins {
    checkstyle
    jacoco
    id("com.diffplug.spotless")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

checkstyle {
    toolVersion = libs.findVersion("checkstyle").get().requiredVersion
    configDirectory.set(rootProject.layout.projectDirectory.dir("config/checkstyle"))
}

jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

spotless {
    lineEndings = LineEnding.UNIX

    java {
        target("src/**/*.java")
        removeUnusedImports()
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<Test>().configureEach {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
