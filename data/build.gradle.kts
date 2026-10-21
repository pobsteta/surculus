import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.pobsteta.surculus.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    // Tests Room sous Robolectric : les schémas exportés servent d'assets de test (ADR 0005).
    sourceSets {
        named("test") { assets.directories.add("$projectDir/schemas") }
    }
    testOptions.unitTests.isIncludeAndroidResources = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Schémas Room versionnés (exportSchema = true) : base des tests de migration.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Les tests relisent les scénarios du testeur : les déclarer, sinon Gradle ignore leurs changements.
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("fixtures/journal")).withPropertyName("fixturesJournal")
    inputs.dir(rootProject.file("fixtures/sauvegarde")).withPropertyName("fixturesSauvegarde")
    inputs.dir(rootProject.file("fixtures/sursync")).withPropertyName("fixturesSursync")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    // Journal Room en ajout seul (ADR 0005, mission 008).
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    // Lecture et écriture du .sursync (JSON Lines, arbre JSON sans réflexion) : mission 009.
    implementation(libs.gson)
    ksp(libs.room.compiler)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit4)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
