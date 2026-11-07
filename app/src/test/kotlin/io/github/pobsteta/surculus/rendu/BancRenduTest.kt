package io.github.pobsteta.surculus.rendu

import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Le banc lui-même (mission 080) : un effet qui revient d'un `withContext(Dispatchers.IO)` reprend sur le fil principal, comme en
 * production (`AndroidUiDispatcher`). Avec `createEmptyComposeRule()` seul, il reprenait sur le fil d'entrées-sorties
 * (`DefaultDispatcher-worker-1`, vu à la sonde) : la recomposition qui suivait s'y faisait, et les `BackHandler` du contrôle
 * ajoutaient un observateur de cycle de vie hors du fil principal (`IllegalStateException`, CI du 2026-10-30).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class BancRenduTest {
    val regle = BancRendu.nouvelleRegle()

    /** Le garde-fou entoure la règle Compose, comme dans les autres classes du banc. */
    @get:Rule val chaine: RuleChain = RuleChain.outerRule(BancRendu.surveillant()).around(regle)

    @After fun fermer() = BancRendu.fermer()

    @Test fun `un effet qui revient d'un travail en entrees-sorties reprend sur le fil principal`() {
        val fils = ArrayList<Thread>()
        BancRendu.rendre(
            contenu = {
                Text("banc")
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) { Thread.sleep(20) }
                    synchronized(fils) { fils += Thread.currentThread() }
                }
            },
            // Attente bornée, comme celle du banc : jusqu'à 20 s que l'effet ait repris, sinon on le dit.
            apres = {
                val limite = System.nanoTime() + 20_000_000_000L
                while (synchronized(fils) { fils.isEmpty() } && System.nanoTime() < limite) {
                    waitForIdle()
                    Thread.sleep(20)
                }
            },
            regle = regle,
        )
        assertTrue(fils.isNotEmpty(), "l'effet n'a pas repris en 20 s")
        assertEquals(Looper.getMainLooper().thread, fils.single(), "l'effet a repris sur ${fils.single().name}, pas sur le fil principal")
    }
}
