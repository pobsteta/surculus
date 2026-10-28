/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

/** Les deux touches de volume. */
enum class Touche { HAUT, BAS }

/**
 * Les seuils des gestes, en millisecondes (décision de Pascal du 2026-10-28, « tels quels, à essayer » gants aux mains
 * à Couchey ; un échec les rouvre une fois).
 */
data class SeuilsGestes(
    /** À partir de là, un appui est long (comme le long appui du système et le push-to-talk de Marculus). */
    val longMs: Long = 500,
    /** Un appui de volume haut qui dure autant sort du mode poche (décision de Pascal du 2026-10-27). */
    val sortieMs: Long = 3_000,
    /** Fenêtre du double appui bas, et attente avant d'écrire un « mort » (décision de Pascal du 2026-10-28). */
    val doubleMs: Long = 350,
    /** Un appui qui revient moins de ce temps après le relâchement de la même touche est un rebond, ignoré. */
    val rebondMs: Long = 40,
)

/** Ce que le classeur rend à l'écran : un geste de la table, ou un signal à sentir. */
sealed interface SortieClasseur {
    /** Un geste de la table des boutons (spec § 4.3, [GestesPlacette]). */
    data class Geste(val geste: io.github.pobsteta.surculus.core.session.Geste) : SortieClasseur

    /** Volume haut tenu 0,5 s : « atteint » sera pris si l'on lâche ici (la vibration vient avec la 012-C2). */
    data object Tic : SortieClasseur

    /** Volume haut tenu 3 s : sortie du mode poche. Rien n'est écrit. */
    data object Bourdon : SortieClasseur

    /** Les deux touches ensemble : ignorées (raccourci d'accessibilité d'Android), rien n'est écrit. */
    data object DeuxTouches : SortieClasseur
}

/**
 * Classeur de gestes des boutons de volume (mission 012-C1), en Kotlin pur, horloge injectée : l'appelant donne l'instant
 * (en ms, monotone) de chaque événement, et appelle [avancer] quand [echeance] est atteinte.
 *
 * * volume haut : on agit **au relâchement** ; court (< 0,5 s) : sain ; long (de 0,5 s à moins de 3 s) : atteint, avec un
 *   [SortieClasseur.Tic] à 0,5 s ; 3 s : [SortieClasseur.Bourdon], le relâchement qui suit ne fait rien ;
 * * volume bas : long (0,5 s) pris tout de suite (dictée) ; court : écrit seulement **0,35 s après** le relâchement, pour
 *   qu'un double appui n'écrive jamais « mort » ; double : [Geste.VOLUME_BAS_DOUBLE_COURT] ;
 * * deux touches ensemble, et rebond (< 40 ms) : ignorés.
 */
class ClasseurGestes(private val seuils: SeuilsGestes = SeuilsGestes()) {
    private val enfoncees = HashSet<Touche>()
    private val dernierRelacheMs = HashMap<Touche, Long>()
    private var tenue: Touche? = null
    private var debutMs = 0L
    private var ticEmis = false
    private var longEmis = false
    private var sortieEmise = false
    /** Second appui d'un double : le relâchement qui suit n'est rien. */
    private var consomme = false
    /** Deux touches ensemble : rien ne compte jusqu'au relâchement complet. */
    private var conflit = false
    /** Instant du relâchement d'un premier appui bas court, dont le « mort » attend la fin de la fenêtre. */
    private var basEnAttenteMs: Long? = null

    fun appuye(touche: Touche, instantMs: Long): List<SortieClasseur> {
        val sorties = avancer(instantMs).toMutableList()
        if (touche in enfoncees) return sorties // répétition de l'appui tenu
        val dernier = dernierRelacheMs[touche]
        if (dernier != null && instantMs - dernier < seuils.rebondMs) return sorties // rebond
        if (enfoncees.isNotEmpty()) {
            enfoncees += touche
            conflit = true
            return sorties + SortieClasseur.DeuxTouches
        }
        enfoncees += touche
        tenue = touche
        debutMs = instantMs
        ticEmis = false; longEmis = false; sortieEmise = false; consomme = false
        val attente = basEnAttenteMs
        if (touche == Touche.BAS && attente != null) {
            basEnAttenteMs = null
            consomme = true
            sorties += SortieClasseur.Geste(Geste.VOLUME_BAS_DOUBLE_COURT)
        }
        return sorties
    }

    fun relache(touche: Touche, instantMs: Long): List<SortieClasseur> {
        val sorties = avancer(instantMs).toMutableList()
        dernierRelacheMs[touche] = instantMs
        if (touche !in enfoncees) return sorties
        enfoncees -= touche
        if (conflit) {
            if (enfoncees.isEmpty()) { conflit = false; tenue = null }
            return sorties
        }
        if (touche != tenue) return sorties
        tenue = null
        if (consomme) { consomme = false; return sorties }
        when (touche) {
            Touche.HAUT -> when {
                sortieEmise -> {}
                ticEmis -> sorties += SortieClasseur.Geste(Geste.VOLUME_HAUT_LONG)
                else -> sorties += SortieClasseur.Geste(Geste.VOLUME_HAUT_COURT)
            }
            Touche.BAS -> if (!longEmis) basEnAttenteMs = instantMs
        }
        return sorties
    }

    /** Fait passer le temps : rend ce qui est arrivé à échéance (tic, sortie, long, « mort » après la fenêtre). */
    fun avancer(instantMs: Long): List<SortieClasseur> {
        val sorties = ArrayList<SortieClasseur>()
        val attente = basEnAttenteMs
        if (attente != null && instantMs >= attente + seuils.doubleMs) {
            basEnAttenteMs = null
            sorties += SortieClasseur.Geste(Geste.VOLUME_BAS_COURT)
        }
        val k = tenue
        if (k != null && !conflit && !consomme) {
            val duree = instantMs - debutMs
            when (k) {
                Touche.HAUT -> {
                    if (!ticEmis && duree >= seuils.longMs) { ticEmis = true; sorties += SortieClasseur.Tic }
                    if (!sortieEmise && duree >= seuils.sortieMs) { sortieEmise = true; sorties += SortieClasseur.Bourdon }
                }
                Touche.BAS -> if (!longEmis && duree >= seuils.longMs) {
                    longEmis = true
                    sorties += SortieClasseur.Geste(Geste.VOLUME_BAS_LONG)
                }
            }
        }
        return sorties
    }

    /** Prochain instant où [avancer] aura quelque chose à rendre ; `null` : rien n'attend. */
    fun echeance(): Long? {
        val candidats = ArrayList<Long>()
        basEnAttenteMs?.let { candidats += it + seuils.doubleMs }
        val k = tenue
        if (k != null && !conflit && !consomme) {
            when (k) {
                Touche.HAUT -> when {
                    !ticEmis -> candidats += debutMs + seuils.longMs
                    !sortieEmise -> candidats += debutMs + seuils.sortieMs
                }
                Touche.BAS -> if (!longEmis) candidats += debutMs + seuils.longMs
            }
        }
        return candidats.minOrNull()
    }

    /** Oublie tout (l'application perd le focus, le mode poche se lève) : un relâchement perdu ne laisse rien en attente. */
    fun remettreAZero() {
        enfoncees.clear(); dernierRelacheMs.clear()
        tenue = null; ticEmis = false; longEmis = false; sortieEmise = false; consomme = false; conflit = false
        basEnAttenteMs = null
    }
}
