package io.github.pobsteta.surculus.data.journal

import java.security.SecureRandom
import java.util.Random

/**
 * Générateur d'UUID version 7 (RFC 9562, § 5.7), écrit à la main : pas de dépendance.
 *
 * Disposition : 48 bits de millisecondes Unix | version 7 (4 bits) | `rand_a` 12 bits | variante `10`
 * (2 bits) | `rand_b` 62 bits. `rand_a` sert de compteur (méthode 1 du § 6.2) : strictement croissant dans
 * un même processus, même à la même milliseconde, et même si l'horloge recule (on garde alors la dernière
 * milliseconde émise). Les `uuid` d'un même appareil se rangent donc dans l'ordre de saisie ; entre
 * redémarrages, un recul d'horloge peut les désordonner, d'où l'ordre total (`saisi_le_utc`, appareil,
 * `sequence`, `uuid`) où l'`uuid` ne départage qu'en dernier.
 */
class GenerateurUuidV7(
    private val horlogeMs: () -> Long = System::currentTimeMillis,
    private val aleatoire: Random = SecureRandom(),
) {
    private var dernierMs = -1L
    private var compteur = 0

    @Synchronized
    fun suivant(): String {
        var ms = maxOf(horlogeMs(), dernierMs)
        if (ms == dernierMs) {
            compteur++
            if (compteur > MAX_COMPTEUR) { // 4096 UUID dans la même milliseconde : on avance d'une milliseconde
                ms++
                compteur = departCompteur()
            }
        } else {
            compteur = departCompteur()
        }
        dernierMs = ms
        return assembler(ms, compteur, aleatoire.nextLong())
    }

    // Départ aléatoire dans la moitié basse : au moins 2048 UUID de marge dans la même milliseconde.
    private fun departCompteur() = aleatoire.nextInt(MAX_COMPTEUR / 2)

    companion object {
        private const val MAX_COMPTEUR = 0xFFF

        /** Assemble un UUID v7 ; `randB` : seuls ses 62 bits de poids faible sont gardés. */
        fun assembler(unixMs: Long, randA: Int, randB: Long): String {
            require(unixMs in 0 until (1L shl 48)) { "horodatage hors des 48 bits : $unixMs" }
            require(randA in 0..MAX_COMPTEUR) { "rand_a hors des 12 bits : $randA" }
            val haut = (unixMs shl 16) or (7L shl 12) or randA.toLong()
            val bas = (randB and 0x3FFF_FFFF_FFFF_FFFFL) or (2L shl 62)
            return "%08x-%04x-%04x-%04x-%012x".format(
                haut ushr 32,
                (haut ushr 16) and 0xFFFF,
                haut and 0xFFFF,
                bas ushr 48,
                bas and 0xFFFF_FFFF_FFFFL,
            )
        }

        /** Millisecondes Unix lues dans un UUID v7. */
        fun horodatageMs(uuid: String): Long = uuid.replace("-", "").substring(0, 12).toLong(16)
    }
}
