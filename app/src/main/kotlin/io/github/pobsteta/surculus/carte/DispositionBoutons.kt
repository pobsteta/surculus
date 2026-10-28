package io.github.pobsteta.surculus.carte

/** Mission 064 : combien de boutons de carte par colonne, selon la hauteur visible de la carte. */
internal object DispositionBoutons {
    const val CIBLE_DP = 48
    const val ECART_DP = 8
    const val MARGE_DP = 8

    /** Hauteur (dp) d'une colonne de [n] boutons, marges haut et bas comprises. */
    fun hauteurColonneDp(n: Int): Int = if (n <= 0) 0 else n * CIBLE_DP + (n - 1) * ECART_DP + 2 * MARGE_DP

    /** Nombre maximal de boutons par colonne dans [hauteurDp] ; au moins un. En portrait, tous tiennent : une colonne. */
    fun boutonsParColonne(hauteurDp: Float, nombreBoutons: Int): Int {
        var n = nombreBoutons
        while (n > 1 && hauteurColonneDp(n) > hauteurDp) n--
        n = n.coerceAtLeast(1)
        // Colonnes équilibrées : 3 + 3 plutôt que 4 + 2.
        val colonnes = (nombreBoutons + n - 1) / n
        return (nombreBoutons + colonnes - 1) / colonnes
    }

    fun colonnes(hauteurDp: Float, nombreBoutons: Int): Int {
        val par = boutonsParColonne(hauteurDp, nombreBoutons)
        return (nombreBoutons + par - 1) / par
    }
}
