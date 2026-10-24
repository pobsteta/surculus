pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // terrain-voix (mission 013) : bibliothèque commune de pobsteta, publiée par JitPack (terrain/README.md).
        maven("https://jitpack.io") { content { includeGroup("com.github.pobsteta.terrain") } }
    }
}

rootProject.name = "Surculus"

// Modules (ADR 0004) : domaine pur, GeoPackage JVM, journal Room, application Compose.
include(":core")
include(":gpkg")
include(":data")
include(":app")
