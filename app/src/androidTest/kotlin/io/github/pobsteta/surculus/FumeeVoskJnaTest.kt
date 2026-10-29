package io.github.pobsteta.surculus

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Essai de fumée de la dictée (mission 071) : sur la release v0.7.80, R8 renommait les classes de JNA et de Vosk,
 * `LibVosk.<clinit>` plantait (« Can't obtain peer field ID for class com.sun.jna.Pointer ») et l'application se
 * fermait dès l'ouverture de l'écoute. Les tests unitaires et `assembleDebug` ne passent pas par R8 ; ce test, joué
 * sur le type de build `fumee` (R8 de la release), charge JNA puis LibVosk sans modèle téléchargé.
 *
 * Les règles de garde sont dans `proguard-rules.pro` (la release), pas dans `proguard-fumee.pro`.
 * Chargement par nom : si R8 renomme ou retire la classe, `ClassNotFoundException` est aussi un échec.
 */
@RunWith(AndroidJUnit4::class)
class FumeeVoskJnaTest {
    private fun charger(nom: String): Class<*> = Class.forName(nom, true, javaClass.classLoader)

    @Test
    fun jnaSeCharge() {
        // Native.<clinit> charge libjnidispatch.so puis appelle initIDs (champs de Pointer, etc.).
        assertEquals("com.sun.jna.Native", charger("com.sun.jna.Native").name)
        assertEquals("com.sun.jna.Pointer", charger("com.sun.jna.Pointer").name)
    }

    @Test
    fun libVoskSeCharge() {
        // LibVosk.<clinit> : Native.register("vosk") charge libvosk.so, sans modèle.
        assertEquals("org.vosk.LibVosk", charger("org.vosk.LibVosk").name)
    }
}
