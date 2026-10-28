package io.github.pobsteta.surculus.gnss

import io.github.pobsteta.surculus.controle.ArriveeControle
import io.github.pobsteta.surculus.controle.BancControle
import io.github.pobsteta.surculus.controle.EtatArrivee
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.session.FixSession
import io.github.pobsteta.surculus.core.session.JournalControle
import io.github.pobsteta.surculus.core.session.ValeurControle
import io.github.pobsteta.surculus.data.journal.db.SourceHorloge
import io.github.pobsteta.surculus.data.session.HorlogeSessionAppareil
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Mission 065, de bout en bout côté session : une vraie saisie par `SessionControle` (via le contrôleur de l'écran), avec
 * le fix donné par `FixPourSession`, écrit le jour GNSS dans la ligne `controle` et l'âge du fix mesuré sur le monotone.
 */
class FixPourSessionControleTest {
    private class Telephone(val utcMs: Long, val monotone: Long) : SourceHorloge {
        override fun maintenantUtcMs() = utcMs
        override fun monotoneMs() = monotone
        override fun demarrage() = 1L
    }

    /** Journal en mémoire qui retient le fix reçu à chaque saisie (la table Room en tire `age_fix_ms`). */
    private class JournalQuiNoteLeFix(private val j: JournalControle) : JournalControle by j {
        val fixParType = HashMap<String, FixSession?>()
        override fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?): String {
            fixParType[type] = fix
            return j.saisir(objetUuid, type, valeur, operateur, remplaceUuid, fix)
        }
    }

    @Test
    fun `fix de 3 s et telephone deregle - le jour GNSS est ecrit dans la ligne controle, age du fix 3000 ms`() = with(BancControle()) {
        val tel = Telephone(utcMs = Instant.parse("2025-01-01T10:00:00Z").toEpochMilli(), monotone = 9_000_000L)
        val gnss = FixGnss(
            position = BancControle.fixSur(BancControle.POSITION_P1), qualite = QualiteFix.AUTONOME, nbSatellites = 12, hdop = 0.8,
            altitudeM = null, ageCorrectionsS = null, precisionHorizontaleM = 3.0, horodatageMs = 0L,
            heureUtcMs = Instant.parse("2026-07-14T23:30:00Z").toEpochMilli(), recuMonotoneMs = 9_000_000L - 3_000L,
        )
        val notant = JournalQuiNoteLeFix(journal)
        val ouverture = ArriveeControle.ouvrir(
            contexte, notant, HorlogeSessionAppareil(tel) { FixPourSession.depuis(gnss) }, essences, positionsAuPiquet(), null, sauvegarde,
        )
        val controleur = (ouverture as EtatArrivee.Ouverte).controleur
        controleur.dire(EtatDsf.SAIN)

        val ligne = journal.lignesCourantes().first { it.type == "controle" }
        val controle = assertNotNull(ValeurControle.lire(ligne.valeur))
        assertEquals("2026-07-15", controle.jourCivil, "23 h 30 UTC le 14 juillet = 1 h 30 le 15 à Paris ; le téléphone dit 2025")
        val fix = assertNotNull(notant.fixParType["controle"])
        assertEquals(3_000L, tel.monotone - fix.monotoneMs, "âge du fix enregistré avec la ligne")
    }
}
