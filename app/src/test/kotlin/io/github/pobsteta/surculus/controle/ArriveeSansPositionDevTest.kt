package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.session.ValeurControle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Mission 087, tests du `dev` (en plus de ceux du `testeur`) : un fix reçu l'emporte toujours sur l'attente ; les plants
 * pointés de la confirmation comptent ceux du journal ; un fix reçu après l'ouverture sans position ne change rien.
 *
 * Coordonnées : Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres ; fix en (latitude, longitude) degrés WGS 84 ; P35-01 à
 * Couchey (Côte-d'Or), en France.
 */
class ArriveeSansPositionDevTest {
    @Test
    fun `un fix recu l'emporte, meme localisation coupee - c'est une position (recepteur RTK branche)`() {
        assertEquals(SuiteSansFix.POSITION_RECUE, ArriveeSansPosition.suite(0, 1, localisationActive = false, permissionAccordee = false))
    }

    @Test
    fun `la confirmation compte aussi les plants pointes au journal, sans doublon`() = with(BancControle()) {
        // Un premier contrôle avec position, qui pointe un plant de plus que les 12 du passage précédent.
        val c = assertIs<EtatArrivee.Ouverte>(ArriveeControle.ouvrir(contexte, journal, horloge, essences, positionsAuPiquet(), null, sauvegarde)).controleur
        c.dire(EtatDsf.SAIN)
        val avant = journal.nombreDeLignes
        val e = assertIs<EtatArrivee.ConfirmerSansPosition>(ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, essences, " P35-01 ", sauvegarde))
        assertEquals(12, e.plantsPointes, "les 12 du passage précédent, le journal n'en ajoute pas")
        assertEquals(avant, journal.nombreDeLignes, "confirmer n'écrit rien")
    }

    @Test
    fun `le texte de confirmation accorde le pluriel et n'a ni UUID ni underscore`() {
        val t = TextesControle.confirmationSansPosition(io.github.pobsteta.surculus.core.placette.Piquet(BancControle.P1, "P35-01", BancControle.POSITION_P1, ug = " "), 1)
        assertEquals("P35-01 · 1 plant pointé : ouvrir sans position ?", t)
        assertFalse("_" in TextesControle.POSITION_ABERRANTE + TextesControle.ACTIVER_LOCALISATION + TextesControle.CODE_REQUIS)
    }

    @Test
    fun `ouvert sans position, le controle reste sans position mesuree a la fin`() = with(BancControle()) {
        val c = assertIs<EtatArrivee.Ouverte>(
            ArriveeControle.ouvrirSansPosition(contexte, journal, horloge, Essences(emptySet()), "P35-01", sauvegarde, confirmee = true),
        ).controleur
        c.dire(EtatDsf.SAIN)
        c.terminer()
        val v = journal.lignesCourantes().filter { it.type == "controle" }.map { ValeurControle.lire(it.valeur)!! }
        assertTrue(v.isNotEmpty() && v.all { it.sansPositionMesuree && it.positionMesureeX == null && it.rattacheParIdentite })
    }
}
