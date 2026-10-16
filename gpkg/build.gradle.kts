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
}

tasks.test {
    useJUnitPlatform()
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
