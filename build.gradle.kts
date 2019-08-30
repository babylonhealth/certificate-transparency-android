import com.appmattus.markdown.rules.LineLengthRule
import com.appmattus.markdown.rules.ProperNamesRule

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.50")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.18")
        classpath("org.owasp:dependency-check-gradle:5.2.1")
        classpath("org.kt3k.gradle.plugin:coveralls-gradle-plugin:2.8.3")
        classpath("com.novoda:bintray-release:0.9.1")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.0.1"
    id("com.github.ben-manes.versions") version "0.22.0"
    id("com.appmattus.markdown") version "0.5.0"
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

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.0.1")
}

detekt {
    input = files("$projectDir")
    buildUponDefaultConfig = true
    config = files("detekt-config.yml")
}

markdownlint {
    rules {
        +LineLengthRule(codeBlocks = false)
        +ProperNamesRule { excludes = listOf(".*/NOTICE.md") }
    }
}
