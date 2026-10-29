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

# JNA 5.18.1 et Vosk (vosk-android 0.3.75, ADR 0004), mission 071. Le code natif de JNA (libjnidispatch.so) cherche par
# leur nom les classes et les champs de com.sun.jna (Pointer.peer, Structure, Callback, les exceptions…) ; R8 les
# renommait, et l'écoute plantait avec « UnsatisfiedLinkError: Can't obtain peer field ID for class
# com.sun.jna.Pointer » (Native.initIDs, depuis LibVosk.<clinit>), release v0.7.80, journal de plantage du téléphone.
# Règle tirée de la FAQ de JNA pour Android/ProGuard : garder com.sun.jna.* et les membres publics de ses sous-classes
# (Structure, Callback, PointerType, Library : le natif lit leurs champs et appelle leurs méthodes par réflexion).
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }
# Vosk : LibVosk lie ses fonctions natives (libvosk.so) par Native.register, donc par le nom ; Model, Recognizer,
# SpeechService et leurs types passent par JNA (PointerType). On garde org.vosk pour que JNA retrouve ses noms.
-keep class org.vosk.** { *; }
# JNA référence AWT (Window, Component…) pour son support du bureau : absent sur Android.
-dontwarn java.awt.**
-dontwarn com.sun.jna.platform.**
