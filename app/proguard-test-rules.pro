# Règles R8 de l'APK de test de la release (essai de fumée, ADR 0011).
# androidx.test cite des annotations errorprone qui n'existent pas à l'exécution ; elles sont sans effet.
-dontwarn com.google.errorprone.annotations.**
