package io.github.pobsteta.surculus.gnss

import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.core.placette.JourCivil
import io.github.pobsteta.surculus.data.journal.db.SourceHorloge
import io.github.pobsteta.surculus.data.session.HorlogeSessionAppareil
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mission 065 : le dernier fix GNSS (avec son heure UTC et son instant monotone) est donné à la session de contrôle, pour
 * que le jour civil se calcule sur l'heure GNSS quand le fix a au plus 5 s (spec § 5), même si le téléphone est déréglé.
 */
class FixPourSessionTest {
    private val couchey = Position(latitude = 47.1, longitude = 4.9)
    private val somalie = Position(latitude = 2.0469, longitude = 45.3182)

    private fun fix(position: Position = couchey, heureUtcMs: Long? = null, recuMonotoneMs: Long? = null) = FixGnss(
        position = position, qualite = QualiteFix.AUTONOME, nbSatellites = 12, hdop = 0.8, altitudeM = 300.0, ageCorrectionsS = null,
        precisionHorizontaleM = 3.0, horodatageMs = 0L, heureUtcMs = heureUtcMs, recuMonotoneMs = recuMonotoneMs,
    )

    private class Telephone(var utcMs: Long, var monotone: Long) : SourceHorloge {
        override fun maintenantUtcMs() = utcMs
        override fun monotoneMs() = monotone
        override fun demarrage() = 1L
    }

    /** Jour civil que la session retiendrait : les horloges de l'appareil et le fix, passés par la même règle que la session. */
    private fun jourRetenu(tel: Telephone, fixSource: FixGnss?): LocalDate {
        val h = HorlogeSessionAppareil(tel) { FixPourSession.depuis(fixSource) }
        val f = h.fix()
        return JourCivil.deSaisie(h.maintenantUtcMs(), f?.heureUtcMs, f?.let { h.monotoneMs() - it.monotoneMs })
    }

    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun `un fix sans heure UTC ou sans instant monotone n'est pas donne a la session`() {
        assertNull(FixPourSession.depuis(null))
        assertNull(FixPourSession.depuis(fix(heureUtcMs = null, recuMonotoneMs = 10L)))
        assertNull(FixPourSession.depuis(fix(heureUtcMs = 10L, recuMonotoneMs = null)))
    }

    @Test
    fun `un fix complet est donne avec son heure UTC et son instant monotone`() {
        val f = assertNotNull(FixPourSession.depuis(fix(heureUtcMs = 123L, recuMonotoneMs = 456L)))
        assertEquals(123L, f.heureUtcMs)
        assertEquals(456L, f.monotoneMs)
    }

    @Test
    fun `fix de 3 s, telephone deregle sur un autre jour - le jour est celui de l'heure GNSS, en ete`() {
        // 23 h 30 UTC le 14 juillet = 1 h 30 le 15 juillet à Paris (UTC+2). Le téléphone dit le 1er janvier 2025.
        val tel = Telephone(utcMs = ms("2025-01-01T10:00:00Z"), monotone = 9_000_000L)
        val gnss = fix(heureUtcMs = ms("2026-07-14T23:30:00Z"), recuMonotoneMs = 9_000_000L - 3_000L)
        assertEquals(LocalDate.parse("2026-07-15"), jourRetenu(tel, gnss))
    }

    @Test
    fun `fix de 3 s, telephone deregle - minuit a Paris en hiver (UTC+1)`() {
        val tel = Telephone(utcMs = ms("2026-12-01T12:00:00Z"), monotone = 9_000_000L)
        val avant = fix(heureUtcMs = ms("2026-01-10T22:59:59Z"), recuMonotoneMs = 8_997_000L)
        val apres = fix(heureUtcMs = ms("2026-01-10T23:00:00Z"), recuMonotoneMs = 8_997_000L)
        assertEquals(LocalDate.parse("2026-01-10"), jourRetenu(tel, avant))
        assertEquals(LocalDate.parse("2026-01-11"), jourRetenu(tel, apres))
    }

    @Test
    fun `fix de 3 s, telephone deregle - minuit a Paris en ete (UTC+2)`() {
        val tel = Telephone(utcMs = ms("2026-12-01T12:00:00Z"), monotone = 9_000_000L)
        val avant = fix(heureUtcMs = ms("2026-07-10T21:59:59Z"), recuMonotoneMs = 8_997_000L)
        val apres = fix(heureUtcMs = ms("2026-07-10T22:00:00Z"), recuMonotoneMs = 8_997_000L)
        assertEquals(LocalDate.parse("2026-07-10"), jourRetenu(tel, avant))
        assertEquals(LocalDate.parse("2026-07-11"), jourRetenu(tel, apres))
    }

    @Test
    fun `fix de plus de 5 s, absent, ou d'age negatif - le jour est celui de l'heure du telephone`() {
        val tel = Telephone(utcMs = ms("2026-10-27T09:00:00Z"), monotone = 9_000_000L)
        val heureGnss = ms("2026-10-15T09:00:00Z")
        val attendu = LocalDate.parse("2026-10-27")
        assertEquals(attendu, jourRetenu(tel, fix(heureUtcMs = heureGnss, recuMonotoneMs = 9_000_000L - 5_001L)), "5,001 s")
        assertEquals(attendu, jourRetenu(tel, null), "pas de fix")
        assertEquals(attendu, jourRetenu(tel, fix(heureUtcMs = heureGnss, recuMonotoneMs = 9_000_000L + 1L)), "reçu après la saisie")
        assertEquals(LocalDate.parse("2026-10-15"), jourRetenu(tel, fix(heureUtcMs = heureGnss, recuMonotoneMs = 9_000_000L - 5_000L)), "5 s pile : retenu")
    }

    @Test
    fun `l'age du fix est mesure sur l'horloge monotone, pas sur l'heure du telephone`() {
        // Le téléphone saute d'un jour entre la réception et la saisie : seul le monotone dit l'âge (3 s).
        val tel = Telephone(utcMs = ms("2030-01-01T00:00:00Z"), monotone = 9_000_000L)
        val h = HorlogeSessionAppareil(tel) { FixPourSession.depuis(fix(heureUtcMs = ms("2026-10-27T09:00:00Z"), recuMonotoneMs = 8_997_000L)) }
        assertEquals(3_000L, h.monotoneMs() - h.fix()!!.monotoneMs)
    }

    /** Placette de la Somalie (chapitre 15) : l'heure du fix ne dépend pas de l'endroit, et un fix hors de France ne casse rien. */
    @Test
    fun `un fix hors de France garde la meme regle d'heure et ne casse rien`() {
        val tel = Telephone(utcMs = ms("2025-01-01T10:00:00Z"), monotone = 9_000_000L)
        val g = fix(position = somalie, heureUtcMs = ms("2026-10-27T09:00:00Z"), recuMonotoneMs = 8_998_000L)
        assertNotNull(FixPourSession.depuis(g))
        assertEquals(LocalDate.parse("2026-10-27"), jourRetenu(tel, g))
    }

    @Test
    fun `un fix simule n'est pas une heure GNSS`() {
        assertNull(FixPourSession.depuis(fix(heureUtcMs = 123L, recuMonotoneMs = 456L).copy(simule = true)))
    }

    // Le branchement : l'écran du contrôle passe le dernier fix, plus jamais `{ null }`.
    private val main = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
    private fun source(chemin: String) = File(main, "kotlin/io/github/pobsteta/surculus/$chemin").readText()

    @Test
    fun `l'ecran du controle donne le dernier fix a la session, pas null`() {
        val ecran = source("controle/EcranControle.kt")
        assertFalse(Regex("""horloge\s*\(\s*ctx\s*\)\s*\{\s*null\s*\}""").containsMatchIn(ecran), "l'horloge sans fix est le défaut de la mission 065")
        assertTrue("FixPourSession.depuis(SourcePosition.fix.value)" in ecran, "le fix lu au moment de la saisie, pas celui de la composition")
    }
}
