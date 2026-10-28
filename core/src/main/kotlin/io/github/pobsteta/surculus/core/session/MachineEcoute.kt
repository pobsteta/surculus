/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 012-D2, plan 012d-plan.md § 3 et § 8) ; le principe « tenir pour parler » et l'anti-larsen
 * sont ceux de Marculus (PttController, lu en lecture seule), le code est neuf.
 */
package io.github.pobsteta.surculus.core.session

/**
 * Les délais de l'écoute, en constantes nommées (décision de Pascal du 2026-10-28, question 11 : « tels quels, à mesurer
 * sur le téléphone »). Le délai « 10 s sans phrase » du plan n'existe plus : on tient pour parler.
 */
object DelaisEcoute {
    /** Si la fin d'annonce ne vient jamais (synthèse en échec), le micro se rouvre au bout de ce temps (plan § 8). */
    const val GARDE_FOU_ANNONCE_MS = 5_000L

    /** Le coup de vibration qui dit « le micro est ouvert » (plan § 3.3). */
    const val VIBRATION_OUVERTURE_MS = 200L

    /** Au-delà, le chargement du modèle se dit : « un instant » (plan § 3.3). */
    const val UN_INSTANT_MS = 2_000L

    /** Un bruit (ou `[unk]` seul) dans la seconde qui suit la vibration d'ouverture : le vibreur s'entend, on l'ignore (Q7). */
    const val BRUIT_APRES_OUVERTURE_MS = 1_000L

    /** Après un relâchement, Vosk rend son résultat final : on l'attend au plus ce temps. */
    const val RESULTAT_FINAL_MS = 5_000L
}

/** Où en est l'écoute. */
enum class EtatEcoute {
    /** Micro fermé. */
    FERMEE,

    /** Appui pris, le moteur charge puis ouvre le micro. */
    OUVERTURE,

    /** Micro ouvert : on parle. */
    OUVERTE,

    /** Micro coupé le temps d'une annonce (anti-larsen) ; rouvert à la fin si la touche est encore tenue. */
    SUSPENDUE,
}

/** Ce que la machine demande au monde extérieur (le moteur, le vibreur, la voix, l'applicateur). */
sealed interface EffetEcoute {
    /** Charger le modèle si besoin et ouvrir le micro ; [generation] revient avec `moteurPret`. Hors du fil principal. */
    data class Ouvrir(val generation: Int) : EffetEcoute

    /** Fermer le micro (Vosk rend alors son résultat final). */
    data object Fermer : EffetEcoute

    /** Le micro est réellement ouvert : un coup de [DelaisEcoute.VIBRATION_OUVERTURE_MS]. */
    data object VibrerOuverture : EffetEcoute

    /** Le micro vient d'être fermé par le relâchement : deux coups. */
    data object VibrerFermeture : EffetEcoute

    /** Le modèle met plus de [DelaisEcoute.UN_INSTANT_MS] à charger : la voix dit « un instant ». */
    data object DireUnInstant : EffetEcoute

    /** Une phrase à passer à `ApplicateurDictee`. */
    data class Appliquer(val phrase: String) : EffetEcoute
}

/**
 * L'écoute « tenir pour parler » (mission 012-D2), en fonction pure : l'appelant donne l'instant (ms, monotone) de chaque
 * événement et exécute les [EffetEcoute] rendus. Un seul fil (le fil principal).
 *
 * * [tenir] (volume bas tenu 0,5 s en mode poche, ou bouton micro appuyé) ouvre ; [relacher] ferme, et le résultat final de
 *   Vosk est attendu ([phrase]) ;
 * * fermée aussi par [arretForce] (sortie du mode poche, `onPause`/`onStop`, écran quitté) et [erreur] du moteur ; **jamais
 *   rouverte seule** : seule une fin d'annonce, touche encore tenue, rouvre ;
 * * anti-larsen : le micro est coupé pendant une annonce ([annonceDebut], ou une [phrase] avec voix) et rouvert à
 *   [annonceTerminee] si la touche est tenue ; garde-fou [DelaisEcoute.GARDE_FOU_ANNONCE_MS] si la fin ne vient pas.
 */
class MachineEcoute {
    var etat: EtatEcoute = EtatEcoute.FERMEE
        private set

    /** La touche (ou le bouton) est tenue. */
    var tenue: Boolean = false
        private set

    /** Numéro de la dernière ouverture demandée : un `moteurPret` d'une ouverture périmée est sans effet. */
    var generation: Int = 0
        private set

    private var debutOuvertureMs = 0L
    /** Instant de la dernière ouverture réelle du micro ; très ancien tant qu'aucune n'a eu lieu. */
    private var ouvertA = -DelaisEcoute.BRUIT_APRES_OUVERTURE_MS
    private var unInstantDit = false
    private var suspenduDepuisMs = 0L
    private var resultatAttenduJusqua: Long? = null

    fun tenir(instantMs: Long): List<EffetEcoute> {
        if (tenue) return emptyList() // répétition d'un appui tenu, ou touche tenue après une erreur
        tenue = true
        if (etat != EtatEcoute.FERMEE) return emptyList() // SUSPENDUE : la fin de l'annonce rouvrira
        return ouvrir(instantMs)
    }

    fun relacher(instantMs: Long): List<EffetEcoute> {
        if (!tenue) return emptyList()
        tenue = false
        return when (etat) {
            EtatEcoute.OUVERTE -> {
                etat = EtatEcoute.FERMEE
                resultatAttenduJusqua = instantMs + DelaisEcoute.RESULTAT_FINAL_MS
                listOf(EffetEcoute.Fermer, EffetEcoute.VibrerFermeture)
            }
            // L'annonce finit, le micro est déjà coupé : la fermeture se sent quand même (deux coups), l'agent a lâché.
            EtatEcoute.SUSPENDUE -> { etat = EtatEcoute.FERMEE; listOf(EffetEcoute.VibrerFermeture) }
            EtatEcoute.OUVERTURE, EtatEcoute.FERMEE -> emptyList() // OUVERTURE : se ferme dès que le moteur est prêt
        }
    }

    /** Le moteur a ouvert le micro (`demarrer` revenu sans erreur). */
    fun moteurPret(generation: Int, instantMs: Long): List<EffetEcoute> {
        if (generation != this.generation || etat != EtatEcoute.OUVERTURE) return emptyList()
        return if (tenue) {
            etat = EtatEcoute.OUVERTE
            ouvertA = instantMs
            listOf(EffetEcoute.VibrerOuverture)
        } else {
            // Lâché pendant le chargement : le micro s'ouvre puis se ferme sans rien dire.
            etat = EtatEcoute.FERMEE
            listOf(EffetEcoute.Fermer)
        }
    }

    /** Le moteur a échoué (micro pris, modèle illisible) : fermé, jamais rouvert seul. */
    fun erreur(@Suppress("UNUSED_PARAMETER") instantMs: Long): List<EffetEcoute> {
        resultatAttenduJusqua = null
        if (etat == EtatEcoute.FERMEE) return emptyList()
        etat = EtatEcoute.FERMEE
        return listOf(EffetEcoute.Fermer)
    }

    /**
     * Sortie du mode poche, `onPause`/`onStop`, écran quitté : tout se ferme, rien ne se rouvre. Si le micro était ouvert ou
     * suspendu, la fermeture se sent (deux coups). Le résultat final que Vosk rend encore est **jeté** : l'application quitte
     * le premier plan ou le mode poche se lève, une phrase ne doit plus écrire à ce moment.
     */
    fun arretForce(): List<EffetEcoute> {
        tenue = false
        resultatAttenduJusqua = null
        return when (etat) {
            EtatEcoute.FERMEE -> emptyList()
            EtatEcoute.OUVERTURE -> { etat = EtatEcoute.FERMEE; listOf(EffetEcoute.Fermer) }
            EtatEcoute.OUVERTE, EtatEcoute.SUSPENDUE -> { etat = EtatEcoute.FERMEE; listOf(EffetEcoute.Fermer, EffetEcoute.VibrerFermeture) }
        }
    }

    /**
     * Le moteur rend une phrase (au fil de l'écoute, ou le résultat final après un relâchement). Une phrase qui n'est
     * attendue par personne est ignorée. Un texte vide ou `[unk]` seul dans la seconde qui suit l'ouverture est ignoré (Q7) ;
     * hors de cette seconde, `[unk]` est transmis : l'applicateur dit « Pas compris. ».
     */
    fun phrase(texte: String, instantMs: Long, voixDisponible: Boolean): List<EffetEcoute> {
        val attendu = resultatAttenduJusqua?.let { instantMs <= it } == true
        if (etat != EtatEcoute.OUVERTE && !attendu) return emptyList()
        val brut = texte.trim()
        val bruit = brut.isEmpty() || brut == INCONNU
        // La seconde de bruit joue quel que soit l'état : un relâchement rapide rend son résultat final dans cette seconde.
        if (bruit && instantMs - ouvertA < DelaisEcoute.BRUIT_APRES_OUVERTURE_MS) { resultatAttenduJusqua = null; return emptyList() }
        if (brut.isEmpty()) return emptyList()
        if (etat != EtatEcoute.OUVERTE) resultatAttenduJusqua = null // le résultat final est arrivé
        if (voixDisponible && etat == EtatEcoute.OUVERTE) {
            etat = EtatEcoute.SUSPENDUE
            suspenduDepuisMs = instantMs
            return listOf(EffetEcoute.Fermer, EffetEcoute.Appliquer(texte))
        }
        return listOf(EffetEcoute.Appliquer(texte))
    }

    /** Une annonce va parler : le micro est coupé avant (anti-larsen). Sans effet si l'écoute est fermée. */
    fun annonceDebut(instantMs: Long): List<EffetEcoute> = when (etat) {
        EtatEcoute.OUVERTURE, EtatEcoute.OUVERTE -> {
            etat = EtatEcoute.SUSPENDUE
            suspenduDepuisMs = instantMs
            listOf(EffetEcoute.Fermer)
        }
        EtatEcoute.SUSPENDUE -> { suspenduDepuisMs = instantMs; emptyList() }
        EtatEcoute.FERMEE -> emptyList()
    }

    /** La voix a fini de parler : le micro se rouvre si la touche est encore tenue, sinon l'écoute est fermée. */
    fun annonceTerminee(instantMs: Long): List<EffetEcoute> {
        if (etat != EtatEcoute.SUSPENDUE) return emptyList()
        if (!tenue) { etat = EtatEcoute.FERMEE; return emptyList() }
        return ouvrir(instantMs)
    }

    /** Fait passer le temps : « un instant », garde-fou de l'annonce, fin de l'attente du résultat final. */
    fun avancer(instantMs: Long, voixParle: Boolean): List<EffetEcoute> {
        val effets = ArrayList<EffetEcoute>()
        resultatAttenduJusqua?.let { if (instantMs >= it) resultatAttenduJusqua = null }
        if (etat == EtatEcoute.OUVERTURE && !unInstantDit && instantMs - debutOuvertureMs >= DelaisEcoute.UN_INSTANT_MS) {
            unInstantDit = true
            effets += EffetEcoute.DireUnInstant
        }
        if (etat == EtatEcoute.SUSPENDUE && instantMs - suspenduDepuisMs >= DelaisEcoute.GARDE_FOU_ANNONCE_MS) {
            // Une longue annonce qui parle encore n'est pas une fin perdue : on attend (jamais de larsen).
            if (voixParle) suspenduDepuisMs = instantMs else effets += annonceTerminee(instantMs)
        }
        return effets
    }

    /** Prochain instant où [avancer] aura quelque chose à faire ; `null` : rien n'attend. */
    fun echeance(): Long? = listOfNotNull(
        if (etat == EtatEcoute.OUVERTURE && !unInstantDit) debutOuvertureMs + DelaisEcoute.UN_INSTANT_MS else null,
        if (etat == EtatEcoute.SUSPENDUE) suspenduDepuisMs + DelaisEcoute.GARDE_FOU_ANNONCE_MS else null,
        resultatAttenduJusqua,
    ).minOrNull()

    private fun ouvrir(instantMs: Long): List<EffetEcoute> {
        etat = EtatEcoute.OUVERTURE
        generation++
        debutOuvertureMs = instantMs
        unInstantDit = false
        resultatAttenduJusqua = null
        return listOf(EffetEcoute.Ouvrir(generation))
    }

    private companion object {
        const val INCONNU = "[unk]"
    }
}
