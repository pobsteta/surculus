/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 013, ADR 0004) ; aucun code repris de Marculus ni de terrain.
 */
package io.github.pobsteta.surculus.core.dictee

import io.github.pobsteta.surculus.core.dictee.ResultatDictee.Accepte
import io.github.pobsteta.surculus.core.dictee.ResultatDictee.Refus

/**
 * Analyse d'une phrase rendue par le moteur (spec § 4.3). Fonctions pures.
 *
 * Normalisation : seuls les espaces en tête, en queue ou doublés sont ramenés à un espace ; la casse, la
 * ponctuation et les accents ne le sont pas (une phrase hors forme est refusée, jamais approchée).
 */
object AnalyseurDictee {
    private val espaces = Regex("\\s+")

    fun normaliser(phrase: String): String = phrase.trim().replace(espaces, " ")

    /** La phrase seule, sans contexte. */
    fun analyser(phrase: String): ResultatDictee {
        val p = normaliser(phrase)
        if (p.isEmpty()) return Refus("phrase vide")
        Grammaire.commandesFixes[p]?.let { return Accepte(it) }
        val prefixe = Grammaire.MOT_HAUTEUR + " "
        if (p.startsWith(prefixe)) {
            val cm = LectureNombres.analyser(p.removePrefix(prefixe))
                ?: return Refus("hauteur : nombre de ${Grammaire.HAUTEUR_MIN_CM} à ${Grammaire.HAUTEUR_MAX_CM} attendu")
            return Accepte(Commande.Hauteur(cm))
        }
        return Refus("hors grammaire")
    }

    /**
     * La phrase confrontée à l'état courant du plant ([etatCourant] null : aucun état dans ce contrôle).
     * Règles : spec § 4.3, § 5 et § 6 ; décisions 4, 9, 10, 11 et 12 de Pascal (`fixtures/README.md`, Dictée).
     */
    fun appliquer(phrase: String, etatCourant: EtatDicte?): ResultatDictee {
        val r = analyser(phrase)
        if (r !is Accepte) return r
        val vivantOuMort = etatCourant == EtatDicte.SAIN || etatCourant == EtatDicte.ATTEINT || etatCourant == EtatDicte.MORT
        return when (val c = r.commande) {
            is Commande.Detail ->
                if (vivantOuMort) r else Refus("« mangé » demande un plant sain, atteint ou mort")
            is Commande.Cause ->
                if (etatCourant == EtatDicte.ATTEINT || etatCourant == EtatDicte.MORT) r
                else Refus("une cause demande un plant atteint ou mort")
            // Hauteur refusée sur un plant absent ou non observé (spec § 5) ; sans état, refus (décision de Pascal, 2026-10-24 : la voix demande d'abord l'état).
            is Commande.Hauteur ->
                if (vivantOuMort) r else Refus("hauteur refusée sans plant sain, atteint ou mort")
            is Commande.Regarni ->
                if (etatCourant == EtatDicte.MORT || etatCourant == EtatDicte.ABSENT) r
                else Refus("« remplace » demande un plant mort ou absent")
            is Commande.Etat -> when {
                etatCourant == null -> r
                // Redite : aucune ligne, la voix confirme, les mesures sont conservées (décision 11).
                etatCourant == c.etat -> Accepte(c, ligneAjoutee = false, confirmationVocale = true, mesuresConservees = true)
                // Correction annoncée (décisions 10 et 12) ; « non_observe » ne porte aucune mesure (spec § 5).
                else -> Accepte(
                    c,
                    correction = true,
                    annonceVocale = true,
                    mesuresConservees = if (c.etat == EtatDicte.NON_OBSERVE) false else null,
                )
            }
            else -> r
        }
    }
}
