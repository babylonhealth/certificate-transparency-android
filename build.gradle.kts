import com.appmattus.markdown.rules.LineLengthRule
import com.appmattus.markdown.rules.ProperNamesRule

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.10.1")
        classpath("org.owasp:dependency-check-gradle:5.3.2.1")
        //classpath("org.kt3k.gradle.plugin:coveralls-gradle-plugin:2.8.3")
        //classpath("com.novoda:bintray-release:0.9.2")
    }
}

plugins {
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

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

apply(from = "$rootDir/gradle/scripts/detekt.gradle.kts")
apply(from = "$rootDir/gradle/scripts/dependencyUpdates.gradle.kts")

markdownlint {
    rules {
        +LineLengthRule(codeBlocks = false)
        +ProperNamesRule { excludes = listOf(".*/NOTICE.md") }
    }
}
