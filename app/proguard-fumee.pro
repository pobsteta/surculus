# Règles R8 du seul type de build `fumee` (essai de fumée de la release, mission 006, ADR 0011).
# Le type `fumee` est calqué sur `release` (même R8, mêmes natifs, même signature) ; ces règles s'y ajoutent et
# n'atteignent jamais l'APK release distribué.

# :gpkg et :core : l'application n'appelle pas encore ces modules, donc R8 retire leurs classes de l'APK, et l'essai
# n'éprouverait plus les natifs de sqlite-jdbc (le vrai risque de l'ADR 0011). On garde leurs points d'entrée publics.
# À retirer quand l'app appelle :gpkg.
-keep class io.github.pobsteta.surculus.gpkg.** { public *; }
-keep class io.github.pobsteta.surculus.core.** { public *; }

# Le runner androidx.test de l'APK de test charge androidx.tracing et la bibliothèque standard de Kotlin depuis l'APK
# de l'application (les classes déjà présentes dans l'application ne sont pas reprises dans l'APK de test) ; R8 les
# retirait, et le runner plantait avant le premier test.
-keep class androidx.tracing.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
