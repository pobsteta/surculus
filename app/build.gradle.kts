import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.pobsteta.surculus"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.pobsteta.surculus"
        minSdk = 26
        targetSdk = 36
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "0.1.0"
    }

    // Signature release, distribution et clé : ADR 0010 (pas encore écrite). Seul le debug est construit.
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    // Pas de useLegacyPackaging (arbitrage de Pascal, 2026-10-16) : les bibliothèques natives
    // restent non compressées et alignées sur 16 Ko dans l'APK (contrôle en CI, scripts/verifier-16ko.sh).

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    // Dictée hors ligne (ADR 0004) : dépendances seules, aucun code de dictée avant la mission 013.
    // Elles sont là pour que le contrôle 16 Ko porte dès maintenant sur de vraies bibliothèques natives.
    // La JNA doit être tirée en .aar (bibliothèques natives), comme dans Marculus.
    implementation(libs.vosk.android)
    implementation(variantOf(libs.jna) { artifactType("aar") })
}
