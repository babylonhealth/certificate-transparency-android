import com.appmattus.markdown.rules.LineLengthRule
import com.appmattus.markdown.rules.ProperNamesRule

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.4.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.31")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
        classpath("org.owasp:dependency-check-gradle:5.0.0-M3.1")
        classpath("org.kt3k.gradle.plugin:coveralls-gradle-plugin:2.8.3")
        classpath("com.novoda:bintray-release:0.9.1")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC14"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("com.appmattus.markdown") version "0.4.1"
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
    rules {
        +LineLengthRule(codeBlocks = false)
        +ProperNamesRule { excludes = listOf(".*/NOTICE.md") }
    }
}
