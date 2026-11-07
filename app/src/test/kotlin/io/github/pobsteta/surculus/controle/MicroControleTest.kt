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
 *
 * Le bloc d'écoute (`BlocEcouteEncadre`) : couché, il est posé par-dessus le haut de la zone qui défile (gardes ci-dessous, sur le
 * `Box` de zone). Debout (mission 083), il n'est plus dans la zone : il part du pied, à gauche du micro, et monte sur le bas de la
 * zone ; sa place est jugée par le test de rendu (`RenduControleTest`), pas ici.
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

    /**
     * Couché (083, reprise 2/3) : le bloc n'est plus dans le `Box` de zone mais frère de la `Row` (zone + colonne du micro), dans un `Box`
     * qui les contient : il s'étend vers la droite, sur la colonne du micro. L'appel debout (`BlocEcouteEncadre(cote = false)`) n'est pas compté.
     */
    @Test fun `le bloc J'ECOUTE est pose hors de la colonne qui defile`() {
        val defile = corps("Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp))")
        assertFalse(defile.contains("BlocEcouteEncadre("), "le bloc ne doit pas être dans la colonne qui défile")
        val zone = corps("Box(m) {")
        assertFalse(zone.contains("BlocEcouteEncadre("), "couché, le bloc n'est plus dans le Box de zone : il déborde à droite, sur la colonne du micro")
        // Surimpression : le bloc est un frère de la Row (zone + micro), APRÈS elle, posé avec Modifier.align(.
        val couche = corps("if (emplacement == EmplacementMicro.COLONNE) {")
        val ligneRow = couche.indexOf("Row(Modifier.fillMaxSize()) {")
        assertTrue(ligneRow >= 0, "la Row (zone + colonne du micro) doit être dans le Box couché")
        val ouvreRow = couche.indexOf('{', ligneRow)
        var prof = 0
        var finRow = -1
        for (k in ouvreRow until couche.length) {
            if (couche[k] == '{') prof++
            if (couche[k] == '}' && --prof == 0) { finRow = k; break }
        }
        assertTrue(finRow > 0, "accolade de la Row non fermée")
        val bloc = couche.indexOf("BlocEcouteEncadre()")
        assertTrue(bloc >= 0, "BlocEcouteEncadre() doit être dans le Box couché")
        assertEquals(1, Regex("BlocEcouteEncadre\\(\\)").findAll(couche).count(), "un seul appel couché")
        assertTrue(bloc > finRow, "le bloc doit venir APRÈS la Row (frère au-dessus), pas avant ni dedans")
        val ligne = couche.substring(couche.lastIndexOf('\n', bloc) + 1, couche.indexOf('\n', bloc))
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
