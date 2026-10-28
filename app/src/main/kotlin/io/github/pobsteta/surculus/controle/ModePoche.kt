/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.session.ClasseurGestes
import io.github.pobsteta.surculus.core.session.SortieClasseur
import io.github.pobsteta.surculus.core.session.Touche

/** Pourquoi le mode poche s'est levé. */
enum class RaisonSortie {
    /** Appui de 3 s sur volume haut (décision de Pascal du 2026-10-27). */
    TROIS_SECONDES,

    /** « Terminer » : la placette est finie (même décision). */
    TERMINER,

    /** L'application a quitté le premier plan (écran verrouillé, appel, autre application) : sortie par prudence. */
    ARRIERE_PLAN,

    /** Quinze minutes sans geste : garde-fou contre l'agent « enfermé » (décision de Pascal du 2026-10-28). */
    INACTIVITE,

    /** La vue Plant a été quittée (le regarni se pointe à l'écran, la boussole et la distance tapée l'exigent). */
    VUE_QUITTEE,
}

/**
 * Le mode poche (mission 012-C1, spec § 4.3, ADR 0004) : l'écran reste allumé, le toucher et le retour sont ignorés,
 * les touches de volume sont des gestes. État tenu **hors de Compose** (objet du processus, [ModePoche]) pour traverser la
 * rotation. Kotlin pur : l'horloge est donnée par l'appelant (millisecondes monotones).
 */
class ModePoche {
    var actif: Boolean = false
        private set
    private var derniereActiviteMs = 0L
    private var rappele = false
    private val abonnes = ArrayList<(Boolean, RaisonSortie?) -> Unit>()

    /** Écoute les changements : `(actif, raison)` ; la raison n'est donnée qu'à la sortie. Rend de quoi se désabonner. */
    fun abonner(ecoute: (Boolean, RaisonSortie?) -> Unit): () -> Unit {
        abonnes += ecoute
        return { abonnes -= ecoute }
    }

    fun entrer(instantMs: Long) {
        if (actif) return
        actif = true
        derniereActiviteMs = instantMs
        rappele = false
        abonnes.toList().forEach { it(true, null) }
    }

    fun sortir(raison: RaisonSortie) {
        if (!actif) return
        actif = false
        abonnes.toList().forEach { it(false, raison) }
    }

    /** Un geste vient d'être fait : l'inactivité repart de zéro. */
    fun activite(instantMs: Long) { derniereActiviteMs = instantMs; rappele = false }

    /** Cinq minutes sans geste : vrai **une seule fois** par période sans geste (012-C2), jamais hors du mode. */
    fun rappelDu(instantMs: Long): Boolean {
        if (!actif || rappele || instantMs - derniereActiviteMs < RAPPEL_MS) return false
        rappele = true
        return true
    }

    /** Instant du rappel vocal ; `null` hors du mode, ou si le rappel de cette période est fait. */
    fun echeanceRappelMs(): Long? = if (actif && !rappele) derniereActiviteMs + RAPPEL_MS else null

    /** Lève le mode si [INACTIVITE_MS] se sont écoulées sans geste. */
    fun verifier(instantMs: Long) {
        if (actif && instantMs - derniereActiviteMs >= INACTIVITE_MS) sortir(RaisonSortie.INACTIVITE)
    }

    /** L'Activity s'arrête : une rotation ([changementDeConfiguration]) ne lève rien, un vrai passage en arrière-plan lève le mode. */
    fun surArret(changementDeConfiguration: Boolean) {
        if (!changementDeConfiguration) sortir(RaisonSortie.ARRIERE_PLAN)
    }

    /** Instant où l'inactivité lèvera le mode ; `null` hors du mode. */
    fun echeanceInactiviteMs(): Long? = if (actif) derniereActiviteMs + INACTIVITE_MS else null

    companion object {
        const val INACTIVITE_MS = 15 * 60_000L

        /** Rappel vocal « mode poche actif » après cinq minutes sans geste (décision de Pascal du 2026-10-28). */
        const val RAPPEL_MS = 5 * 60_000L

        /** Le toucher est-il pris en compte ? Non, en mode poche. */
        fun toucherPris(actif: Boolean): Boolean = !actif

        /** Le retour système est-il pris en compte ? Non, en mode poche (une poche appuie sur « retour »). */
        fun retourPris(actif: Boolean): Boolean = !actif
    }
}

/**
 * Relie les touches de volume au classeur, en mode poche seulement (décision de Pascal du 2026-10-28 : hors du mode, le
 * volume règle le son). Kotlin pur ; l'Activity pose les événements et programme [echeance].
 */
class PontPoche(private val mode: ModePoche, private val classeur: ClasseurGestes = ClasseurGestes()) {
    /** Reçoit les gestes et les signaux (deux touches, tic) ; la vibration de la 012-C2 s'y branche. */
    var ecoute: ((SortieClasseur) -> Unit)? = null

    /** Cinq minutes sans geste : la voix rappelle que le mode poche est actif (012-C2). */
    var surRappel: (() -> Unit)? = null

    init {
        mode.abonner { actif, _ -> if (!actif) classeur.remettreAZero() }
    }

    /** Touches prises à l'appui : leurs répétitions et leur relâchement le sont aussi, même si le mode s'est levé entre-temps. */
    private val prises = HashSet<Touche>()

    /**
     * Une touche de volume bouge. Rend `true` si elle est prise (mode poche, ou touche déjà prise et pas encore relâchée),
     * `false` si le système la garde. [repetition] : appui répété d'une touche tenue (Android répète le `onKeyDown`).
     */
    fun touche(touche: Touche, appuyee: Boolean, instantMs: Long, repetition: Boolean = false): Boolean {
        if (repetition) return touche in prises
        if (appuyee) {
            if (!mode.actif) return false
            prises += touche
            traiter(classeur.appuye(touche, instantMs), instantMs)
            return true
        }
        if (touche !in prises) return false
        prises -= touche
        traiter(classeur.relache(touche, instantMs), instantMs)
        return true
    }

    /** Le temps passe : tics, sortie à 3 s, « mort » après la fenêtre, inactivité. */
    fun avancer(instantMs: Long) {
        if (!mode.actif) return
        traiter(classeur.avancer(instantMs), instantMs)
        mode.verifier(instantMs)
        if (mode.rappelDu(instantMs)) surRappel?.invoke()
    }

    /** Prochain instant où [avancer] aura quelque chose à faire ; `null` hors du mode poche. */
    fun echeance(): Long? {
        if (!mode.actif) return null
        return listOfNotNull(classeur.echeance(), mode.echeanceInactiviteMs(), mode.echeanceRappelMs()).minOrNull()
    }

    /** L'application perd le focus : un relâchement perdu ne doit rien laisser en attente. */
    fun remettreAZero() { classeur.remettreAZero(); prises.clear() }

    private fun traiter(sorties: List<SortieClasseur>, instantMs: Long) {
        for (s in sorties) {
            when (s) {
                is SortieClasseur.Geste -> mode.activite(instantMs)
                SortieClasseur.Bourdon -> mode.sortir(RaisonSortie.TROIS_SECONDES)
                SortieClasseur.Tic, SortieClasseur.DeuxTouches, SortieClasseur.BasRelacheApresLong -> {}
            }
            ecoute?.invoke(s)
        }
    }
}

/** Le mode poche et son pont, uniques dans le processus : l'Activity, qui reçoit les touches, et l'écran du contrôle s'y retrouvent. */
object PocheApp {
    val mode = ModePoche()
    val pont = PontPoche(mode)

    /** La voix et la vibration (012-C2) ; posées par l'Activity au démarrage, avant que le mode puisse s'ouvrir. */
    @Volatile var sorties: SortiesPoche? = null
        private set

    fun installer(contexte: android.content.Context) {
        if (sorties == null) sorties = SortiesPoche(VoixAndroid(contexte), VibreurAndroid(contexte))
    }
}
