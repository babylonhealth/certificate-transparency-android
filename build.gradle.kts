buildscript {
    extra["kotlin_version"] = "1.3.21"

    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.3.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlin_version"]}")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
        classpath("org.owasp:dependency-check-gradle:5.0.0-M2")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC14"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("com.appmattus.markdown") version "0.3.2"
}

allprojects {
    repositories {
        google()
        jcenter()

        // For material dialogs
        maven(url = "https://dl.bintray.com/drummer-aidan/maven/")
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}

detekt {
    input = files("$projectDir")
    filters = ".*test.*,.*androidTest.*,.*/resources/.*,.*/tmp/.*"
    config = files("detekt-config.yml")
}

markdownlint {
    configFile = File("markdownlint.gradle.kts")
}
