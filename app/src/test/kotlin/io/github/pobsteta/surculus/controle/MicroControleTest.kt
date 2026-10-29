package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.carte.CONTRASTE_MINIMAL
import io.github.pobsteta.surculus.carte.CONTRASTE_PASTILLE
import io.github.pobsteta.surculus.carte.contraste
import io.github.pobsteta.surculus.ui.theme.SchemaClair
import io.github.pobsteta.surculus.ui.theme.SchemaSombre
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Mission 075 : le bouton micro comme celui de Marculus. Sans Compose : la place (fonction pure de l'écran, jamais de l'écoute),
 * l'aspect (fonction pure de l'écoute et de « prêt ») et les contrastes. « Il ne bouge pas sous le doigt » et « visible sans défiler »
 * ne se prouvent qu'à la capture.
 */
class MicroControleTest {
    @Test fun `couche, le micro est dans une colonne fixe de 128 dp a droite`() {
        assertEquals(EmplacementMicro.COLONNE, DispositionMicro.emplacement(largeurDp = 823f, hauteurDp = 384f))
        assertEquals(128f, DimensionsMicro.ZONE.value)
    }

    @Test fun `debout, le micro est dans un pied fixe sous la zone qui defile`() {
        assertEquals(EmplacementMicro.PIED, DispositionMicro.emplacement(largeurDp = 360f, hauteurDp = 800f))
        assertEquals(EmplacementMicro.PIED, DispositionMicro.emplacement(largeurDp = 500f, hauteurDp = 500f))
    }

    @Test fun `la place du micro ne depend que de l'ecran, jamais de l'ecoute`() {
        // emplacement() n'a que la largeur et la hauteur : l'apparition du bloc ne peut pas déplacer le micro.
        assertEquals(EmplacementMicro.PIED, DispositionMicro.emplacement(360f, 800f))
    }

    // --- Gardes de source (sans Compose : la composition ne se teste qu'à la capture) ---

    private val source = java.io.File("src/main/kotlin/io/github/pobsteta/surculus/controle/EcranControle.kt").readText()

    /** Corps (entre accolades) de ce qui suit [debut] dans la source, accolades appariées. */
    private fun corps(debut: String): String {
        val i = source.indexOf(debut)
        assertTrue(i >= 0, "introuvable : $debut")
        val ouvre = source.indexOf('{', i + debut.length - 1)
        var prof = 0
        for (k in ouvre until source.length) {
            if (source[k] == '{') prof++
            if (source[k] == '}' && --prof == 0) return source.substring(ouvre, k + 1)
        }
        error("accolade non fermée : $debut")
    }

    @Test fun `l'etat du micro est calcule dans BoutonMicro, apres la lecture de EcouteApp version`() {
        val bouton = corps("private fun BoutonMicro(")
        val lecture = bouton.indexOf("EcouteApp.version")
        val calcul = bouton.indexOf("AspectMicro.pour(EcouteApp.affichage")
        assertTrue(lecture >= 0, "BoutonMicro doit lire EcouteApp.version (état Compose)")
        assertTrue(calcul > lecture, "AspectMicro.pour(EcouteApp.affichage, …) doit venir APRÈS la lecture de EcouteApp.version")
        assertEquals(1, Regex("AspectMicro\\.pour\\(").findAll(source).count(), "un seul calcul de l'aspect, dans BoutonMicro")
    }

    @Test fun `le bloc J'ECOUTE est pose hors de la colonne qui defile`() {
        val defile = corps("Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp))")
        assertFalse(defile.contains("BlocEcouteEncadre()"), "le bloc ne doit pas être dans la colonne qui défile")
        // Surimpression : le bloc est un frère de la Column, dans le corps du Box de zone, APRÈS elle, posé avec Modifier.align(.
        val zone = corps("Box(m) {")
        val debutColonne = zone.indexOf("Column(Modifier.fillMaxSize().verticalScroll(")
        assertTrue(debutColonne >= 0, "la Column qui défile doit être dans le Box de zone")
        val ouvreColonne = zone.indexOf('{', zone.indexOf("verticalArrangement", debutColonne))
        var prof = 0
        var finColonne = -1
        for (k in ouvreColonne until zone.length) {
            if (zone[k] == '{') prof++
            if (zone[k] == '}' && --prof == 0) { finColonne = k; break }
        }
        assertTrue(finColonne > 0, "accolade de la Column non fermée")
        val bloc = zone.indexOf("BlocEcouteEncadre()")
        assertTrue(bloc >= 0, "BlocEcouteEncadre() doit être dans le corps du Box de zone")
        assertEquals(1, Regex("BlocEcouteEncadre\\(\\)").findAll(zone).count(), "un seul appel dans la zone")
        assertTrue(bloc > finColonne, "le bloc doit venir APRÈS la Column (frère au-dessus), pas avant ni dedans")
        val ligne = zone.substring(zone.lastIndexOf('\n', bloc) + 1, zone.indexOf('\n', bloc))
        assertTrue(ligne.contains("Modifier.align("), "le bloc est posé avec Modifier.align( : $ligne")
    }

    @Test fun `la zone qui defile ne change pas de place dans la composition entre Plant et Pointer`() {
        assertFalse(source.contains("!avecMicro ->"), "zone() ne doit pas dépendre d'un when sur avecMicro (le contenu serait recréé)")
    }

    @Test fun `le micro fait 112 dp, son icone 48 dp, et la zone le contient`() {
        assertEquals(112f, DimensionsMicro.BOUTON.value)
        assertEquals(48f, DimensionsMicro.ICONE.value)
        assertTrue(DimensionsMicro.ZONE.value >= DimensionsMicro.BOUTON.value)
        assertTrue(DimensionsMicro.BOUTON.value >= DimensionsControle.CIBLE_MIN.value)
    }

    @Test fun `l'aspect - pret, ecoute, barre`() {
        assertEquals(EtatMicro.PRET, AspectMicro.pour(AffichageEcoute.RIEN, pret = true))
        assertEquals(EtatMicro.PRET, AspectMicro.pour(AffichageEcoute.FERMEE, pret = true))
        assertEquals(EtatMicro.ECOUTE, AspectMicro.pour(AffichageEcoute.OUVERTURE, pret = true))
        assertEquals(EtatMicro.ECOUTE, AspectMicro.pour(AffichageEcoute.ECOUTE, pret = true))
        AffichageEcoute.entries.forEach { assertEquals(EtatMicro.BARRE, AspectMicro.pour(it, pret = false), "$it") }
    }

    @Test fun `pret seulement si le micro est accorde et le modele installe`() {
        assertTrue(AspectMicro.pret(EtatPermissionMicro.ACCORDEE, modeleInstalle = true))
        assertFalse(AspectMicro.pret(EtatPermissionMicro.ACCORDEE, modeleInstalle = false))
        EtatPermissionMicro.entries.filter { it != EtatPermissionMicro.ACCORDEE }.forEach {
            assertFalse(AspectMicro.pret(it, modeleInstalle = true), "$it")
        }
    }

    @Test fun `les trois etats se distinguent par la couleur, l'ecoute est en rouge d'erreur`() {
        listOf(SchemaClair, SchemaSombre).forEach { s ->
            assertEquals(s.error, CouleursMicro.fond(EtatMicro.ECOUTE, s))
            assertEquals(s.onError, CouleursMicro.icone(EtatMicro.ECOUTE, s))
            assertEquals(s.primaryContainer, CouleursMicro.fond(EtatMicro.PRET, s))
            assertEquals(s.surfaceVariant, CouleursMicro.fond(EtatMicro.BARRE, s))
            assertNotEquals(CouleursMicro.fond(EtatMicro.PRET, s), CouleursMicro.fond(EtatMicro.ECOUTE, s))
            assertNotEquals(CouleursMicro.fond(EtatMicro.PRET, s), CouleursMicro.fond(EtatMicro.BARRE, s))
        }
    }

    @Test fun `l'icone se lit sur son fond dans les trois etats, en clair et en sombre`() {
        listOf(SchemaClair, SchemaSombre).forEach { s ->
            EtatMicro.entries.forEach { e ->
                val c = contraste(CouleursMicro.icone(e, s), CouleursMicro.fond(e, s))
                assertTrue(c >= CONTRASTE_MINIMAL, "$e, sombre=${s === SchemaSombre} : $c")
            }
        }
    }

    @Test fun `le filet du bouton se voit sur le fond de l'ecran, en clair et en sombre`() {
        listOf(SchemaClair, SchemaSombre).forEach { s ->
            val c = contraste(CouleursMicro.filet(s), s.surface)
            assertTrue(c >= CONTRASTE_PASTILLE, "filet : $c")
        }
    }
}
