import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// ---------------------------------------------------------------------------------------------------------
// sqlite-jdbc sur l'appareil (ADR 0011). Maven Central publie, à côté du jar, un classifieur `natives-android`
// qui contient libsqlitejdbc.so par ABI (aarch64, arm, x86_64, x86), liées en pages de 16 Ko depuis 3.50.3.0.
// On en extrait les ABI embarquées (abiFilters ci-dessous) vers un dossier jniLibs généré ; sqlite-jdbc le charge
// alors par System.loadLibrary("sqlitejdbc") sur Android.
// ---------------------------------------------------------------------------------------------------------
val nativesSqlite: Configuration = configurations.create("nativesSqlite") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val natifsSqliteDir = layout.buildDirectory.dir("generated/jniLibs-sqlite")

abstract class ExtraireNativesSqlite : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val jars: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val sortie: DirectoryProperty

    @TaskAction
    fun extraire() {
        // Dossier du jar -> ABI Android. x86 (32 bits) n'est pas embarquée (abiFilters).
        val abis = mapOf("aarch64" to "arm64-v8a", "arm" to "armeabi-v7a", "x86_64" to "x86_64")
        val racine = sortie.get().asFile
        racine.deleteRecursively()
        var extraites = 0
        for (jar in jars.files) {
            ZipFile(jar).use { zip ->
                for (entree in zip.entries().asSequence().filter { !it.isDirectory }) {
                    val m = Regex("org/sqlite/native/Linux-Android/([^/]+)/libsqlitejdbc\\.so").matchEntire(entree.name) ?: continue
                    val abi = abis[m.groupValues[1]] ?: continue
                    val cible = File(racine, "$abi/libsqlitejdbc.so")
                    cible.parentFile.mkdirs()
                    zip.getInputStream(entree).use { entrant -> cible.outputStream().use { entrant.copyTo(it) } }
                    extraites++
                }
            }
        }
        check(extraites == abis.size) { "libsqlitejdbc.so : $extraites ABI extraites sur ${abis.size} attendues" }
    }
}

val extraireNativesSqlite = tasks.register<ExtraireNativesSqlite>("extraireNativesSqlite") {
    group = "build"
    description = "Extrait libsqlitejdbc.so (arm64-v8a, armeabi-v7a, x86_64) du classifieur natives-android de sqlite-jdbc."
    jars.from(nativesSqlite)
    sortie.set(natifsSqliteDir)
}

android {
    namespace = "io.github.pobsteta.surculus"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.pobsteta.surculus"
        minSdk = 26
        targetSdk = 36
        // Version injectée par la CI (-PappVersionName / -PappVersionCode) ; valeurs de repli en local.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ABI embarquées (arbitrage de Pascal, 2026-10-16) : téléphones 64 et 32 bits ARM, émulateur
        // x86_64. On retire armeabi, mips, mips64 et x86, que JNA embarque encore.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    // Clé de signature release lue dans l'environnement (CI, secrets) ou les propriétés Gradle,
    // jamais dans le dépôt. Sans keystore, repli sur la signature debug (builds locaux seulement).
    // La CI ne publie jamais d'APK debug : docs/publier.md. Distribution et clé : ADR 0010.
    val cheminKeystore = System.getenv("RELEASE_KEYSTORE") ?: (project.findProperty("releaseKeystore") as String?)
    val keystoreExiste = cheminKeystore != null && file(cheminKeystore).exists()
    if (cheminKeystore != null && !keystoreExiste) {
        logger.warn("RELEASE_KEYSTORE pointe vers un fichier absent ($cheminKeystore) : signature debug utilisée.")
    }

    signingConfigs {
        if (keystoreExiste) {
            create("release") {
                storeFile = file(cheminKeystore!!)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: (project.findProperty("releaseKeystorePassword") as String?)
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                    ?: (project.findProperty("releaseKeyAlias") as String?)
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                    ?: (project.findProperty("releaseKeyPassword") as String?)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Un APK signé debug ne se met pas à jour en APK signé release (signatures différentes).
            signingConfig = signingConfigs.getByName(if (keystoreExiste) "release" else "debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // Essai de fumée (ADR 0011) : calqué sur la release (même R8, mêmes natifs, même signature), avec en plus les
        // règles de proguard-fumee.pro. Ces règles ne touchent jamais l'APK release distribué.
        create("fumee") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            // INDISPENSABLE : sans suffixe, l'APK de fumée (versionCode 1 en local) porte l'identifiant de l'application
            // installée et tente de la remplacer ; refusé pour « downgrade », le nettoyage du lanceur de test la
            // désinstalle alors, avec ses données. Avec le suffixe, il s'installe à côté et ne peut ni la remplacer
            // ni la désinstaller.
            applicationIdSuffix = ".fumee"
            versionNameSuffix = "-fumee"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-fumee.pro",
            )
            // L'APK de test est minifié comme lui : androidx.test cite des annotations errorprone absentes.
            testProguardFiles("proguard-test-rules.pro")
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
        // sqlite-jdbc sur Android (ADR 0011, USAGE.md du projet) : désucrage des API Java récentes.
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            // Le jar de sqlite-jdbc embarque les bibliothèques natives de bureau (Linux, Windows, macOS, BSD) :
            // inutiles sur Android, où l'on charge libsqlitejdbc.so depuis jniLibs. On les retire de l'APK.
            excludes += "org/sqlite/native/**"
            excludes += "META-INF/native-image/**"
        }
    }

    // Essai de fumée (ADR 0011) : les tests instrumentés jouent sur l'APK **release** (R8, natifs, désucrage), pas sur
    // le debug. Ils tournent sur émulateur dans release.yml : `./gradlew :app:connectedFumeeAndroidTest`.
    testBuildType = "fumee"
    // La tournée de départ de l'essai est le modèle de sommieR de :gpkg (un seul exemplaire dans le dépôt).
    sourceSets.getByName("androidTest").assets.directories.add(rootProject.file("gpkg/src/test/resources/modele").path)

    // Natifs Android de sqlite-jdbc, extraits du classifieur `natives-android` (ADR 0011).
    sourceSets.getByName("main").jniLibs.directories.add(natifsSqliteDir.get().asFile.path)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    // Lecture et écriture du travaux.gpkg (ADR 0011) : sqlite-jdbc arrive avec le module, ses natifs par la tâche ci-dessus.
    implementation(project(":gpkg"))
    nativesSqlite("org.xerial:sqlite-jdbc:${libs.versions.sqliteJdbc.get()}:natives-android")
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Carte : osmdroid, fonds IGN et OSM en ligne (ADR 0009, amendé par la mission 055).
    implementation(libs.osmdroid.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Dictée hors ligne (ADR 0004, mission 013) ; terrain-voix : installation du modèle et grammaire.
    // Les bibliothèques natives de Vosk passent le contrôle 16 Ko.
    // La JNA doit être tirée en .aar (bibliothèques natives), comme dans Marculus.
    implementation(libs.vosk.android)
    implementation(libs.terrain.voix)
    implementation(variantOf(libs.jna) { artifactType("aar") })
}

// Les natifs doivent être là avant toute fusion de ressources JNI.
tasks.named("preBuild") { dependsOn(extraireNativesSqlite) }

// La fumée échoue quand aucun test ne tourne (Gradle rend un succès si l'instrumentation plante avant le premier test).
// Le rapport est effacé avant l'essai, pour qu'un ancien rapport vert ne masque pas un plantage.
val rapportsFumee = layout.buildDirectory.dir("outputs/androidTest-results/connected/fumee").get().asFile
val verifierScriptFumee = rootProject.file("scripts/verifier-fumee.sh")
val verifierFumee = tasks.register<Exec>("verifierFumee") {
    description = "Vérifie que la fumée a joué au moins un test, sans échec (scripts/verifier-fumee.sh)."
    commandLine("bash", verifierScriptFumee.path, rapportsFumee.path)
}
val effacerRapportFumee = tasks.register<Delete>("effacerRapportFumee") { delete(rapportsFumee) }
tasks.matching { it.name == "connectedFumeeAndroidTest" }.configureEach {
    dependsOn(effacerRapportFumee)
    finalizedBy(verifierFumee)
}
