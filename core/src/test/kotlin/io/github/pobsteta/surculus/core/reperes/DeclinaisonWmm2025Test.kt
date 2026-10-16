package io.github.pobsteta.surculus.core.reperes

import io.github.pobsteta.surculus.core.reperes.wmm.Declinaison
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.time.LocalDate
import kotlin.math.abs

class DeclinaisonWmm2025Test {

    private val ressourceTests = "/io/github/pobsteta/surculus/core/reperes/wmm/WMM2025_TestValues.txt"
    private val ressourceCof = "/io/github/pobsteta/surculus/core/reperes/wmm/WMM2025.COF"

    private fun sha256(chemin: String): String {
        val octets = checkNotNull(javaClass.getResourceAsStream(chemin)) { "ressource absente : $chemin" }.readBytes()
        return MessageDigest.getInstance("SHA-256").digest(octets).joinToString("") { "%02x".format(it) }
    }

    private fun point(lat: Double, lon: Double) = PointGeographique.de(lat, lon).valeurOuEchec()

    /**
     * Les 100 valeurs de test officielles du WMM2025 : fichier `WMM2025_TestValues.txt` de l'archive NOAA NCEI
     * https://www.ncei.noaa.gov/sites/default/files/2024-12/WMM2025COF.zip (téléchargée le 2026-10-16),
     * recopié sans modification. Champs : 1 année décimale, 2 altitude (km au-dessus de l'ellipsoïde WGS 84,
     * README-WMM-COEFS.txt), 3 latitude géodésique, 4 longitude, 5 déclinaison (degrés, arrondie au centième).
     * Tolérance : 0,01°, l'arrondi de NCEI.
     */
    @Test
    fun `les 100 valeurs de test officielles de NCEI sont retrouvees`() {
        assertEquals("e6975b093dddeb6153e0b23cc418425c438167e7c5b1dd795da379cb654f5819", sha256(ressourceTests), "fichier de test NCEI modifié")
        val lignes = javaClass.getResourceAsStream(ressourceTests)!!.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
        assertEquals(100, lignes.size)
        val ecarts = lignes.mapNotNull { l ->
            val c = l.trim().split(Regex("\\s+")).map { it.toDouble() }
            val obtenu = Declinaison.declinaisonDeg(latDeg = c[2], lonDeg = c[3], annee = c[0], altitudeM = c[1] * 1000.0)
            if (abs(obtenu - c[4]) > 0.01 + 1e-9) "$l -> $obtenu" else null
        }
        assertTrue(ecarts.isEmpty(), ecarts.joinToString("\n"))
    }

    @Test
    fun `le fichier de coefficients est celui de NCEI, epoque 2025_0`() {
        assertEquals("dfa8597825af4e0b87ff4198a5b4fb661b3c49f4cd090cd0164e0259b075582f", sha256(ressourceCof))
        // Un nouveau modèle change l'époque : ce test oblige alors à revoir PREMIER_JOUR et DERNIER_JOUR.
        assertEquals(2025.0, DeclinaisonWmm2025.epoqueDuModele)
    }

    @Test
    fun `bornes de validite posees, du 2025-01-01 au 2029-12-31 inclus`() {
        assertEquals(LocalDate.of(2025, 1, 1), DeclinaisonWmm2025.PREMIER_JOUR)
        assertEquals(LocalDate.of(2029, 12, 31), DeclinaisonWmm2025.DERNIER_JOUR)
        val strasbourg = point(48.58, 7.75)
        assertTrue(DeclinaisonWmm2025.calculer(strasbourg, LocalDate.of(2025, 1, 1), 0.0) is Resultat.Accepte)
        assertTrue(DeclinaisonWmm2025.calculer(strasbourg, LocalDate.of(2029, 12, 31), 0.0) is Resultat.Accepte)
        val r = DeclinaisonWmm2025.calculer(strasbourg, LocalDate.of(2030, 1, 1), 0.0)
        assertEquals("date_hors_validite", (r as Resultat.Refuse).refus.motif)
        assertTrue(DeclinaisonWmm2025.calculer(strasbourg, LocalDate.of(2024, 12, 31), 0.0) is Resultat.Refuse)
    }

    @Test
    fun `le 2030-01-01 est refuse alors que la garde de Groma l'aurait accepte`() {
        val annee = DeclinaisonWmm2025.anneeDecimale(LocalDate.of(2030, 1, 1))
        assertEquals(2030.0, annee)
        assertTrue(annee in Declinaison.validite, "garde de Groma : 2030.0 inclus")
        assertTrue(DeclinaisonWmm2025.calculer(point(47.26, 4.99), LocalDate.of(2030, 1, 1), 0.0) is Resultat.Refuse)
    }

    @Test
    fun `annee decimale du logiciel WMM`() {
        // docs/referentiels/modele-geomagnetique.md : 2026-10-15 = 2026,7863
        assertEquals(2026.7863, DeclinaisonWmm2025.anneeDecimale(LocalDate.of(2026, 10, 15)), 1e-4)
        assertEquals(2028.0 + 182.0 / 366.0, DeclinaisonWmm2025.anneeDecimale(LocalDate.of(2028, 7, 1)), 1e-12)
    }

    /** Critère de la fiche 005 : déclinaison à Couchey de +2,80° au 2026-10-15, à 0,05° près. */
    @Test
    fun `Couchey vaut +2,80 degres au 2026-10-15`() {
        val d = DeclinaisonWmm2025.calculer(point(47.26, 4.99), LocalDate.of(2026, 10, 15), 0.0).valeurOuEchec()
        assertEquals(2.80, d, 0.05)
    }

    @Test
    fun `signe positif vers l'est, negatif vers l'ouest`() {
        // Valeurs officielles NCEI (WMM2025_TestValues.txt) : 54,89 à (82, 87) et -29,91 à (80, -96), 2025.0.
        assertTrue(Declinaison.declinaisonDeg(82.0, 87.0, 2025.0, 54_000.0) > 0)
        assertTrue(Declinaison.declinaisonDeg(80.0, -96.0, 2025.0, 48_000.0) < 0)
    }

    @Test
    fun `l'altitude negligee sur le terrain change la declinaison de moins de 0,005 degre pour 500 m`() {
        for ((lat, lon) in listOf(48.39 to -4.49, 47.26 to 4.99, 48.58 to 7.75, 43.3 to 5.37)) {
            val p = point(lat, lon)
            val a0 = DeclinaisonWmm2025.calculer(p, LocalDate.of(2026, 10, 15), 0.0).valeurOuEchec()
            val a500 = DeclinaisonWmm2025.calculer(p, LocalDate.of(2026, 10, 15), 500.0).valeurOuEchec()
            assertTrue(abs(a500 - a0) < 0.005, "($lat, $lon) : ${a500 - a0}")
        }
    }
}
