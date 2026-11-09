package io.github.pobsteta.surculus.messages

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onRoot
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.carte.EcranGuidage
import io.github.pobsteta.surculus.controle.EcranControle
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.gpkg.Geometrie
import io.github.pobsteta.surculus.gpkg.GeometrieGpkg
import io.github.pobsteta.surculus.gpkg.Sqlite
import io.github.pobsteta.surculus.rendu.BancRendu
import io.github.pobsteta.surculus.tournee.DepotTournees
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 103 : le refus « Carte impossible : … » du guidage et du contrôle, quand la tournée est chargée mais que la placette
 * demandée n'y est pas (ou, au guidage, n'a pas de position), est une phrase française sans UUID, sans majuscule après « : »,
 * qui dit quoi faire (comme les refus des missions 082 et 084). Écrit par le `testeur` depuis la fiche 103, la spec et les
 * fiches 082 et 084, **sans lire** le corps de `EcranGuidage.kt` ni de `EcranControle.kt` (seulement leurs signatures).
 *
 * Aucune phrase exacte n'est figée : Pascal la tranche. Les tests jugent le texte **affiché**, préfixe (`carte_refus`) compris,
 * lu dans l'arbre sémantique de l'écran réel ouvert sur une vraie tournée d'essai (modèle de sommieR 0.31.1 de `:gpkg`, trois
 * placettes de Couchey en Lambert-93, x = Est, y = Nord, mètres ; une quatrième sans position).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = BancRendu.DEBOUT_PLEIN)
class PlacetteIntrouvableEnFrancaisTest {
    val regle = BancRendu.nouvelleRegle()

    @get:Rule val chaine: org.junit.rules.RuleChain = org.junit.rules.RuleChain.outerRule(BancRendu.surveillant()).around(regle)

    @After fun remettre() = BancRendu.fermer()

    private companion object {
        const val TOURNEE = "essai-103"
        const val TRAVAUX_ID = "11111111-1111-4111-8111-111111111111"

        /** Une placette qui n'est pas dans la tournée. */
        const val ABSENTE = "3f2a9c1e-7b44-4d0a-9e5f-0123456789ab"
        const val ABSENTE_MAJUSCULES = "3F2A9C1E-7B44-4D0A-9E5F-0123456789AB"
        const val SANS_POSITION = "00000000-0000-4000-8000-000000000199"
        fun presente(k: Int) = "00000000-0000-4000-8000-%012x".format(100 + k)
        val UUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        val CENTRES = listOf(4.93250 to 47.25910, 4.93300 to 47.25940, 4.93350 to 47.25900)
    }

    // ------------------------------------------------------------------------------------------ la tournée d'essai

    private fun poserLaTournee() {
        val dossier = File(DepotTournees.racine(RuntimeEnvironment.getApplication()), TOURNEE).also { it.mkdirs() }
        val modele = listOf(
            File("../gpkg/src/test/resources/modele/travaux-sommier-0.31.1.gpkg"),
            File("gpkg/src/test/resources/modele/travaux-sommier-0.31.1.gpkg"),
        ).first { it.isFile }
        val gpkg = File(dossier, "travaux.gpkg")
        Files.copy(modele.toPath(), gpkg.toPath(), StandardCopyOption.REPLACE_EXISTING)
        val c = CENTRES.map { (lon, lat) -> Projections.versLambert93(lon = lon, lat = lat).let { PointLambert93(it.first, it.second) } }
        Sqlite.ouvrir(gpkg.toPath()).use { cnx ->
            val x0 = c.minOf { it.xMetres } - 300
            val x1 = c.maxOf { it.xMetres } + 300
            val y0 = c.minOf { it.yMetres } - 300
            val y1 = c.maxOf { it.yMetres } + 300
            val anneau = listOf(x0 to y0, x1 to y0, x1 to y1, x0 to y1, x0 to y0).map { PointLambert93(it.first, it.second) }
            val multi = GeometrieGpkg.encoder(Geometrie.MultiPolygone(listOf(Geometrie.Polygone(listOf(anneau)))), 2154)
            cnx.prepareStatement("INSERT INTO foret (geom, nom) VALUES (?, 'Forêt de Couchey (essai)')").use { it.setBytes(1, multi); it.executeUpdate() }
            cnx.prepareStatement("INSERT INTO ug (geom, numero) VALUES (?, '35')").use { it.setBytes(1, multi); it.executeUpdate() }
            cnx.createStatement().use { st ->
                st.executeUpdate("INSERT INTO suivis (id, libelle, ug) VALUES ('$TRAVAUX_ID', 'Plantation 2022', '35')")
                st.executeUpdate("INSERT INTO codes (code, libelle, unites, formes) VALUES ('PL', 'Plantation', 'ha', 'surface')")
            }
            fun placette(uuid: String, code: String, geom: ByteArray?) =
                cnx.prepareStatement(
                    "INSERT INTO placettes (geom, uuid, code_placette, travaux_id, rayon_m, materialisation, ug) VALUES (?, ?, ?, ?, 3.99, 'piquet bois', '35')",
                ).use { ps ->
                    if (geom != null) ps.setBytes(1, geom) else ps.setNull(1, java.sql.Types.BLOB)
                    ps.setString(2, uuid)
                    ps.setString(3, code)
                    ps.setString(4, TRAVAUX_ID)
                    ps.executeUpdate()
                }
            for (k in 0..2) placette(presente(k), "P35-%02d".format(k + 1), GeometrieGpkg.encoder(Geometrie.Point(c[k]), 2154))
            placette(SANS_POSITION, "P35-04", null)
        }
    }

    // ------------------------------------------------------------------------------------------ lire le texte affiché

    private fun textes(n: SemanticsNode, sortie: MutableList<String>) {
        n.config.getOrNull(SemanticsProperties.Text)?.let { sortie += it.joinToString("") { t -> t.text } }
        n.children.forEach { textes(it, sortie) }
    }

    private fun toutLeTexte(): List<String> {
        regle.waitForIdle()
        return ArrayList<String>().also { textes(regle.onRoot(useUnmergedTree = true).fetchSemanticsNode(), it) }
    }

    /** Le préfixe du refus tel que l'application l'affiche (ressource `carte_refus`, sans son argument). */
    private val prefixe: String get() = RuntimeEnvironment.getApplication().getString(R.string.carte_refus, "").trim()

    /**
     * Ouvre [ecran] sur la tournée d'essai et rend le texte du refus affiché, **préfixe compris** (le texte de l'arbre qui
     * commence par « Carte impossible : »). Attend jusqu'à 20 s que le chargement de la tournée soit fini.
     */
    private fun refusAffiche(ecran: @Composable () -> Unit): String {
        poserLaTournee()
        var trouve: String? = null
        BancRendu.rendre(
            contenu = ecran,
            apres = {
                val limite = System.nanoTime() + 20_000_000_000L
                while (trouve == null && System.nanoTime() < limite) {
                    trouve = toutLeTexte().firstOrNull { it.startsWith(prefixe) }
                    if (trouve == null) Thread.sleep(50)
                }
            },
            regle = regle,
        )
        return assertNotNull(trouve, "aucun texte « $prefixe… » en 20 s ; textes à l'écran : ${toutLeTexte()}")
    }

    private fun guidage(uuid: String) = refusAffiche { EcranGuidage(id = TOURNEE, placetteUuid = uuid, onRetour = {}) }
    private fun controle(uuid: String) = refusAffiche { EcranControle(id = TOURNEE, placetteUuid = uuid, onRetour = {}) }

    // ------------------------------------------------------------------------------------------ les critères

    /** Critère « dit quoi faire » des missions 082 et 084 : après le premier « : », un verbe à l'infinitif. */
    private fun ditQuoiFaire(texte: String): Boolean {
        val apres = texte.substringAfter(':', "")
        val horsVerbes = setOf("autre", "entre", "contre", "notre", "votre", "outre", "ordre", "être", "mettre", "chaque")
        return Regex("\\p{L}{3,}").findAll(apres).map { it.value.lowercase() }.any { m ->
            (m.endsWith("er") || m.endsWith("ir") || m.endsWith("re")) && !m.endsWith("ier") && m !in horsVerbes
        }
    }

    /** Les mots à majuscule initiale qui suivent un « : » (un sigle comme « GPS » reste permis, comme à la 084). */
    private fun majusculesApresDeuxPoints(texte: String): List<String> =
        Regex(":\\s*(\\p{Lu}\\p{Ll}\\p{L}*)").findAll(texte).map { it.groupValues[1] }.toList()

    /** Tous les défauts du texte (vide = propre) : un échec les nomme tous, pour voir chaque critère rouge séparément. */
    private fun defauts(texte: String, uuidDemande: String): List<String> = buildList {
        if (!texte.startsWith(prefixe)) add("ne commence pas par le préfixe « $prefixe »")
        if (UUID.containsMatchIn(texte)) add("montre un UUID")
        if (uuidDemande.lowercase() in texte.lowercase()) add("montre l'UUID demandé")
        if (uuidDemande.substringBefore('-').lowercase() in texte.lowercase()) add("montre un bout de l'UUID demandé")
        majusculesApresDeuxPoints(texte).forEach { add("majuscule après « : » (« $it »)") }
        if (!ditQuoiFaire(texte)) add("ne dit pas quoi faire (verbe à l'infinitif attendu après « : » : revenir, choisir une autre placette, demander à celui qui a préparé la tournée)")
        if ('_' in texte) add("garde un « _ »")
    }

    private fun assertEnFrancaisSansUuid(ou: String, texte: String, uuidDemande: String) {
        val d = defauts(texte, uuidDemande)
        assertTrue(d.isEmpty(), "$ou : « $texte » a ${d.size} défaut(s) : $d")
    }

    // ------------------------------------------------------------------------------------------ les critères eux-mêmes

    @Test fun `les criteres distinguent le refus d'aujourd'hui d'une phrase attendue`() {
        val avant = "Carte impossible : placette $ABSENTE"
        assertTrue(UUID.containsMatchIn(avant))
        assertFalse(ditQuoiFaire(avant))
        assertTrue(ditQuoiFaire("Carte impossible : placette absente de la tournée, revenir à la liste."))
        assertTrue(ditQuoiFaire("Carte impossible : choisir une autre placette."))
        assertFalse(ditQuoiFaire("Carte impossible : placette introuvable."))
        assertEquals(listOf("Placette"), majusculesApresDeuxPoints("Carte impossible : Placette absente"))
        assertEquals(emptyList(), majusculesApresDeuxPoints("Carte impossible : placette absente"))
    }

    // ------------------------------------------------------------------------------------------ les écrans

    @Test fun `guidage - placette absente de la tournee`() =
        assertEnFrancaisSansUuid("guidage, placette absente", guidage(ABSENTE), ABSENTE)

    @Test fun `guidage - placette absente, UUID en majuscules`() =
        assertEnFrancaisSansUuid("guidage, placette absente (majuscules)", guidage(ABSENTE_MAJUSCULES), ABSENTE_MAJUSCULES)

    @Test fun `guidage - placette de la tournee mais sans position`() =
        assertEnFrancaisSansUuid("guidage, placette sans position", guidage(SANS_POSITION), SANS_POSITION)

    @Test fun `controle - placette absente de la tournee`() =
        assertEnFrancaisSansUuid("contrôle, placette absente", controle(ABSENTE), ABSENTE)

    @Test fun `controle - placette absente, UUID en majuscules`() =
        assertEnFrancaisSansUuid("contrôle, placette absente (majuscules)", controle(ABSENTE_MAJUSCULES), ABSENTE_MAJUSCULES)
}
