package io.github.pobsteta.surculus.controle

/**
 * Mission 095 (décision de Pascal du 2026-11-06) : avant « Terminer », tout plant actif, ancien ou pointé ce jour, a un état
 * ou « non observé ». Ce complément dit « non observé » (dictée) sur chaque plant qui n'a encore rien, puis revient au plant
 * où l'on était : les tests qui terminent avec quelques plants dits et les autres laissés de côté gardent leurs assertions.
 */
fun ControleurPlacette.autresPlantsNonObserves() {
    val depart = instantane().plant.numero
    while (instantane().plant.aPrecedent) plantPrecedent()
    while (true) {
        val p = instantane().plant
        if (p.libelleEtat == TextesControle.PAS_ENCORE_DIT) dicter("non observé")
        val avant = p.numero
        if (plantSuivant().plant.numero == avant) break
    }
    while (instantane().plant.numero != depart) plantPrecedent()
}
