@file:Suppress("MagicNumber")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "com.babylon.certificatetransparency.sampleapp"
        minSdkVersion(19)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/atomicfu.kotlin_module")
    }
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(project(":android"))
    implementation(kotlin("stdlib-jdk7"))
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta3")
    implementation("com.google.android.material:material:1.1.0-beta02")
    implementation("com.google.android.gms:play-services-base:17.1.0")
    implementation("com.squareup.retrofit2:retrofit:2.6.2")
    implementation("com.squareup.retrofit2:converter-gson:2.6.2")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.1.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.1.0")
    implementation("android.arch.navigation:navigation-fragment-ktx:1.0.0")
    implementation("android.arch.navigation:navigation-ui-ktx:1.0.0")
    implementation("com.xwray:groupie:2.7.1")
    implementation("com.xwray:groupie-kotlin-android-extensions:2.7.1")
    implementation("com.afollestad.material-dialogs:core:3.1.1")
    implementation("com.afollestad.material-dialogs:input:3.1.1")
    implementation("com.pddstudio:highlightjs-android:1.5.0")
    implementation("com.android.volley:volley:1.1.1")
    implementation("com.github.spullara.mustache.java:compiler:0.9.6")
}
