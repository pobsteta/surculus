package io.github.pobsteta.surculus.core.placette

import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.reperes.RefusRepere
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 081 : aucun texte montré au forestier ne porte un identifiant snake_case, un UUID,
 * un nombre à point décimal ou un nom d'exception. Seul ce qui est AFFICHÉ (`message`) est
 * vérifié : les identifiants stables (`motif`) restent inchangés.
 */
class AucunTexteTechniqueTest {
    private val uuidA = UuidPlant("0190f3a2-7b1c-7d4e-8a9b-1c2d3e4f5a6b")
    private val uuidB = UuidPlant("0190f3a2-7b1c-7d4e-8a9b-aaaaaaaaaaaa")
    private val placette = UuidPlacette("0190f3a2-7b1c-7d4e-8a9b-bbbbbbbbbbbb")

    private val uuidRe = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val decimalRe = Regex("\\d\\.\\d")
    private val exceptionRe = Regex("(Exception|Error|java\\.|kotlin\\.)")

    private fun verifier(nom: String, texte: String) {
        assertTrue(texte.isNotBlank(), "$nom : texte vide")
        assertFalse('_' in texte, "$nom : « $texte » contient un « _ »")
        assertFalse(uuidRe.containsMatchIn(texte), "$nom : « $texte » contient un UUID")
        assertFalse(decimalRe.containsMatchIn(texte), "$nom : « $texte » contient un nombre à point décimal")
        assertFalse(exceptionRe.containsMatchIn(texte), "$nom : « $texte » contient un nom d'exception")
    }

    private fun tousLesRefus(): List<RefusPlacette> = buildList {
        add(RefusPlacette.EtatInconnu("vivant"))
        add(RefusPlacette.GroupeCauseInconnu("gel"))
        EtatDsf.entries.forEach { add(RefusPlacette.CauseSurEtat(it)); add(RefusPlacette.DetailSurEtat(it, "abroutissement")) }
        add(RefusPlacette.HauteurSurAbsent(40))
        add(RefusPlacette.PlantEnDoubleDansControle(uuidA))
        add(RefusPlacette.HauteurInvalide(-5))
        add(RefusPlacette.MesureSurNonObserve)
        add(RefusPlacette.EssenceInconnue("XYZ"))
        add(RefusPlacette.UuidEnDouble(uuidA))
        add(RefusPlacette.PlantDansDeuxPlacettes(uuidA))
        add(RefusPlacette.NumeroEnDouble(placette, 7))
        add(RefusPlacette.NumeroChange(uuidA, 3, 4))
        add(RefusPlacette.PlacetteChangee(uuidA))
        add(RefusPlacette.NumerosDejaFiges)
        add(RefusPlacette.RemplaceObligatoire)
        add(RefusPlacette.PlantRemplaceInconnu(uuidA))
        add(RefusPlacette.PlantRemplaceAutrePlacette(uuidA))
        add(RefusPlacette.PlantRemplaceNiMortNiAbsent(uuidA, null))
        EtatDsf.entries.forEach { add(RefusPlacette.PlantRemplaceNiMortNiAbsent(uuidA, it)) }
        add(RefusPlacette.PlantDejaRemplace(uuidA, uuidB))
        add(RefusPlacette.AucunePlacette)
        add(RefusPlacette.HorsQuinzeMetres(17.3))
        add(RefusPlacette.HorsQuinzeMetres(15.04))
        add(RefusPlacette.CodeInconnu("BOISEMENT"))
    }

    @Test fun `aucun refus de placette ne montre de texte technique`() {
        tousLesRefus().forEach { verifier(it::class.simpleName ?: "refus", it.message) }
    }

    @Test fun `un refus n'affiche jamais l'UUID d'un plant ou d'une placette`() {
        tousLesRefus().forEach {
            assertFalse(uuidA.valeur in it.message, "${it::class.simpleName} montre l'UUID du plant")
            assertFalse(uuidB.valeur in it.message, "${it::class.simpleName} montre l'UUID du plant remplaçant")
            assertFalse(placette.valeur in it.message, "${it::class.simpleName} montre l'UUID de la placette")
        }
    }

    @Test fun `le plant non observe est dit en francais, le motif stable ne change pas`() {
        assertFalse("non_observe" in RefusPlacette.MesureSurNonObserve.message)
        assertTrue(RefusPlacette.MesureSurNonObserve.motif.isNotBlank())
    }

    @Test fun `l'alerte hors rayon ne montre pas de texte technique`() {
        verifier("HorsRayon", AlertePlacette.HorsRayon(5.5234, 3.99, 0.10).message)
        verifier("HorsRayon rond", AlertePlacette.HorsRayon(6.0, 4.0, 0.1).message)
    }

    @Test fun `une distance refusee est dite sans decimale a point`() {
        listOf(-1.0, -0.5, Double.NaN, Double.POSITIVE_INFINITY, 1e12).forEach {
            verifier("DistanceInvalide($it)", RefusRepere.DistanceInvalide(it).message)
        }
    }

    @Test fun `les refus d'azimut sont sans identifiant technique ni exception`() {
        // Les décimales de l'azimut et de la pente : hors fiche, question posée à Pascal.
        listOf(
            RefusRepere.AzimutHorsPlage(400.0, UniteAzimut.DEGRES).message,
            RefusRepere.UniteAzimutNonDeclaree.message,
            RefusRepere.UnitePenteNonDeclaree.message,
        ).forEach {
            assertFalse('_' in it, it)
            assertFalse(uuidRe.containsMatchIn(it), it)
            assertFalse(exceptionRe.containsMatchIn(it), it)
        }
    }
}
