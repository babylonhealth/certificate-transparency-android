import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
    id("java-library")
    id("kotlin")
    id("org.jetbrains.dokka")
    id("org.owasp.dependencycheck")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${rootProject.extra["kotlin_version"]}")

    implementation("org.bouncycastle:bcpkix-jdk15on:1.61")
    implementation("org.bouncycastle:bcprov-jdk15on:1.61")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.5.0")
    implementation("com.squareup.retrofit2:converter-gson:2.5.0")
    testImplementation("com.squareup.retrofit2:retrofit-mock:2.5.0")

    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.25.0")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")

    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.1.5")
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions {
        allWarningsAsErrors = true
        freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
    }
}

tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "${project.buildDir}/javadoc"

    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://square.github.io/okhttp/3.x/okhttp/")
    })
}

dependencyCheck {
    failBuildOnCVSS = 0f

    analyzers {
        assemblyEnabled = false
    }
}

tasks.getByName("check").dependsOn(tasks.dependencyCheckAnalyze)
tasks.getByName("check").dependsOn(rootProject.tasks.getByName("detekt"))
tasks.getByName("check").dependsOn(rootProject.tasks.getByName("markdownlint"))
