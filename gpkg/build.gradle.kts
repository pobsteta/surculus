import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
}

// Module JVM (ADR 0004) : lecture et écriture du travaux.gpkg, testées sans appareil.
// geopackage-java sera branché par la mission 006 ; seul le pilote SQLite est posé ici.
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
    implementation(project(":core"))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.sqlite.jdbc)
    testImplementation(libs.gson)
}

tasks.test {
    useJUnitPlatform()
    // Les cas d'acceptation de l'export (mission 020) relisent les fixtures et les référentiels.
    inputs.dir(rootProject.file("fixtures")).withPropertyName("fixtures")
    inputs.dir(rootProject.file("docs/referentiels")).withPropertyName("referentiels")
    // Rapport de couverture après chaque exécution des tests (lu par le badge, workflow badges.yml).
    finalizedBy("jacocoTestReport")
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
