package io.github.pobsteta.surculus.carte

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 060 : le bandeau de la carte ne se voyait pas (v0.7.57). Cause : osmdroid 6.1.20 ne se limite pas à ses bords
 * (`MapView.dispatchDraw`, `TilesOverlay.onTileReadyToDraw` : tuiles entières, sans `clipRect` quand `canvasRect == null`),
 * et Compose ne rogne pas non plus une vue Android (`AndroidViewHolder` : `drawBehind` sans clip ; `AndroidViewsHandler` :
 * `clipChildren = false`). Dans la `Column`, la carte est dessinée après le bandeau : elle le recouvre.
 * Sans émulateur, lecture des sources : la carte doit être rognée à ses bords, et seul EcranCarte héberge une MapView.
 */
class CarteClipTest {
    private val main = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
    private val ecran by lazy { File(main, "kotlin/io/github/pobsteta/surculus/carte/EcranCarte.kt").readText() }

    @Test
    fun `la carte est rognee a ses bords, elle et son conteneur`() {
        val debut = ecran.indexOf("AndroidView(")
        assertTrue(debut > 0)
        val appel = ecran.substring(debut, ecran.indexOf("factory", debut))
        assertTrue("clipToBounds()" in appel, "AndroidView(MapView) doit porter clipToBounds()")
        assertTrue(
            Regex("Box\\(Modifier\\.weight\\(1f\\)\\.fillMaxWidth\\(\\)\\.clipToBounds\\(\\)\\)").containsMatchIn(ecran),
            "la Box de la carte doit porter clipToBounds()",
        )
        assertTrue("import androidx.compose.ui.draw.clipToBounds" in ecran)
    }

    @Test
    fun `le bandeau est pose avant la carte, et le guidage n'a pas de carte`() {
        assertTrue(ecran.indexOf("BandeauCarte(") < ecran.indexOf("AndroidView("))
        val guidage = File(main, "kotlin/io/github/pobsteta/surculus/carte/EcranGuidage.kt").readText()
        assertFalse("MapView" in guidage || "AndroidView" in guidage)
        assertTrue("BandeauCompact" in guidage)
        val ailleurs = main.walkTopDown()
            .filter { it.extension == "kt" && it.name != "EcranCarte.kt" && "MapView(" in it.readText() }
            .toList()
        assertEquals(emptyList(), ailleurs)
    }
}
