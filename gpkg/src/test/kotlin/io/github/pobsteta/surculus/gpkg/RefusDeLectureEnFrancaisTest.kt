package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.reperes.RefusRepere
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 084 (testeur, 2026-11-08) : les six textes de refus d'une tournée, composés avec leur préfixe d'écran
 * (« Tournée refusée : », « Illisible : », « Carte impossible : »), sont du français pour le forestier :
 * ni `_`, ni nom de table GeoPackage, ni UUID, ni nombre à plus de deux décimales (ni coordonnée brute), pas de
 * majuscule juste après « : », et une action dite.
 *
 * Critère d'« action dite » : la phrase nomme à qui s'adresser ou le geste à refaire, par l'un de ces mots :
 * sommieR (l'outil qui prépare la tournée), « préparé/préparer/préparée » (celui qui l'a préparée), « demander »,
 * « exporter/réexporter », « refaire », « recommencer », « contacter ». Un constat seul (« Aucun référentiel déclaré »)
 * ne passe pas : le forestier ne sait pas quoi faire.
 */
class RefusDeLectureEnFrancaisTest {
    private val uuid = "3f2b8c1e-9a4d-4e57-b6a0-1c2d3e4f5a6b"

    /** Les préfixes sont lus dans les ressources de l'écran : un changement de préfixe est suivi sans retoucher le test. */
    private val prefixes: List<String> by lazy {
        val xml = listOf("../app/src/main/res/values/strings_tournees.xml", "app/src/main/res/values/strings_tournees.xml")
            .map(::File).firstOrNull { it.exists() }
        assertNotNull(xml, "strings_tournees.xml introuvable depuis ${File(".").absoluteFile}")
        val texte = xml.readText()
        listOf("tournees_import_refus", "tournees_illisible", "carte_refus").map { nom ->
            val m = Regex("""<string name="$nom">([^<]*?)%1\${'$'}s</string>""").find(texte)
            assertNotNull(m, "chaîne $nom introuvable")
            m.groupValues[1]
        }.also { assertEquals(listOf("Tournée refusée : ", "Illisible : ", "Carte impossible : "), it) }
    }

    private val uuidRe = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val troisDecimalesRe = Regex("""\d+[.,]\d{3,}""")
    private val grandNombreRe = Regex("""\d{5,}""")
    private val majusculeApresDeuxPointsRe = Regex(""":\s*\p{Lu}""")
    private val actionRe = Regex("sommieR|préparé|préparer|demand|export|refai|recommenc|contact", RegexOption.IGNORE_CASE)

    private fun verifie(nom: String, refus: RefusGpkg, interdits: List<String> = emptyList()) {
        for (prefixe in prefixes) {
            val t = prefixe + refus.message
            val ou = "[$nom] « $t »"
            assertFalse('_' in t, "$ou contient « _ »")
            assertFalse(Regex("gpkg", RegexOption.IGNORE_CASE).containsMatchIn(t), "$ou nomme une table ou un format GeoPackage (gpkg…)")
            assertFalse("srs" in t.lowercase() || "spatial_ref" in t.lowercase(), "$ou nomme srs_id ou une table de références spatiales")
            assertFalse(uuidRe.containsMatchIn(t), "$ou contient un UUID")
            assertFalse(troisDecimalesRe.containsMatchIn(t), "$ou contient un nombre à plus de deux décimales")
            assertFalse(grandNombreRe.containsMatchIn(t), "$ou contient un nombre de 5 chiffres ou plus (coordonnée brute)")
            assertFalse(majusculeApresDeuxPointsRe.containsMatchIn(t), "$ou met une majuscule après « : »")
            assertTrue(actionRe.containsMatchIn(refus.message), "$ou ne dit pas quoi faire (qui contacter ou quoi refaire)")
            for (i in interdits) assertFalse(i in t, "$ou montre « $i »")
        }
    }

    @Test fun `un srs_id inconnu est dit en francais avec une action`() =
        verifie("ReferentielSpatialInconnu", RefusGpkg.ReferentielSpatialInconnu(3857))

    @Test fun `un fichier qui mele 2154 et 9794 est dit en francais avec une action`() =
        verifie("ReferentielsMeles", RefusGpkg.Referentiel(RefusRepere.ReferentielsMeles(setOf(2154, 9794))))

    @Test fun `un referentiel non pris en charge (4326) est dit en francais avec une action`() =
        verifie("ReferentielNonPrisEnCharge", RefusGpkg.Referentiel(RefusRepere.ReferentielNonPrisEnCharge(setOf(4326))))

    @Test fun `un fichier sans referentiel dit quoi faire`() =
        verifie("AucunReferentiel", RefusGpkg.Referentiel(RefusRepere.AucunReferentiel))

    @Test fun `une couche absente est dite sans majuscule apres le prefixe`() =
        verifie("CoucheAbsente", RefusGpkg.CoucheAbsente("photos"))

    @Test fun `une placette de France aux axes inverses est refusee sans UUID ni coordonnee brute`() =
        // Lambert-93 d'une placette française (x = Est 846164,7654321 ; y = Nord 6686123,1234567), saisie inversée.
        verifie(
            "PointHorsFrance (axes inversés)",
            RefusGpkg.PointHorsFrance("placettes", uuid, 6_686_123.1234567, 846_164.7654321, axesInverses = true),
            interdits = listOf(uuid, "6686123", "6 686 123", "846164", "846 164", "1234567", "7654321"),
        )

    @Test fun `la placette en Somalie (hors de France, degres pris pour des metres) est refusee sans UUID ni coordonnee brute`() =
        // Somalie : latitude 5,1234567 ; longitude 46,1234567. Saisie x = longitude, y = latitude.
        verifie(
            "PointHorsFrance (Somalie)",
            RefusGpkg.PointHorsFrance("placettes", uuid, 46.1234567, 5.1234567, axesInverses = false),
            interdits = listOf(uuid, "46.1234567", "5.1234567", "1234567"),
        )

    @Test fun `les motifs restent les codes stables`() {
        assertEquals("referentiel_spatial_inconnu", RefusGpkg.ReferentielSpatialInconnu(3857).motif)
        assertEquals("melange_reperes", RefusGpkg.Referentiel(RefusRepere.ReferentielsMeles(setOf(2154, 9794))).motif)
        assertEquals("referentiel_non_pris_en_charge", RefusGpkg.Referentiel(RefusRepere.ReferentielNonPrisEnCharge(setOf(4326))).motif)
        assertEquals("aucun_referentiel", RefusGpkg.Referentiel(RefusRepere.AucunReferentiel).motif)
        assertEquals("couche_absente", RefusGpkg.CoucheAbsente("photos").motif)
        assertEquals("hors_france", RefusGpkg.PointHorsFrance("placettes", uuid, 1.0, 2.0, false).motif)
    }
}
