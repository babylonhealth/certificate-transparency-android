import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.appmattus.markdown.rules.LineLengthRule
import com.appmattus.markdown.rules.ProperNamesRule
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTask

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.1")
    }
}

plugins {
    kotlin("jvm") version "1.4.10" apply false
    id("org.jetbrains.dokka") version "1.4.0"
    id("org.owasp.dependencycheck") version "6.0.0.1"
    id("com.appmattus.markdown") version "0.6.0"
}

allprojects {
    repositories {
        google()
        jcenter()

        // For material dialogs
        maven(url = "https://dl.bintray.com/drummer-aidan/maven/")
    }
}

subprojects {
    tasks.withType<DokkaTask> {
        outputDirectory.set(buildDir.resolve("reports/dokka"))

        dokkaSourceSets {
            configureEach {
                skipDeprecated.set(true)

                sourceLink {
                    localDirectory.set(rootDir)
                    remoteUrl.set(java.net.URL("https://github.com/babylonhealth/certificate-transparency-android/blob/main/"))
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

apply(from = "$rootDir/gradle/scripts/detekt.gradle.kts")
apply(from = "$rootDir/gradle/scripts/dependencyUpdates.gradle.kts")

val dokka = tasks.named<DokkaMultiModuleTask>("dokkaHtmlMultiModule") {
    outputDirectory.set(buildDir.resolve("dokkaCustomMultiModuleOutput"))
    documentationFileName.set("module.md")
}

tasks.register("check").dependsOn(dokka)

markdownlint {
    rules {
        +LineLengthRule(codeBlocks = false)
        +ProperNamesRule { excludes = listOf(".*/NOTICE.md") }
    }
}
