package io.github.pobsteta.surculus.gpkg

/**
 * État de suivi d'une placette, calculé par sommieR (`R/travaux_qfield.R:334-337`) et lu tel quel dans la couche
 * `placettes` : `jamais` (aucun contrôle), `a_jour` (dernier besoin vide ou `aucun`), `a_programmer` (sinon).
 * Surculus ne le recalcule pas. Toute autre valeur, ou l'absence de valeur, est [INCONNU] : on n'invente rien.
 */
enum class EtatSuivi(val valeur: String?) {
    JAMAIS("jamais"),
    A_PROGRAMMER("a_programmer"),
    A_JOUR("a_jour"),
    INCONNU(null),
    ;

    companion object {
        fun depuis(valeur: String?): EtatSuivi = entries.firstOrNull { it.valeur != null && it.valeur == valeur } ?: INCONNU
    }
}

/** Teintes de la palette du projet (quatre au plus) ; l'application les traduit en couleurs. */
enum class Teinte { ROUGE, ORANGE, VERT, NOIR }

enum class FormePastille { DISQUE_PLEIN, ANNEAU, DISQUE_AVEC_POINT_INTERROGATION }

/**
 * Comment une placette se dessine selon son [EtatSuivi] : la couleur ne porte pas seule le sens (plein soleil,
 * daltonisme), la forme le redit. Liseré blanc pour tous.
 * Source : pilotage/journal.md, 2026-10-22, décisions de Pascal (mission 033), option A.
 */
data class Pastille(val teinte: Teinte, val forme: FormePastille) {
    companion object {
        fun de(etat: EtatSuivi): Pastille = when (etat) {
            EtatSuivi.JAMAIS -> Pastille(Teinte.ROUGE, FormePastille.DISQUE_PLEIN)
            EtatSuivi.A_PROGRAMMER -> Pastille(Teinte.ORANGE, FormePastille.DISQUE_PLEIN)
            EtatSuivi.A_JOUR -> Pastille(Teinte.VERT, FormePastille.ANNEAU)
            EtatSuivi.INCONNU -> Pastille(Teinte.NOIR, FormePastille.DISQUE_AVEC_POINT_INTERROGATION)
        }
    }
}

/** Décomptes de la légende : une entrée par état, zéro compris. */
fun decomptes(placettes: List<PlacetteTournee>): Map<EtatSuivi, Int> =
    EtatSuivi.entries.associateWith { e -> placettes.count { it.etatSuivi == e } }
