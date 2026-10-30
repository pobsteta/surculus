package io.github.pobsteta.surculus.rendu

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.github.pobsteta.surculus.carte.CartePrete
import io.github.pobsteta.surculus.carte.DispositionBoutons
import io.github.pobsteta.surculus.carte.ExamenOrtho
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.gnss.ErreurRecepteurCarte
import io.github.pobsteta.surculus.gpkg.Cartographie
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
import io.github.pobsteta.surculus.gpkg.Tournee
import io.github.pobsteta.surculus.tournee.TourneeOuverte
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 080 : la carte rendue à la taille du téléphone de Pascal (2316 × 1080 px, 450 dpi), debout et couché, avec et sans
 * plein écran, avec le bandeau « Récepteur GNSS non démarré ». Échoue si un bouton est coupé, trop petit, ou si un texte ou un
 * autre nœud en recouvre un.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RenduCarteTest {
    @get:Rule val regle = createEmptyComposeRule()

    companion object {
        /** Les trois placettes de Couchey (P35-01 à 03), en Lambert-93, comme `BancControle` ; toutes en France. */
        val PLACETTES = listOf(
            PlacetteTournee("uuid-p1", "P35-01", null, 3.99, null, null, PointLambert93(846_164.76, 6_686_123.26)),
            PlacetteTournee("uuid-p2", "P35-02", null, 3.99, null, null, PointLambert93(846_264.76, 6_686_223.26)),
            PlacetteTournee("uuid-p3", "P35-03", null, 3.99, null, null, PointLambert93(846_364.76, 6_686_323.26)),
        )

        fun tournee(): TourneeOuverte = TourneeOuverte(
            dossier = File("couchey-essai-plants"),
            tournee = Tournee(
                epsg = CodeEpsg.RGF93_V1_LAMBERT93, srsId = 2154, couches = emptyList(), placettes = PLACETTES, controles = emptyList(),
                empriseForet = EmpriseLambert93(846_000.0, 6_686_000.0, 846_500.0, 6_686_500.0), projet = null, plants = emptyList(), etats = emptyList(),
            ),
            cartographie = Cartographie(foret = emptyList(), ug = emptyList(), parcelles = emptyList(), anomalies = emptyList(), empriseUg = null),
        )
    }


    private fun rendre(cas: String, choisie: Boolean) {
        val resultat = BancRendu.rendre(
            contenu = {
                CartePrete(
                    ouverte = tournee(), examenOrtho = ExamenOrtho(exploitable = false, illisible = false, mention = null),
                    centreId = if (choisie) "uuid-p3" else null, onRetour = {}, onGuider = {}, onControler = {},
                    erreurRecepteur = ErreurRecepteurCarte.NON_DEMARRE,
                )
            },
            regle = regle,
        )
        val attendus = if (choisie) listOf("Contrôler", "Naviguer vers la placette", "Zoom avant", "Zoom arrière", "Cadrer la tournée", "Recentrer") else listOf("Naviguer vers la placette", "Zoom avant", "Zoom arrière", "Cadrer la tournée", "Recentrer")
        val defauts = ArrayList<String>()

        // 1. Les boutons : tous là, entiers dans la racine.
        val boutons = attendus.map { BancRendu.cliquable(resultat, it) }
        attendus.zip(boutons).forEach { (nom, b) ->
            if (b == null) defauts += "bouton « $nom » absent"
            else if (!BancRendu.vu(resultat, b)) defauts += "bouton « $nom » ${BancRendu.fmt(b.rect)} n'est pas vu en entier dans la racine ${BancRendu.fmt(resultat.racine)}"
        }
        // 2. La taille : jamais sous le plancher, 48 dp quand la hauteur le permet.
        val colonne = boutons.filterNotNull()
        colonne.forEach { b ->
            if (b.rect.width < DispositionBoutons.MIN_DP - 0.5f || b.rect.height < DispositionBoutons.MIN_DP - 0.5f) {
                defauts += "bouton « ${b.nom} » de ${"%.0f".format(b.rect.width)} × ${"%.0f".format(b.rect.height)} dp, sous le plancher de ${DispositionBoutons.MIN_DP} dp"
            }
        }
        // 3. Rien ne recouvre un cliquable ; la carte (AndroidView) n'a pas de nœud, elle n'a donc pas d'exception à faire valoir.
        defauts += BancRendu.recouvrements(resultat)

        val sortie = BancRendu.rapport(cas, resultat, defauts)
        assertTrue(defauts.isEmpty(), "Rendu « $cas » :\n$sortie")
        assertEquals(attendus.size, colonne.size)
    }

    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `carte couchee en plein ecran avec placette choisie`() = rendre("carte-couche-plein-6", true)
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `carte couchee en plein ecran sans placette choisie`() = rendre("carte-couche-plein-5", false)
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `carte couchee sans plein ecran avec placette choisie`() = rendre("carte-couche-sans-plein-6", true)
    @Test @Config(qualifiers = BancRendu.COUCHE_SANS_PLEIN) fun `carte couchee sans plein ecran sans placette choisie`() = rendre("carte-couche-sans-plein-5", false)
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `carte debout en plein ecran avec placette choisie`() = rendre("carte-debout-plein-6", true)
    @Test @Config(qualifiers = BancRendu.DEBOUT_PLEIN) fun `carte debout en plein ecran sans placette choisie`() = rendre("carte-debout-plein-5", false)
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `carte debout sans plein ecran avec placette choisie`() = rendre("carte-debout-sans-plein-6", true)
    @Test @Config(qualifiers = BancRendu.DEBOUT_SANS_PLEIN) fun `carte debout sans plein ecran sans placette choisie`() = rendre("carte-debout-sans-plein-5", false)

    /**
     * Le rognage de la carte (mission 060) : `AndroidView` dessinait la `MapView` hors de ses bornes, sans changer sa taille ; les
     * bornes sémantiques ne le voient pas, seul un pixel le voit. Le fond du bandeau, aux endroits sans texte ni bouton, doit
     * être de la même couleur d'un bout à l'autre ; si la carte déborde dessus, ses pixels y paraissent.
     */
    @Test @Config(qualifiers = BancRendu.COUCHE_PLEIN) fun `la carte ne deborde pas sur le bandeau`() {
        BancRendu.rendre(
            contenu = {
                CartePrete(
                    ouverte = tournee(), examenOrtho = ExamenOrtho(exploitable = false, illisible = false, mention = null),
                    centreId = "uuid-p3", onRetour = {}, onGuider = {}, onControler = {}, erreurRecepteur = ErreurRecepteurCarte.NON_DEMARRE,
                )
            },
            regle = regle,
        )
        val vue = BancRendu.activite!!.window.decorView
        val image = android.graphics.Bitmap.createBitmap(vue.width, vue.height, android.graphics.Bitmap.Config.ARGB_8888)
        vue.draw(android.graphics.Canvas(image))
        val d = 2.8125f
        val pixels = IntArray(image.width * image.height).also { image.getPixels(it, 0, image.width, 0, 0, image.width, image.height) }
        fun px(xDp: Float, yDp: Float) = pixels[(yDp * d).toInt() * image.width + (xDp * d).toInt()]
        // Le bandeau fait 48 dp ; entre ses éléments (titre à 12-36 dp, boutons à 8-48 et 775-815 dp), le haut du bandeau (1-3 dp) est vide.
        val reference = px(300f, 2f)
        val ecarts = (60..760 step 20).filter { px(it.toFloat(), 2f) != reference || px(it.toFloat(), 45f) != reference }
        println("PIXEL référence=%08x écarts=%s".format(reference, ecarts))
        assertTrue(ecarts.isEmpty(), "le bandeau n'est pas d'une seule couleur : la carte déborde dessus (x en dp : $ecarts)")
    }

    @Test fun `les trois placettes de Couchey tombent en France`() {
        PLACETTES.forEach { p ->
            val (lon, lat) = Projections.versWgs84(p.position!!.xMetres, p.position!!.yMetres)
            assertTrue(lat in 41.0..51.5 && lon in -5.5..10.0, "${p.code} : lat $lat, lon $lon hors de France")
        }
    }
}
