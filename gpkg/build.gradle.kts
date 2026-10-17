import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    jacoco
}

// Module JVM (ADR 0004, écriture : ADR 0011) : lecture et écriture du travaux.gpkg, testées sans appareil.
// Un seul SQLite (sqlite-jdbc) sur la JVM et sur l'appareil, un petit écrivain GeoPackage maison,
// ni geopackage-java ni proj4j.
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
    implementation(libs.sqlite.jdbc)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Gson ne sert qu'aux tests et à l'outil de scénario de la CI (jamais dans l'APK).
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

// Outil de la CI (mission 007) : joue un scénario de saisies sur une tournée de sommieR et dépose l'export.
// Jamais embarqué dans l'APK (classes de test). Usage, depuis la racine du dépôt :
//   ./gradlew :gpkg:exporterScenario -Ptournee=<dossier de travaux.gpkg> -Psortie=<dossier>
//   [-Pscenario=fixtures/export/scenario-couchey.json]
tasks.register<JavaExec>("exporterScenario") {
    group = "verification"
    description = "Joue un scénario de saisies sur une tournée de sommieR et écrit l'export Surculus (zip et dossier)."
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("io.github.pobsteta.surculus.gpkg.outil.ExporterScenario")
    val racine = rootProject.layout.projectDirectory
    workingDir = racine.asFile
    val chemin = { nom: String, defaut: String? ->
        providers.gradleProperty(nom).orElse(providers.provider { defaut ?: throw GradleException("Option -P$nom=<chemin> manquante") })
            .map { racine.file(it).asFile.path }
    }
    args(chemin("tournee", null).get(), chemin("scenario", "fixtures/export/scenario-couchey.json").get(), chemin("sortie", null).get())
}
