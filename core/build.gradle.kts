import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
}

// Bytecode JVM 17 (Java + Kotlin cohérents) pour rester consommable par les modules Android.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.gson)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Rapport de couverture après chaque exécution des tests (lu par le badge, workflow badges.yml).
    finalizedBy("jacocoTestReport")
    // Les tests relisent les fixtures et les référentiels : les déclarer, sinon Gradle ignore leurs changements.
    inputs.dir(rootProject.file("fixtures")).withPropertyName("fixtures")
    inputs.dir(rootProject.file("docs/referentiels")).withPropertyName("referentiels")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn("test")
    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
        csv.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacoco.csv"))
    }
}
