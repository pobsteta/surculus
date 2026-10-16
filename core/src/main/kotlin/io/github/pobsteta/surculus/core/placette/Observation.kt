/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.placette

/**
 * États d'un plant contrôlé, nomenclature du DSF : `docs/referentiels/etats-plants.yaml`, section
 * `etats_dsf` ([DSF24] p. 8). Un test vérifie que cette liste est exactement celle du référentiel.
 * L'agent ne dit jamais « hors entreprise » : c'est sommieR qui le décide (spec § 4.3).
 */
enum class EtatDsf(val code: String, val vivant: Boolean) {
    SAIN("sain", vivant = true),
    ATTEINT("atteint", vivant = true),
    MORT("mort", vivant = false),
    ABSENT("absent", vivant = false),
    ;

    companion object {
        /** Lecture stricte, sensible à la casse : « SAIN » ou « vivant » sont refusés. */
        fun lire(code: String): EtatDsf? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Les quatre groupes de causes du bilan DSF : `docs/referentiels/etats-plants.yaml`, section
 * `causes_dsf_groupes_bilan` ([DSF24] p. 8). Un test vérifie l'égalité avec le référentiel.
 */
enum class GroupeCause(val code: String) {
    ENTOMOLOGIQUE("entomologique"),
    PATHOLOGIQUE("pathologique"),
    ANIMAUX("animaux"),
    ABIOTIQUE_COMPLEXE_INDETERMINE("abiotique_complexe_indetermine"),
    ;

    companion object {
        fun lire(code: String): GroupeCause? = entries.firstOrNull { it.code == code }
    }
}

/** Code d'essence ONF à 3 caractères, pris dans `docs/referentiels/essences.csv`. */
@JvmInline
value class CodeEssence(val code: String) {
    override fun toString() = code
}

/**
 * Codes d'essence connus, **injectés** par l'appelant (lus dans `essences.csv` embarqué) : `:core` ne lit
 * aucun fichier (arbitrage de Pascal 2026-10-16, Q10).
 */
class Essences(connues: Set<String>) {
    private val codes = connues.toSet()

    fun valider(code: String): Verdict<CodeEssence> =
        if (code in codes) Verdict.Accepte(CodeEssence(code)) else Verdict.Refuse(RefusPlacette.EssenceInconnue(code))
}

/** Ce qu'un contrôle dit d'un plant : un état observé, ou « non observé » (ligne de `etats_plants`). */
sealed interface Observation {
    /**
     * Plant vu, dans un état du DSF. On ne l'obtient valide que par [Observations.valider] ; le constructeur
     * revérifie les mêmes règles.
     */
    data class Observe(
        val etat: EtatDsf,
        val causeGroupe: GroupeCause? = null,
        /** Détail libre (Q1), sans espace autour, jamais vide. */
        val causeDetail: String? = null,
        /** Hauteur en centimètres, entier strictement positif (Q8). */
        val hauteurCm: Int? = null,
    ) : Observation {
        init {
            check(Observations.refus(etat, causeGroupe, causeDetail, hauteurCm) == null) {
                "Observation invalide : ${Observations.refus(etat, causeGroupe, causeDetail, hauteurCm)?.message}"
            }
        }

        /**
         * Plant abrouti : sain ou atteint dont le détail porte l'abroutissement (spec § 4.3). Un plant mort
         * d'abroutissement n'est pas abrouti au sens de `nb_abroutis`.
         */
        val abrouti: Boolean get() = etat.vivant && causeDetail == Observations.DETAIL_ABROUTISSEMENT
    }

    /**
     * Plant pointé non retrouvé : ce n'est pas un état, c'est une absence de mesure. Hors totaux de ce
     * contrôle, à chercher au passage suivant (spec § 4.3).
     */
    data object NonObserve : Observation
}

object Observations {
    /** Code écrit dans `etats_plants.etat` pour un plant non observé (spec § 5). */
    const val CODE_NON_OBSERVE = "non_observe"

    /**
     * Seule valeur de détail reconnue par le domaine (spec § 4.3, arbitrage de Pascal 2026-10-16, Q1) ; les
     * autres détails (frottis, hylobe, gel…) sont du texte libre, sans contrôle de cohérence avec le groupe.
     */
    const val DETAIL_ABROUTISSEMENT = "abroutissement"

    /**
     * Valide une ligne d'état saisie. Règles (spec § 4.3 et § 6) :
     * - état parmi ceux du DSF, ou `non_observe` ;
     * - groupe de cause parmi les quatre du DSF, refusé sur un sain ou un absent ;
     * - détail : refusé sur un absent ; sur un sain, seul « abroutissement » ; libre sur atteint ou mort,
     *   avec ou sans groupe ;
     * - hauteur entière strictement positive ;
     * - un non observé ne porte ni cause, ni détail, ni hauteur.
     */
    fun valider(etat: String, causeGroupe: String? = null, causeDetail: String? = null, hauteurCm: Int? = null): Verdict<Observation> {
        val detail = causeDetail?.trim()?.takeIf { it.isNotEmpty() }
        if (etat == CODE_NON_OBSERVE) {
            return if (causeGroupe != null || detail != null || hauteurCm != null) {
                Verdict.Refuse(RefusPlacette.MesureSurNonObserve)
            } else {
                Verdict.Accepte(Observation.NonObserve)
            }
        }
        val e = EtatDsf.lire(etat) ?: return Verdict.Refuse(RefusPlacette.EtatInconnu(etat))
        val g = causeGroupe?.let { GroupeCause.lire(it) ?: return Verdict.Refuse(RefusPlacette.GroupeCauseInconnu(it)) }
        refus(e, g, detail, hauteurCm)?.let { return Verdict.Refuse(it) }
        return Verdict.Accepte(Observation.Observe(e, g, detail, hauteurCm))
    }

    internal fun refus(etat: EtatDsf, groupe: GroupeCause?, detail: String?, hauteurCm: Int?): RefusPlacette? {
        if (groupe != null && (etat == EtatDsf.SAIN || etat == EtatDsf.ABSENT)) return RefusPlacette.CauseSurEtat(etat)
        if (detail != null) {
            if (detail.isBlank() || detail != detail.trim()) return RefusPlacette.DetailSurEtat(etat, detail)
            if (etat == EtatDsf.ABSENT) return RefusPlacette.DetailSurEtat(etat, detail)
            if (etat == EtatDsf.SAIN && detail != DETAIL_ABROUTISSEMENT) return RefusPlacette.DetailSurEtat(etat, detail)
        }
        if (hauteurCm != null && hauteurCm <= 0) return RefusPlacette.HauteurInvalide(hauteurCm)
        return null
    }
}

/** Une ligne de `etats_plants` : l'observation d'un plant lors d'un contrôle. */
data class EtatPlant(val plant: UuidPlant, val observation: Observation)
