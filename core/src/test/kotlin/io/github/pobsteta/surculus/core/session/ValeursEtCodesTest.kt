package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.placette.CodesRefus
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.JourCivil
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.reperes.RefusRepere
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Format de `valeur` (docs/formats/journal-valeurs-v1.md), codec JSON, table des codes de refus. */
class ValeursEtCodesTest {

    @Test
    fun `le JSON rend les memes textes et relit ce qu'il ecrit, echappements compris`() {
        val m = linkedMapOf<String, Any?>(
            "a" to "l'\"agent\"\n\\ é", "b" to 1.0e-5, "c" to 12L, "d" to true, "e" to null, "f" to linkedMapOf<String, Any?>("x" to 1L),
        )
        val texte = Json.ecrire(m)
        assertEquals(texte, Json.ecrire(Json.lire(texte)!!), "même saisie, même texte, caractère pour caractère")
        assertEquals("l'\"agent\"\n\\ é", Json.lire(texte)!!["a"])
        assertEquals(1.0e-5, Json.lire(texte)!!["b"])
    }

    @Test
    fun `un texte qui n'est pas un objet JSON est illisible, jamais devine`() {
        assertNull(Json.lire(null))
        assertNull(Json.lire(""))
        assertNull(Json.lire("[1]"))
        assertNull(Json.lire("{\"a\":1} suite"))
        assertNull(Json.lire("{\"a\":"))
        assertNull(ValeurControle.lire("{\"v\":1}"))
        assertNull(ValeurPointage.lire("pas du json"))
        assertNull(ValeurEtat.lire("{}"))
    }

    @Test
    fun `un nombre non fini n'est pas ecrit`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { Json.ecrire(mapOf("x" to Double.NaN)) }
    }

    @Test
    fun `valeur controle aller-retour, avec et sans numeros`() {
        val v = ValeurControle("p", "P35-01", "2026-10-27", "Agent", false, 1_790_000_000_000, 846164.76, 6686123.26, 846165.0, 6686124.0, true, 2154)
        assertEquals(v, ValeurControle.lire(v.versJson()))
        val t = v.copy(termine = true, numeros = mapOf("u1" to 1, "u2" to 2))
        assertEquals(t, ValeurControle.lire(t.versJson()))
    }

    @Test
    fun `valeur pointage aller-retour garde l'observation brute et les termes derives`() {
        val v = ValeurPointage(
            "c", "p", 2.0, 250.0, "gr", "saisie", 1.7, 0.4, 105.9, 105.5, "2026-10-27", 846166.1, 6686122.0, 2154, "CHS", "m1", true, 1_790_000_000_000,
        )
        assertEquals(v, ValeurPointage.lire(v.versJson()))
        assertEquals(v.copy(essence = null, remplaceUuid = null), ValeurPointage.lire(v.copy(essence = null, remplaceUuid = null).versJson()))
    }

    @Test
    fun `valeur etat aller-retour`() {
        val v = ValeurEtat("c", "u", "atteint", "animaux", "abroutissement", 35)
        assertEquals(v, ValeurEtat.lire(v.versJson()))
        val n = ValeurEtat("c", "u", "non_observe", null, null, null)
        assertEquals(n, ValeurEtat.lire(n.versJson()))
    }

    @Test
    fun `les types de ligne du format sont ceux de la liste fermee de la spec 5`() {
        assertEquals(setOf("controle", "pointage", "etat", "annulation"), setOf(
            FormatValeurs.TYPE_CONTROLE, FormatValeurs.TYPE_POINTAGE, FormatValeurs.TYPE_ETAT, FormatValeurs.TYPE_ANNULATION,
        ))
    }

    @Test
    fun `placettes controlees ignore les valeurs illisibles et les controles non termines`() {
        val ouvert = ValeurControle("p1", "P1", "2026-10-27", "A", false, 1, 0.0, 0.0, 0.0, 0.0, false, 2154)
        val termine = ouvert.copy(placetteUuid = "p2", termine = true)
        val lignes = listOf(
            LigneCourante("l1", "c1", "controle", ouvert.versJson(), null),
            LigneCourante("l2", "c2", "controle", termine.versJson(), "l0"),
            LigneCourante("l3", "c3", "controle", "illisible", null),
            LigneCourante("l4", "u", "etat", ouvert.copy(termine = true).versJson(), null),
        )
        assertEquals(setOf("p2"), PlacettesControlees.de(lignes))
    }

    // ------------------------------------------------------------ table des codes de refus

    /** Une instance de chacun des cas de [RefusPlacette] : la table doit répondre pour chacun. */
    private val tousLesRefus: List<RefusPlacette> = listOf(
        RefusPlacette.EtatInconnu("x"), RefusPlacette.GroupeCauseInconnu("x"), RefusPlacette.CauseSurEtat(EtatDsf.SAIN),
        RefusPlacette.DetailSurEtat(EtatDsf.SAIN, "x"), RefusPlacette.HauteurSurAbsent(3), RefusPlacette.PlantEnDoubleDansControle(UuidPlant("u")),
        RefusPlacette.HauteurInvalide(0), RefusPlacette.MesureSurNonObserve, RefusPlacette.EssenceInconnue("ZZZ"),
        RefusPlacette.UuidEnDouble(UuidPlant("u")), RefusPlacette.PlantDansDeuxPlacettes(UuidPlant("u")),
        RefusPlacette.NumeroEnDouble(UuidPlacette("p"), 1), RefusPlacette.NumeroChange(UuidPlant("u"), 1, 2),
        RefusPlacette.PlacetteChangee(UuidPlant("u")), RefusPlacette.NumerosDejaFiges, RefusPlacette.RemplaceObligatoire,
        RefusPlacette.PlantRemplaceInconnu(UuidPlant("u")), RefusPlacette.PlantRemplaceAutrePlacette(UuidPlant("u")),
        RefusPlacette.PlantRemplaceNiMortNiAbsent(UuidPlant("u"), null), RefusPlacette.PlantDejaRemplace(UuidPlant("u"), UuidPlant("v")),
        RefusPlacette.AucunePlacette, RefusPlacette.HorsQuinzeMetres(16.0), RefusPlacette.CodeInconnu("x"),
    )

    @Test
    fun `chaque refus du domaine a son code de la spec 6`() {
        assertEquals(23, tousLesRefus.size)
        val attendu = mapOf(
            "etat_inconnu" to "hors_referentiel", "cause_inconnue" to "hors_referentiel", "essence_inconnue" to "hors_referentiel",
            "plant_en_double_dans_controle" to "uuid_en_double", "placette_changee" to "uuid_en_double", "uuid_en_double" to "uuid_en_double",
        )
        for (r in tousLesRefus) {
            assertEquals(attendu[r.motif] ?: r.motif, CodesRefus.code(r), "code de ${r.motif}")
        }
    }

    @Test
    fun `hors referentiel, uuid en double et hors France sont les trois codes de l'export`() {
        assertEquals("hors_referentiel", CodesRefus.code(RefusPlacette.EtatInconnu("vivant")))
        assertEquals("uuid_en_double", CodesRefus.code(RefusPlacette.UuidEnDouble(UuidPlant("u"))))
        assertEquals("hors_france", CodesRefus.code(RefusRepere.PiquetHorsFrance(6_686_123.0, 846_164.0)))
        assertEquals("date_hors_validite", CodesRefus.code(RefusRepere.DateHorsValidite(LocalDate.of(2031, 1, 1), LocalDate.of(2025, 1, 1), LocalDate.of(2029, 12, 31))))
    }

    @Test
    fun `le jour civil d'une saisie suit la regle du fix de 5 secondes, une seule regle pour tout le code`() {
        val phone = java.time.Instant.parse("2026-10-27T09:00:00Z").toEpochMilli()
        val gnss = java.time.Instant.parse("2026-10-20T09:00:00Z").toEpochMilli()
        assertEquals(LocalDate.of(2026, 10, 20), JourCivil.deSaisie(phone, gnss, 0))
        assertEquals(LocalDate.of(2026, 10, 20), JourCivil.deSaisie(phone, gnss, 5_000))
        assertEquals(LocalDate.of(2026, 10, 27), JourCivil.deSaisie(phone, gnss, 5_001))
        assertEquals(LocalDate.of(2026, 10, 27), JourCivil.deSaisie(phone, gnss, -1))
        assertEquals(LocalDate.of(2026, 10, 27), JourCivil.deSaisie(phone, gnss, null))
        assertEquals(LocalDate.of(2026, 10, 27), JourCivil.deSaisie(phone, null, 100))
    }
}
