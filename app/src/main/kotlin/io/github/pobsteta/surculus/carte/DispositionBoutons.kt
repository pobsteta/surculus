package io.github.pobsteta.surculus.carte

/**
 * Mission 078 (remplace la 064) : les boutons de la carte sont toujours en une seule colonne, comme dans Groma, sur toute
 * la hauteur disponible. 48 dp quand elle tient ; sinon la taille baisse juste ce qu'il faut, jamais sous 32 dp (décisions de
 * Pascal, 2026-10-30). Sous le plancher, les derniers boutons sont écrasés.
 */
internal object DispositionBoutons {
    const val CIBLE_DP = 48
    const val MIN_DP = 32
    const val ECART_DP = 8
    const val MARGE_DP = 12

    /** Hauteur (dp) d'une colonne de [n] boutons de [tailleDp], marges haut et bas comprises. */
    fun hauteurColonneDp(n: Int, tailleDp: Int): Int = if (n <= 0) 0 else n * tailleDp + (n - 1) * ECART_DP + 2 * MARGE_DP

    /** La taille des boutons dans [hauteurDp] : la plus grande entre [MIN_DP] (32) et [CIBLE_DP] (48) qui tient, sinon [MIN_DP]. */
    fun tailleDp(hauteurDp: Float, nombreBoutons: Int): Int =
        (CIBLE_DP downTo MIN_DP).firstOrNull { hauteurColonneDp(nombreBoutons, it) <= hauteurDp } ?: MIN_DP

    /** Largeur (dp) que les textes du haut et du bas laissent à la colonne : le bouton et ses deux marges. */
    fun largeurReserveeDp(tailleDp: Int): Int = tailleDp + 2 * MARGE_DP
}
