package io.github.pobsteta.surculus.data.journal

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Un saut d'horloge noté entre deux lignes consécutives d'un même appareil et d'un même démarrage. */
data class SautHorloge(
    val avantUuid: String,
    val apresUuid: String,
    /** Variation de l'heure brute moins variation de l'horloge monotone, en ms, signée. */
    val ecartMs: Long,
    /** `saisiLeUtcMs` de la ligne précédente et de la ligne courante (heures brutes, jamais réécrites). */
    val heureAvantMs: Long,
    val heureApresMs: Long,
)

/** Règles de temps du journal (ADR 0005, « Temps » ; spec § 5). */
object Horloge {
    /** Un écart est noté au-delà de 60 s **strictement**, dans les deux sens. */
    const val SEUIL_SAUT_MS = 60_000L

    /** Un fix GNSS sert au jour civil s'il a au plus 5 s (borne incluse) et un âge non négatif. */
    const val AGE_FIX_MAX_MS = 5_000L

    /** Le jour civil d'un contrôle est celui d'Europe/Paris (règle « un contrôle par placette et par jour »). */
    val PARIS: ZoneId = ZoneId.of("Europe/Paris")

    /**
     * Écart signé entre l'heure brute et l'horloge monotone de deux lignes, ou `null` si elles ne sont pas
     * comparables (autre appareil ou autre démarrage : l'horloge monotone repart de zéro).
     */
    fun ecartMs(avant: LigneJournal, apres: LigneJournal): Long? {
        if (avant.appareil != apres.appareil || avant.demarrage != apres.demarrage) return null
        return (apres.saisiLeUtcMs - avant.saisiLeUtcMs) - (apres.monotoneMs - avant.monotoneMs)
    }

    fun estUnSaut(ecartMs: Long) = kotlin.math.abs(ecartMs) > SEUIL_SAUT_MS

    /**
     * Sauts d'horloge d'un ensemble de lignes : lignes rangées par `sequence` dans chaque couple
     * (appareil, démarrage), comparées deux à deux. Les lignes `saut_horloge` n'y entrent pas.
     */
    fun sauts(lignes: Collection<LigneJournal>): List<SautHorloge> =
        lignes.filter { it.type != TypeLigne.SAUT_HORLOGE }
            .groupBy { it.appareil to it.demarrage }
            .toSortedMap(compareBy({ it.first }, { it.second }))
            .values
            .flatMap { groupe ->
                groupe.sortedBy { it.sequence }.zipWithNext().mapNotNull { (a, b) ->
                    ecartMs(a, b)?.takeIf(::estUnSaut)?.let {
                        SautHorloge(a.uuid, b.uuid, it, a.saisiLeUtcMs, b.saisiLeUtcMs)
                    }
                }
            }

    /**
     * Jour civil Europe/Paris d'une saisie : sur l'heure GNSS **telle qu'enregistrée** quand le fix a un âge
     * compris entre 0 et 5000 ms inclus, sinon sur l'heure du téléphone. Un âge négatif (fix postérieur à la
     * saisie), trop grand ou inconnu écarte le fix.
     */
    fun jourCivil(saisiLeUtcMs: Long, heureGnssMs: Long?, ageFixMs: Long?): LocalDate {
        val gnssUtilisable = heureGnssMs != null && ageFixMs != null && ageFixMs in 0..AGE_FIX_MAX_MS
        val instant = if (gnssUtilisable) heureGnssMs!! else saisiLeUtcMs
        return Instant.ofEpochMilli(instant).atZone(PARIS).toLocalDate()
    }
}
