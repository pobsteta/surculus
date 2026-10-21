package io.github.pobsteta.surculus.data.sauvegarde

/** Ce qui peut déclencher une sauvegarde (ADR 0005, règle « Sauvegarde »). */
enum class Evenement { FIN_PLACETTE, FIN_CONSTAT, FIN_VIDE, FIN_INTERVENTION, SAISIE, TIC }

/**
 * Règle de déclenchement, sans état : à chaque fin d'unité de travail, **et** à la N-ième saisie, **ou** après
 * M minutes de l'**horloge monotone** (jamais l'heure murale : un saut d'horloge ne change rien), au premier des
 * deux. **Jamais sans saisie nouvelle** depuis la dernière sauvegarde (décision 6 de Pascal).
 * N = 20 saisies et M = 5 minutes (décision de Pascal, 2026-10-21).
 */
object RegleDeclenchement {
    const val N_SAISIES = FormatSursync.N_SAISIES
    const val M_MINUTES = FormatSursync.M_MINUTES

    private val FINS = setOf(Evenement.FIN_PLACETTE, Evenement.FIN_CONSTAT, Evenement.FIN_VIDE, Evenement.FIN_INTERVENTION)

    fun doitSauvegarder(
        evenement: Evenement,
        saisiesDepuisDerniere: Int,
        minutesMonotonesDepuisDerniere: Double,
        n: Int = N_SAISIES,
        m: Int = M_MINUTES,
    ): Boolean = when {
        saisiesDepuisDerniere <= 0 -> false
        evenement in FINS -> true
        saisiesDepuisDerniere >= n -> true
        minutesMonotonesDepuisDerniere >= m -> true
        else -> false
    }
}

/** Compteur d'une tournée : saisies et instant monotone de la dernière sauvegarde. */
class SuiviDeclenchement(private var dernierMonotoneMs: Long, private val n: Int = RegleDeclenchement.N_SAISIES, private val m: Int = RegleDeclenchement.M_MINUTES) {
    private var saisies = 0

    /** À appeler à chaque événement ; `true` : sauvegarder maintenant (puis appeler [sauvegardeFaite]). */
    fun surEvenement(evenement: Evenement, monotoneMs: Long): Boolean {
        if (evenement == Evenement.SAISIE) saisies++
        return RegleDeclenchement.doitSauvegarder(evenement, saisies, (monotoneMs - dernierMonotoneMs) / 60_000.0, n, m)
    }

    fun sauvegardeFaite(monotoneMs: Long) { saisies = 0; dernierMonotoneMs = monotoneMs }
}
