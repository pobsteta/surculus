/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import io.github.pobsteta.surculus.data.journal.CleProjection
import io.github.pobsteta.surculus.data.journal.EtatCle
import io.github.pobsteta.surculus.data.journal.TypeLigne

/**
 * Les cinq colonnes de la vue Kanban, celles de Marculus (`EtatKanban`, mission 043, décision de Pascal du 2026-10-27).
 * L'ordre déclaré est l'ordre des colonnes **et** l'ordre de comparaison : « au moins Planifiée » veut dire `ordinal >= PLANIFIEE.ordinal`
 * (comme Marculus, où Abandonnée, dernière, passe donc au-dessus de tout minimum).
 */
enum class EtatKanban(val libelle: String) {
    PROPOSEE("Proposée"),
    VALIDEE("Validée"),
    PLANIFIEE("Planifiée"),
    REALISEE("Réalisée"),
    ABANDONNEE("Abandonnée"),
    ;

    companion object {
        /** Statut gardé sur le téléphone, relu depuis son nom ; absent ou inconnu : Proposée (comme Marculus). */
        fun depuisNom(nom: String?): EtatKanban = entries.firstOrNull { it.name == nom } ?: PROPOSEE
    }
}

/** La vue Kanban est désactivée tant que l'utilisateur ne l'a pas activée dans le menu « ⋮ » (comme Marculus). */
const val VUE_KANBAN_ACTIVE_PAR_DEFAUT = false

/**
 * Nombre de saisies **encore valables** d'une tournée, à partir de sa projection (décision de Pascal, 2026-10-27) : une clé
 * (objet, type) de contrôle, pointage ou état compte une fois si elle a une valeur courante. Une saisie annulée n'a plus de
 * valeur courante (« annuler, c'est défaire ») : elle ne compte plus ; une saisie corrigée compte une fois. Les sauts
 * d'horloge ne sont pas des saisies du forestier.
 */
fun compterSaisies(projection: Map<CleProjection, EtatCle>): Int =
    projection.count { (cle, etat) ->
        etat.courant != null && (cle.type == TypeLigne.CONTROLE || cle.type == TypeLigne.POINTAGE || cle.type == TypeLigne.ETAT)
    }

/**
 * Statut minimal imposé par l'avancement : rendue → Réalisée ; au moins une saisie → Planifiée ; sinon Proposée.
 * (`rendue` est toujours faux tant que l'export n'écrit pas la marque : mission 041.)
 */
fun statutMinimal(saisies: Int, rendue: Boolean): EtatKanban = when {
    rendue -> EtatKanban.REALISEE
    saisies > 0 -> EtatKanban.PLANIFIEE
    else -> EtatKanban.PROPOSEE
}

/** Colonne affichée : le plus avancé du statut gardé et du minimum, même si le statut gardé est inférieur. */
fun colonneAffichee(stocke: EtatKanban, minimal: EtatKanban): EtatKanban = if (stocke.ordinal < minimal.ordinal) minimal else stocke

/**
 * Dépôt d'une carte dans la colonne [cible] : le nouveau statut à garder, ou `null` si le dépôt est refusé (colonne sous le
 * minimum) ou sans effet (déjà le statut gardé). La carte reste alors où elle était.
 */
fun deposer(stocke: EtatKanban, cible: EtatKanban, minimal: EtatKanban): EtatKanban? =
    if (cible != stocke && cible.ordinal >= minimal.ordinal) cible else null

/** Répartit les tournées dans les cinq colonnes (toutes présentes, éventuellement vides), dans l'ordre d'origine. */
fun <T> repartir(lignes: List<T>, colonne: (T) -> EtatKanban): Map<EtatKanban, List<T>> =
    EtatKanban.entries.associateWith { e -> lignes.filter { colonne(it) == e } }

/** Ce que les réglages Kanban lisent et écrivent sur le téléphone (SharedPreferences en vrai, mémoire dans les tests). */
interface MagasinKanban {
    fun lireTexte(cle: String): String?
    fun ecrireTexte(cle: String, valeur: String)
    fun lireBooleen(cle: String, parDefaut: Boolean): Boolean
    fun ecrireBooleen(cle: String, valeur: Boolean)
}

/**
 * Réglages Kanban, **gardés sur le téléphone seulement** : ni le journal, ni `travaux.gpkg`, ni le `.sursync` ne les portent
 * (réponse 1 de Pascal, 2026-10-27). Perdus à la restauration sur un autre téléphone ; accepté.
 */
class ReglagesKanban(private val magasin: MagasinKanban) {
    var vueActive: Boolean
        get() = magasin.lireBooleen(CLE_VUE, VUE_KANBAN_ACTIVE_PAR_DEFAUT)
        set(v) = magasin.ecrireBooleen(CLE_VUE, v)

    fun statut(idTournee: String): EtatKanban = EtatKanban.depuisNom(magasin.lireTexte(PREFIXE_STATUT + idTournee))

    fun definirStatut(idTournee: String, statut: EtatKanban) = magasin.ecrireTexte(PREFIXE_STATUT + idTournee, statut.name)

    private companion object {
        const val CLE_VUE = "vue_kanban"
        const val PREFIXE_STATUT = "statut_"
    }
}
