# Règles R8 de Surculus (build release, pas encore publié : ADR 0010).

# sqlite-jdbc (ADR 0011). Le code natif (libsqlitejdbc.so, JNI) cherche par leur nom des classes, des champs et des
# méthodes de org.sqlite : NativeDB et ses rappels (Function, Collation, ProgressHandler, BusyHandler…), les
# exceptions, les structures de métadonnées. Si R8 les renomme ou les retire, l'appel natif plante à l'exécution,
# sans erreur à la compilation. Le test de fumée du workflow release (écrire puis relire une tournée sur émulateur)
# existe pour attraper ce cas.
-keep class org.sqlite.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
# Classes optionnelles absentes sur Android (journalisation, compilation native GraalVM).
-dontwarn org.slf4j.**
-dontwarn org.graalvm.**
# java.sql.JDBCType n'existe pas dans l'API Android : seul JDBC3PreparedStatement.getParameterTypeName() la cite,
# et Surculus ne l'appelle jamais (rapport de R8, missing_rules.txt, 2026-10-17).
-dontwarn java.sql.JDBCType

# :gpkg (mission 006) : l'application n'appelle pas encore le module, donc R8 retire toutes ses classes de l'APK
# release, et l'essai de fumée n'éprouverait plus les natifs de sqlite-jdbc (le vrai risque de l'ADR 0011).
# On garde les points d'entrée publics. À retirer quand l'app appelle :gpkg.
-keep class io.github.pobsteta.surculus.gpkg.** { public *; }
# Idem pour :core, que l'essai de fumée emploie (repères, états, totaux) : sans cela R8 le renomme dans l'application.
-keep class io.github.pobsteta.surculus.core.** { public *; }

# Essai de fumée (mission 006) : le runner androidx.test de l'APK de test charge androidx.tracing.Trace depuis
# l'APK de l'application (les classes déjà présentes dans l'application ne sont pas reprises dans l'APK de test), ainsi que
# la bibliothèque standard de Kotlin.
# R8 la retirait de la release, car l'application ne l'appelle pas : le runner plantait avant le premier test.
-keep class androidx.tracing.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
