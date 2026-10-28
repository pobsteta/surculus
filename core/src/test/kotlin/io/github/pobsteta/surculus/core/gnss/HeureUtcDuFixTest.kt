package io.github.pobsteta.surculus.core.gnss

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Mission 065 : l'heure UTC **complète** (jour compris) d'un fix du récepteur. La GGA ne porte que l'heure du jour
 * (`hhmmss.ss`) ; le jour vient de la date de la RMC (`ddmmyy`, UTC). Sans RMC valide, pas d'heure : le jour civil
 * retombe sur l'heure du téléphone (spec § 5).
 */
class HeureUtcDuFixTest {
    private fun trame(corps: String) = "\$$corps*${Nmea.sommeControle(corps)}"

    /** Position de Couchey (latitude 47,1 N, longitude 4,9 E : degrés, minutes décimales). */
    private fun gga(heure: String) = trame("GNGGA,$heure,4706.000,N,00454.000,E,4,12,0.8,300.0,M,48.0,M,1.0,0000")

    private fun rmc(heure: String, date: String, statut: String = "A") =
        trame("GNRMC,$heure,$statut,4706.000,N,00454.000,E,0.0,,$date,,,R")

    private fun fix(vararg trames: String, monotone: (() -> Long)? = null): FixGnss? {
        val lecteur = if (monotone == null) LecteurNmea(horloge = { 1L }) else LecteurNmea(horloge = { 1L }, horlogeMonotone = monotone)
        return lecteur.pousser(trames.joinToString("\r\n", postfix = "\r\n")).second.lastOrNull()
    }

    @Test
    fun `la RMC donne la date, la GGA l'heure, le fix porte l'instant UTC complet`() {
        val f = fix(rmc("091500.00", "271026"), gga("091500.00"))!!
        assertEquals(Instant.parse("2026-10-27T09:15:00Z").toEpochMilli(), f.heureUtcMs)
    }

    @Test
    fun `les centiemes de seconde sont gardes`() {
        val f = fix(rmc("091500.50", "271026"), gga("091500.50"))!!
        assertEquals(Instant.parse("2026-10-27T09:15:00.500Z").toEpochMilli(), f.heureUtcMs)
    }

    @Test
    fun `minuit UTC - la RMC d'avant minuit et la GGA d'apres donnent le lendemain`() {
        val f = fix(rmc("235959.00", "271026"), gga("000000.00"))!!
        assertEquals(Instant.parse("2026-10-28T00:00:00Z").toEpochMilli(), f.heureUtcMs)
    }

    @Test
    fun `minuit UTC - la RMC d'apres minuit et la GGA d'avant donnent la veille`() {
        val f = fix(rmc("000000.00", "281026"), gga("235959.00"))!!
        assertEquals(Instant.parse("2026-10-27T23:59:59Z").toEpochMilli(), f.heureUtcMs)
    }

    @Test
    fun `sans RMC, pas d'heure UTC complete`() {
        assertNull(fix(gga("091500.00"))!!.heureUtcMs)
    }

    @Test
    fun `une RMC invalide (statut V) ou sans date ne donne pas d'heure`() {
        assertNull(fix(rmc("091500.00", "271026", statut = "V"), gga("091500.00"))!!.heureUtcMs)
        assertNull(fix(rmc("091500.00", ""), gga("091500.00"))!!.heureUtcMs)
    }

    @Test
    fun `une GGA sans heure ne donne pas d'heure`() {
        assertNull(fix(rmc("091500.00", "271026"), gga(""))!!.heureUtcMs)
    }

    @Test
    fun `le fix note l'instant de reception sur l'horloge monotone, absent sans horloge monotone`() {
        assertEquals(7_654_321L, fix(rmc("091500.00", "271026"), gga("091500.00"), monotone = { 7_654_321L })!!.recuMonotoneMs)
        assertNull(fix(gga("091500.00"))!!.recuMonotoneMs)
    }
}
