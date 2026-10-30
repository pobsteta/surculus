package io.github.pobsteta.surculus.rendu

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
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
    @get:Rule val regle = BancRendu.nouvelleRegle()

    @After fun fermer() = BancRendu.fermer()

    @Test fun `un effet qui revient d'un travail en entrees-sorties reprend sur le fil principal`() {
        val fils = ArrayList<Thread>()
        BancRendu.rendre(
            contenu = {
                androidx.compose.material3.Text("banc")
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) { Thread.sleep(20) }
                    fils += Thread.currentThread()
                }
            },
            apres = { Thread.sleep(200) },
            regle = regle,
        )
        assertTrue(fils.isNotEmpty(), "l'effet n'a pas repris")
        assertEquals(Looper.getMainLooper().thread, fils.single(), "l'effet a repris sur ${fils.single().name}, pas sur le fil principal")
    }
}
