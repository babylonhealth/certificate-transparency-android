@file:Suppress("MagicNumber")

import com.novoda.gradle.release.PublishExtension

plugins {
    id("com.android.library")
}

apply(plugin = "com.novoda.bintray-release")

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(19)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-proguard-rules.pro")
    }
}

dependencies {
    api(project(":lib"))
}

configure<PublishExtension> {
    bintrayUser = System.getenv("BINTRAY_USER") ?: System.getProperty("BINTRAY_USER") ?: "unknown"
    bintrayKey = System.getenv("BINTRAY_KEY") ?: System.getProperty("BINTRAY_KEY") ?: "unknown"

    userOrg = "babylonpartners"
    groupId = "com.babylon.certificatetransparency"
    artifactId = "certificatetransparency-android"
    publishVersion = System.getenv("TRAVIS_TAG") ?: System.getProperty("TRAVIS_TAG") ?: "unknown"
    desc = "Certificate transparency for Android and Java"
    website = "https://github.com/Babylonpartners/certificate-transparency-android"

    dryRun = false
}
