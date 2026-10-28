/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 012-D1, plan 012d-plan.md § 4 et § 5) ; aucun code repris de Marculus.
 */
package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.dictee.AnalyseurDictee
import io.github.pobsteta.surculus.core.dictee.Commande
import io.github.pobsteta.surculus.core.dictee.EtatDicte
import io.github.pobsteta.surculus.core.dictee.Grammaire
import io.github.pobsteta.surculus.core.dictee.ResultatDictee
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.session.IssueDictee.Raison

/**
 * Confronte une phrase dictée à l'état du plant courant puis la fait écrire par la [SessionControle]
 * (012-D1, plan § 4.2). Kotlin pur : aucune écoute, aucune voix, aucun écran.
 *
 * Aucune règle métier ici : le contexte est jugé par [AnalyseurDictee], le domaine par la session.
 * Décisions de Pascal (2026-10-28) : seul « suivant » avance (Q4) ; une correction d'état garde cause, détail
 * et hauteur quand le nouvel état les permet et dit ce qu'elle retire (Q5) ; une placette terminée reste
 * écrite, avec l'alerte avant l'annonce (Q9) ; textes de [TextesDictee] (Q10).
 *
 * Les méthodes de la session sont bloquantes (journal) : à appeler hors du fil principal, comme la session.
 */
object ApplicateurDictee {

    fun appliquer(phrase: String, session: SessionControle): IssueDictee {
        val avant = session.etat()
        val terminee = avant.termine
        val courant = avant.plants.firstOrNull { it.uuid == avant.courant }
        val observation = courant?.duJour
        val etat = etatDicte(observation)
        val alerte = if (terminee) TextesDictee.DEJA_TERMINEE else ""

        fun refus(raison: Raison, texte: String, r: SuiteSession.Refus? = null) = IssueDictee.Refusee(raison, alerte + texte, terminee, r)

        val analyse = AnalyseurDictee.analyser(phrase)
        if (analyse !is ResultatDictee.Accepte) {
            val p = AnalyseurDictee.normaliser(phrase)
            return if (p == Grammaire.MOT_HAUTEUR || p.startsWith(Grammaire.MOT_HAUTEUR + " ")) {
                refus(Raison.HAUTEUR_HORS_BORNES, TextesDictee.HAUTEUR_HORS_BORNES)
            } else {
                // Rien de compris : ce n'est pas une phrase de la grammaire, donc pas d'alerte de placette.
                IssueDictee.Refusee(Raison.PAS_COMPRIS, TextesDictee.PAS_COMPRIS, terminee)
            }
        }
        val commande = analyse.commande

        // Les commandes qui parlent du plant courant n'ont rien à dire s'il n'y en a plus.
        val parleDuPlant = commande !is Commande.Suivant && commande !is Commande.Annule &&
            commande !is Commande.VideDebut && commande !is Commande.VideFin
        if (parleDuPlant && courant == null) return refus(Raison.AUCUN_PLANT, TextesDictee.AUCUN_PLANT)

        // Le contexte : un refus de l'analyseur est traduit d'après l'état, jamais lu tel quel.
        val verdict = AnalyseurDictee.appliquer(phrase, etat)
        if (verdict !is ResultatDictee.Accepte) return refusContexte(commande, etat, ::refus)

        fun numeroDuCourant(): Int = session.etat().let { s -> s.plants.first { it.uuid == courant!!.uuid }.numero }

        fun ecrite(r: SuiteSession, retire: List<String> = emptyList(), texte: (Int) -> String): IssueDictee = when (r) {
            is SuiteSession.Fait -> {
                val n = numeroDuCourant()
                IssueDictee.Ecrite(n, r.lignes, verdict.correction, retire, alerte + texte(n), terminee)
            }
            else -> refusSession(r, ::refus)
        }

        return when (commande) {
            is Commande.Etat -> when {
                // Redite : aucune ligne (la session ne compare pas à l'état existant et écrirait un doublon).
                !verdict.ligneAjoutee -> numeroDuCourant().let { n ->
                    IssueDictee.Redite(n, alerte + TextesDictee.redite(n, commande.etat.code), terminee)
                }
                verdict.correction -> {
                    val (suite, retire) = corriger(session, commande.etat, observation)
                    ecrite(suite, retire) { TextesDictee.correction(it, commande.etat.code, retire) }
                }
                else -> ecrite(if (commande.etat == EtatDicte.NON_OBSERVE) session.nonObserve() else session.dire(commande.etat.code)) {
                    TextesDictee.etatDit(it, commande.etat.code)
                }
            }
            is Commande.Cause -> ecrite(session.preciser(causeGroupe = commande.causeGroupe)) { TextesDictee.causeDite(it, commande.causeGroupe) }
            is Commande.Detail -> ecrite(session.preciser(causeDetail = commande.causeDetail)) { TextesDictee.mange(it) }
            is Commande.Hauteur -> ecrite(session.preciser(hauteurCm = commande.hauteurCm)) { TextesDictee.hauteurDite(it, commande.hauteurCm) }
            Commande.Suivant -> {
                session.suivant()
                val s = session.etat()
                val n = s.plants.firstOrNull { it.uuid == s.courant }?.numero
                IssueDictee.Suivant(n, alerte + (n?.let { TextesDictee.suivant(it) } ?: TextesDictee.TOUS_DITS), terminee)
            }
            Commande.Regarni -> when (val r = session.remplace()) {
                is SuiteSession.Fait -> numeroDuCourant().let { n ->
                    IssueDictee.RemplaceEnAttente(n, alerte + TextesDictee.remplaceEnAttente(n), terminee)
                }
                else -> refusSession(r, ::refus)
            }
            Commande.Annule -> when (val r = session.annuler()) {
                is SuiteSession.Fait -> IssueDictee.Annulee(alerte + TextesDictee.ANNULE, terminee)
                else -> refusSession(r, ::refus)
            }
            Commande.VideDebut -> refusSession(session.vide(), ::refus)
            Commande.VideFin -> refusSession(session.finDuVide(), ::refus)
        }
    }

    private fun etatDicte(o: Observation?): EtatDicte? = when (o) {
        null -> null
        Observation.NonObserve -> EtatDicte.NON_OBSERVE
        is Observation.Observe -> EtatDicte.deCode(o.etat.code)
    }

    /**
     * Correction d'état (Q5) : une seule nouvelle ligne remplace l'ancienne ; elle garde cause, détail et hauteur déjà dits
     * quand le nouvel état les permet (mêmes règles que [Observations.valider], qui reste juge) et rend la liste de ce qui
     * est retiré. « absent » et « non observé » ne portent aucune mesure.
     */
    private fun corriger(session: SessionControle, nouveau: EtatDicte, ancienne: Observation?): Pair<SuiteSession, List<String>> {
        val ancien = ancienne as? Observation.Observe
        if (nouveau == EtatDicte.NON_OBSERVE) return session.nonObserve() to emptyList()
        if (ancien == null) return session.dire(nouveau.code) to emptyList()
        val porteCause = nouveau == EtatDicte.ATTEINT || nouveau == EtatDicte.MORT
        val cause = ancien.causeGroupe?.takeIf { porteCause }
        val detail = ancien.causeDetail?.takeIf {
            porteCause || (nouveau == EtatDicte.SAIN && it == Observations.DETAIL_ABROUTISSEMENT)
        }
        val hauteur = ancien.hauteurCm?.takeIf { nouveau != EtatDicte.ABSENT }
        val retire = buildList {
            if (ancien.causeGroupe != null && cause == null) add("cause")
            if (ancien.causeDetail != null && detail == null) add("détail")
            if (ancien.hauteurCm != null && hauteur == null) add("hauteur")
        }
        return session.dire(nouveau.code, cause?.code, detail, hauteur) to retire
    }

    private fun refusSession(r: SuiteSession, refus: (Raison, String, SuiteSession.Refus?) -> IssueDictee.Refusee): IssueDictee = when (r) {
        is SuiteSession.Refus -> refus(Raison.REFUS_SESSION, TextesDictee.REFUS_SESSION, r)
        SuiteSession.AucunPlantCourant -> refus(Raison.AUCUN_PLANT, TextesDictee.AUCUN_PLANT, null)
        SuiteSession.EtatDAbord -> refus(Raison.ETAT_D_ABORD, TextesDictee.ETAT_D_ABORD, null)
        SuiteSession.RienAAnnuler -> refus(Raison.RIEN_A_ANNULER, TextesDictee.RIEN_A_ANNULER, null)
        is SuiteSession.PasEncoreDisponible -> refus(Raison.VIDE, TextesDictee.VIDE, null)
        else -> refus(Raison.REFUS_SESSION, TextesDictee.REFUS_SESSION, null)
    }

    private fun refusContexte(
        commande: Commande,
        etat: EtatDicte?,
        refus: (Raison, String, SuiteSession.Refus?) -> IssueDictee.Refusee,
    ): IssueDictee = when (commande) {
        is Commande.Hauteur -> when (etat) {
            null -> refus(Raison.HAUTEUR_SANS_ETAT, TextesDictee.HAUTEUR_SANS_ETAT, null)
            EtatDicte.ABSENT -> refus(Raison.HAUTEUR_SUR_ABSENT, TextesDictee.HAUTEUR_ABSENT, null)
            else -> refus(Raison.HAUTEUR_SUR_NON_OBSERVE, TextesDictee.HAUTEUR_NON_OBSERVE, null)
        }
        is Commande.Cause -> when (etat) {
            null -> refus(Raison.ETAT_D_ABORD, TextesDictee.ETAT_D_ABORD, null)
            EtatDicte.ABSENT -> refus(Raison.CAUSE_SUR_ABSENT, TextesDictee.CAUSE_SUR_ABSENT, null)
            EtatDicte.NON_OBSERVE -> refus(Raison.CAUSE_SUR_NON_OBSERVE, TextesDictee.CAUSE_SUR_NON_OBSERVE, null)
            else -> refus(Raison.CAUSE_SUR_SAIN, TextesDictee.CAUSE_SUR_SAIN, null)
        }
        is Commande.Detail -> refus(Raison.DETAIL_REFUSE, TextesDictee.MANGE_REFUSE, null)
        Commande.Regarni -> refus(Raison.REMPLACE_REFUSE, TextesDictee.REMPLACE_REFUSE, null)
        else -> refus(Raison.PAS_COMPRIS, TextesDictee.PAS_COMPRIS, null)
    }
}
