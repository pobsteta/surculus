package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.carte.SECONDES_DE_MOYENNE_ARRIVEE
import io.github.pobsteta.surculus.carte.collecterPositionsArrivee
import io.github.pobsteta.surculus.core.gnss.Position
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-B2 : la boussole du téléphone en Kotlin pur (lissage, classe de précision, champ), et ses garde-fous de source. */
class BoussoleTest {
    /** Écart angulaire entre deux caps, sur le cercle. */
    private fun ecart(a: Double, b: Double): Double = ((a - b) % 360.0 + 540.0) % 360.0 - 180.0

    @Test fun `un cap est ramene dans 0 a 360, un cap non fini n'existe pas`() {
        assertEquals(10.0, assertNotNull(CapMagnetique.de(370.0)).degres, 1e-9)
        assertEquals(350.0, assertNotNull(CapMagnetique.de(-10.0)).degres, 1e-9)
        assertNull(CapMagnetique.de(Double.NaN))
        assertNull(CapMagnetique.de(Double.POSITIVE_INFINITY))
    }

    @Test fun `359 et 1 degres donnent 0, pas 180`() {
        val m = assertNotNull(LissageCap.moyenne(listOf(359.0, 1.0))).degres
        assertTrue(min(m, 360.0 - m) < 1e-6, "$m")
        val m2 = assertNotNull(LissageCap.moyenne(listOf(350.0, 10.0, 0.0))).degres
        assertTrue(min(m2, 360.0 - m2) < 1e-6, "$m2")
    }

    @Test fun `la moyenne de lectures voisines est entre elles`() {
        assertEquals(100.0, assertNotNull(LissageCap.moyenne(listOf(98.0, 100.0, 102.0))).degres, 1e-6)
    }

    @Test fun `des lectures opposees ne donnent pas de cap, on ne devine pas`() {
        assertNull(LissageCap.moyenne(listOf(0.0, 180.0)))
        assertNull(LissageCap.moyenne(emptyList()))
    }

    @Test fun `la fenetre ne garde que la derniere seconde`() {
        val f = FenetreCap()
        f.ajouter(0, 10.0)
        f.ajouter(500, 90.0)
        f.ajouter(1_400, 100.0)
        // À 1 400 ms, la lecture de 0 ms (1,4 s) est sortie ; 500 ms (0,9 s) reste.
        assertEquals(95.0, assertNotNull(f.moyenne(1_400)).degres, 1e-6)
        assertEquals(100.0, assertNotNull(f.moyenne(1_600)).degres, 1e-6, "à 1 600 ms, seule la dernière reste")
        assertNull(f.moyenne(5_000), "plus aucune lecture récente")
    }

    @Test fun `la fenetre videe n'a plus de cap`() {
        val f = FenetreCap()
        f.ajouter(0, 10.0)
        f.vider()
        assertNull(f.moyenne(0))
    }

    @Test fun `classe de precision - bonne, douteuse, inutilisable`() {
        assertEquals(ClasseBoussole.BONNE, ClasseBoussole.depuis(StatutCapteur.ELEVE, 0.05, 5.0))
        assertEquals(ClasseBoussole.BONNE, ClasseBoussole.depuis(StatutCapteur.MOYEN, null, null))
        assertEquals(ClasseBoussole.DOUTEUSE, ClasseBoussole.depuis(StatutCapteur.FAIBLE, 0.05, 5.0))
        assertEquals(ClasseBoussole.INUTILISABLE, ClasseBoussole.depuis(StatutCapteur.INUTILISABLE, 0.05, 5.0))
        // Téléphone trop incliné : « tenez-le à plat ».
        assertEquals(ClasseBoussole.DOUTEUSE, ClasseBoussole.depuis(StatutCapteur.ELEVE, 0.05, 45.0))
        // Incertitude annoncée trop grande ; -1 = pas d'incertitude annoncée, on ne la devine pas.
        assertEquals(ClasseBoussole.DOUTEUSE, ClasseBoussole.depuis(StatutCapteur.ELEVE, Math.toRadians(30.0), 5.0))
        assertEquals(ClasseBoussole.BONNE, ClasseBoussole.depuis(StatutCapteur.ELEVE, -1.0, 5.0))
    }

    @Test fun `les niveaux du capteur Android sont lus, un niveau inconnu est inutilisable`() {
        assertEquals(StatutCapteur.ELEVE, StatutCapteur.de(3))
        assertEquals(StatutCapteur.FAIBLE, StatutCapteur.de(1))
        assertEquals(StatutCapteur.INUTILISABLE, StatutCapteur.de(0))
        assertEquals(StatutCapteur.INUTILISABLE, StatutCapteur.de(42))
    }

    @Test fun `champ perturbe - hors de la plage autour de 45 microteslas`() {
        assertFalse(ChampMagnetique.perturbe(47.0))
        assertTrue(ChampMagnetique.perturbe(10.0))
        assertTrue(ChampMagnetique.perturbe(120.0))
        assertFalse(ChampMagnetique.perturbe(null), "pas de mesure : pas d'alerte inventée")
    }

    @Test fun `une lecture douteuse ou un champ perturbe avertit, sans rien bloquer`() {
        val bonne = LectureBoussole(CapMagnetique.de(10.0), 0.1, ClasseBoussole.BONNE, champPerturbe = false)
        assertFalse(bonne.avertir)
        assertTrue(bonne.copy(classe = ClasseBoussole.DOUTEUSE).avertir)
        assertTrue(bonne.copy(champPerturbe = true).avertir)
    }

    @Test fun `un cap douteux se pointe quand meme - on avertit, on ne bloque pas`() = with(BancControle()) {
        // Le pilote ne connaît pas la classe : un cap, même douteux, est pointé.
        val e = controleur().pointerAvecBoussole("3", CapMagnetique.de(200.0))
        assertEquals(VueControle.PLANT, e.vue)
    }

    // ------------------------------------------------------------------ garde-fous de source

    private val racine = File("src/main/kotlin").takeIf { it.isDirectory } ?: File("app/src/main/kotlin")
    private val sources = racine.walkTopDown().filter { it.extension == "kt" }.map { it.path to it.readText() }.toList()

    @Test fun `aucun fichier de l'application ne cite la classe du systeme qui calcule la declinaison`() {
        assertTrue(sources.size > 20, "${sources.size} fichiers")
        val citent = sources.filter { "GeomagneticField" in it.second }.map { it.first }
        assertTrue(citent.isEmpty(), "la déclinaison est ajoutée une seule fois, par la session : $citent")
    }

    @Test fun `aucun fontSize dans l'ecran du controle, les styles sont ceux de Marculus`() {
        val fautifs = sources.filter { it.first.contains("/controle/") && Regex("""\bfontSize\b""").containsMatchIn(it.second) }.map { it.first }
        assertTrue(fautifs.isEmpty(), "$fautifs")
    }

    @Test fun `la boussole ne connait pas la position, elle rend un CapMagnetique`() {
        val boussole = sources.first { it.first.endsWith("BoussoleTelephone.kt") }.second
        assertFalse("Position" in boussole.replace("PositionControle", ""), "la boussole du téléphone ignore la position")
        assertTrue("CapMagnetique" in boussole)
    }

    // ------------------------------------------------------------------ la moyenne de 5 s, écrite une seule fois

    @Test fun `la moyenne collecte une position par seconde pendant 5 secondes avec son decompte`() = runBlocking {
        val decompte = ArrayList<Int>()
        var n = 0
        val positions = collecterPositionsArrivee(
            position = { Position(latitude = 47.0 + 0.0001 * n++, longitude = 4.0) },
            surSeconde = { decompte += it },
            attendre = {},
        )
        assertEquals(5, SECONDES_DE_MOYENNE_ARRIVEE)
        assertEquals(listOf(0, 1, 2, 3, 4), decompte)
        assertEquals(5, positions.size)
    }

    @Test fun `sans fix, la moyenne ne ramasse rien`() = runBlocking {
        assertTrue(collecterPositionsArrivee(position = { null }, attendre = {}).isEmpty())
    }

    @Test fun `guidage et controle se servent de la meme moyenne, le decompte d'une seconde n'est ecrit qu'une fois`() {
        val guidage = sources.first { it.first.endsWith("EcranGuidage.kt") }.second
        val controle = sources.first { it.first.endsWith("EcranControle.kt") }.second
        assertTrue("collecterPositionsArrivee" in guidage)
        assertTrue("collecterPositionsArrivee" in controle)
        assertFalse("MoyennePosition()" in guidage, "le guidage ne moyenne plus lui-même")
        val decomptes = sources.filter { "delay(1000)" in it.second }.map { it.first.substringAfterLast('/') }
        assertEquals(listOf("MoyenneArrivee.kt"), decomptes)
    }
}
