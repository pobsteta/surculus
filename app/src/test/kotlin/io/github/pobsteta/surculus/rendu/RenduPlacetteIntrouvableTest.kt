package io.github.pobsteta.surculus.rendu

import android.graphics.Bitmap
import android.graphics.Canvas
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 103 (reprise 1/3) : la preuve des trois refus « placette introuvable » dans le rapport `rendu-des-ecrans` de la CI :
 * pour chacun, l'image de l'écran (`.png`) et le texte affiché (`.txt`), écrits dans `build/reports/rendu/` comme les autres cas
 * du banc. Les assertions de la mission sont dans `PlacetteIntrouvableEnFrancaisTest` (du `testeur`) ; ici on ne juge que
 * « le refus est affiché » et on garde la preuve. Même tournée d'essai (Couchey, Lambert-93, x = Est, y = Nord, mètres).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = BancRendu.DEBOUT_PLEIN)
class RenduPlacetteIntrouvableTest {
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

    private val prefixe: String get() = RuntimeEnvironment.getApplication().getString(R.string.carte_refus, "").trim()

    private fun garder(cas: String, ecran: @Composable () -> Unit) {
        poserLaTournee()
        var trouve: String? = null
        val r = BancRendu.rendre(
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
        val texte = assertNotNull(trouve, "aucun texte « $prefixe… » en 20 s ; textes à l'écran : ${toutLeTexte()}")
        val vue = BancRendu.activite!!.window.decorView
        val image = Bitmap.createBitmap(vue.width, vue.height, Bitmap.Config.ARGB_8888)
        vue.draw(Canvas(image))
        val dossier = File("build/reports/rendu").also { it.mkdirs() }
        File(dossier, "$cas.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        BancRendu.rapport(cas, r, emptyList(), entete = listOf("Texte affiché : $texte"))
        assertTrue(image.width > 0 && image.height > 0)
    }

    @Test fun `guidage placette absente`() = garder("placette-introuvable-guidage-absente") { EcranGuidage(id = TOURNEE, placetteUuid = ABSENTE, onRetour = {}) }

    @Test fun `guidage placette sans position`() = garder("placette-introuvable-guidage-sans-position") { EcranGuidage(id = TOURNEE, placetteUuid = SANS_POSITION, onRetour = {}) }

    @Test fun `controle placette absente`() = garder("placette-introuvable-controle-absente") { EcranControle(id = TOURNEE, placetteUuid = ABSENTE, onRetour = {}) }
}
