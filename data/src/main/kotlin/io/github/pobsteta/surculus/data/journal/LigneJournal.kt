package io.github.pobsteta.surculus.data.journal

/** Types de ligne du journal (spec § 5, « Journal des saisies »). */
object TypeLigne {
    const val CONTROLE = "controle"
    const val POINTAGE = "pointage"
    const val ETAT = "etat"
    const val ANNULATION = "annulation"
    const val SAUT_HORLOGE = "saut_horloge"

    /** Liste fermée des cinq types de la spec § 5 : tout autre type est refusé. */
    val TOUS: Set<String> = setOf(CONTROLE, POINTAGE, ETAT, ANNULATION, SAUT_HORLOGE)
}

/**
 * Une ligne du journal : une saisie, jamais modifiée ni effacée (ADR 0005).
 *
 * Unités : tous les instants sont en **millisecondes depuis l'époque Unix, UTC** ; `monotoneMs` est
 * l'horloge monotone de l'appareil (`SystemClock.elapsedRealtime`), valable dans un même `demarrage` ;
 * `ageFixMs` est l'âge du dernier fix GNSS au moment de la saisie, mesuré sur l'horloge monotone
 * (`null` : aucun fix, ou âge inconnu).
 *
 * `valeur` est un texte JSON (ou `null`) ; deux lignes de même `uuid` sont « identiques » si tous leurs
 * champs, `valeur` comprise, sont égaux caractère pour caractère.
 */
data class LigneJournal(
    val uuid: String,
    val objetUuid: String,
    val type: String,
    val valeur: String?,
    val remplaceUuid: String?,
    val operateur: String,
    val appareil: String,
    val sequence: Long,
    val saisiLeUtcMs: Long,
    val heureGnssMs: Long?,
    val monotoneMs: Long,
    val demarrage: Long,
    val ageFixMs: Long?,
)
