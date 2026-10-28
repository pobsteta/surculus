/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.GroupeCause
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.session.EtatSession
import io.github.pobsteta.surculus.core.session.PlantSession
import io.github.pobsteta.surculus.core.session.SessionControle
import io.github.pobsteta.surculus.core.session.SuiteSession

/** Les vues de l'écran du contrôle (règle du 1664 : une idée par vue). */
enum class VueControle {
    /** Le plant courant et son état. */
    PLANT,

    /** Confirmer la fin avec les totaux. */
    TERMINER,

    /** La placette vient d'être terminée : « Contrôlée ». */
    TERMINEE,
}

/** Message à l'écran : une information, ou un refus (en couleur d'erreur, avec son code quand il en a un). */
data class MessageControle(val texte: String, val erreur: Boolean)

/** Ce que l'écran montre du plant courant, en Kotlin pur (testé sans Compose). */
data class VuePlant(
    /** « Plant n° 7 », « Tous les plants sont dits » ou « Aucun plant ». */
    val titre: String,
    val numero: Int?,
    /** « 7 sur 12 ». */
    val compteur: String,
    /** État dit aujourd'hui ; `null` : « pas encore dit ». */
    val etatDuJour: EtatDsf?,
    /** « non observé » (pas un état du DSF) : l'état n'est pas dit, mais le plant n'attend plus. */
    val nonObserve: Boolean,
    val groupeCause: GroupeCause?,
    /** « pas encore dit », ou le libellé de l'état du jour. */
    val libelleEtat: String,
    /** « avant : atteint », ou `null` sans état précédent. */
    val precedent: String?,
    /** Les groupes de cause s'offrent sur un plant atteint ou mort. */
    val causesVisibles: Boolean,
    val alerteTerminee: Boolean,
    val aucunPlant: Boolean,
    val peutTerminer: Boolean,
    val aPrecedent: Boolean,
    val aSuivant: Boolean,
)

/** Totaux à confirmer à la fin : « 12 plants · 9 vivants · 2 abroutis · hauteur moyenne 78 cm ». */
data class VueTotaux(val texte: String, val refus: String?)

/** Tout ce que l'écran lit : fonction pure de l'état de la session, de la vue et du message. */
data class EcranControle(
    val codePlacette: String,
    val jour: String,
    val vue: VueControle,
    val plant: VuePlant,
    val totaux: VueTotaux,
    val message: MessageControle?,
)

/**
 * Pilote de l'écran du contrôle (mission 012-B1) : traduit un appui en commande de la [SessionControle] et rend ce que
 * l'écran montre. Aucune règle ici : états, causes, refus, « terminer » viennent de la session (012-A) ; ce pilote ne
 * garde que la vue ouverte et le dernier message. Méthodes bloquantes (journal) : hors du fil principal.
 */
class ControleurPlacette(private val session: SessionControle) {
    private var vue = VueControle.PLANT
    private var message: MessageControle? = null

    fun instantane(): EcranControle = TextesControle.ecran(session.etat(), vue, message)

    /** Dit l'état du plant courant. Sain et absent n'ont pas de cause : on passe au plant suivant (comme les boutons de volume, spec § 4.3). */
    fun dire(etat: EtatDsf): EcranControle = agir {
        val suite = session.dire(etat.code)
        if (suite is SuiteSession.Fait && (etat == EtatDsf.SAIN || etat == EtatDsf.ABSENT)) session.suivant()
        suite
    }

    /** Cause du plant atteint ou mort, juste après son état, puis plant suivant. */
    fun cause(groupe: GroupeCause): EcranControle = agir {
        val suite = session.preciser(causeGroupe = groupe.code)
        if (suite is SuiteSession.Fait) session.suivant()
        suite
    }

    /** Plant atteint ou mort sans cause : plant suivant. */
    fun sansCause(): EcranControle = agir { session.suivant(); SuiteSession.Fait() }

    fun plantSuivant(): EcranControle = agir {
        val e = session.etat()
        val i = e.plants.indexOfFirst { it.uuid == e.courant }
        e.plants.getOrNull(i + 1)?.let { session.choisir(it.uuid) }
        SuiteSession.Fait()
    }

    fun plantPrecedent(): EcranControle = agir {
        val e = session.etat()
        val i = e.plants.indexOfFirst { it.uuid == e.courant }
        val avant = if (i < 0) e.plants.lastOrNull() else e.plants.getOrNull(i - 1)
        avant?.let { session.choisir(it.uuid) }
        SuiteSession.Fait()
    }

    fun annuler(): EcranControle = agir { session.annuler() }

    /** Ouvre la vue de confirmation ; sans état dit, la session refuse (`RienASaisir`) et l'écran le dit. */
    fun demanderTerminer(): EcranControle {
        val e = session.etat()
        if (e.plants.none { it.duJour != null }) {
            message = TextesControle.message(SuiteSession.RienASaisir)
            vue = VueControle.PLANT
        } else {
            message = null
            vue = VueControle.TERMINER
        }
        return instantane()
    }

    fun terminer(): EcranControle {
        when (val suite = session.terminer()) {
            is SuiteSession.Fait -> { vue = VueControle.TERMINEE; message = null }
            else -> { vue = VueControle.PLANT; message = TextesControle.message(suite) }
        }
        return instantane()
    }

    /** Revient à la vue Plant (« Reprendre »). */
    fun reprendre(): EcranControle { vue = VueControle.PLANT; message = null; return instantane() }

    private fun agir(commande: () -> SuiteSession): EcranControle {
        message = TextesControle.message(commande())
        return instantane()
    }
}

/** Les mots de l'écran et la lecture de l'état de la session : fonctions pures. */
object TextesControle {
    fun libelle(etat: EtatDsf): String = when (etat) {
        EtatDsf.SAIN -> "Sain"
        EtatDsf.ATTEINT -> "Atteint"
        EtatDsf.MORT -> "Mort"
        EtatDsf.ABSENT -> "Absent"
    }

    /** Libellés courts des quatre groupes de cause du DSF (`etats-plants.yaml`, `causes_dsf_groupes_bilan`). */
    fun libelle(groupe: GroupeCause): String = when (groupe) {
        GroupeCause.ENTOMOLOGIQUE -> "Insectes"
        GroupeCause.PATHOLOGIQUE -> "Champignons"
        GroupeCause.ANIMAUX -> "Animaux"
        GroupeCause.ABIOTIQUE_COMPLEXE_INDETERMINE -> "Autre ou inconnu"
    }

    const val PAS_ENCORE_DIT = "pas encore dit"
    const val NON_OBSERVE = "non observé"

    fun compteur(dits: Int, total: Int) = "$dits sur $total"

    /** Le refus avec son code (spec § 6) d'abord, puis l'explication ; les réponses sans code, en clair. */
    fun message(suite: SuiteSession): MessageControle? = when (suite) {
        is SuiteSession.Fait -> null
        is SuiteSession.Refus -> MessageControle("${suite.code} : ${suite.message}", erreur = true)
        is SuiteSession.AConfirmer -> MessageControle(suite.alerte.message, erreur = true)
        is SuiteSession.PasEncoreDisponible -> MessageControle("« ${suite.fonction} » : pas encore disponible.", erreur = false)
        SuiteSession.AucunPlantCourant -> MessageControle("Aucun plant à qui dire un état.", erreur = true)
        SuiteSession.EtatDAbord -> MessageControle("L'état d'abord : dites l'état du plant avant sa cause.", erreur = true)
        SuiteSession.RienAAnnuler -> MessageControle("Rien à annuler.", erreur = false)
        SuiteSession.RienASaisir -> MessageControle("Aucun état dit aujourd'hui : rien à terminer.", erreur = true)
        SuiteSession.DejaTerminee -> MessageControle("Placette déjà terminée aujourd'hui.", erreur = true)
        SuiteSession.DicteeDemandee -> MessageControle("La dictée n'est pas encore disponible.", erreur = false)
    }

    fun ecran(e: EtatSession, vue: VueControle, message: MessageControle?): EcranControle =
        EcranControle(e.codePlacette, e.jour.toString(), vue, plant(e), totaux(e.totaux), message)

    fun plant(e: EtatSession): VuePlant {
        val courant: PlantSession? = e.plants.firstOrNull { it.uuid == e.courant }
        val i = e.plants.indexOfFirst { it.uuid == e.courant }
        val duJour = courant?.duJour
        val observe = duJour as? Observation.Observe
        val titre = when {
            e.plants.isEmpty() -> "Aucun plant"
            courant == null -> "Tous les plants sont dits"
            else -> "Plant n° ${courant.numero}"
        }
        return VuePlant(
            titre = titre,
            numero = courant?.numero,
            compteur = compteur(e.nbDits, e.nbPlants),
            etatDuJour = observe?.etat,
            nonObserve = duJour is Observation.NonObserve,
            groupeCause = observe?.causeGroupe,
            libelleEtat = when {
                observe != null -> libelle(observe.etat)
                duJour is Observation.NonObserve -> NON_OBSERVE
                else -> PAS_ENCORE_DIT
            },
            precedent = courant?.precedent?.let { "avant : " + libellePrecedent(it) },
            causesVisibles = observe != null && (observe.etat == EtatDsf.ATTEINT || observe.etat == EtatDsf.MORT),
            alerteTerminee = e.termine,
            aucunPlant = e.plants.isEmpty(),
            peutTerminer = e.plants.any { it.duJour != null },
            aPrecedent = e.plants.isNotEmpty() && i != 0,
            aSuivant = e.plants.isNotEmpty() && i != e.plants.lastIndex,
        )
    }

    private fun libellePrecedent(o: Observation): String = when (o) {
        is Observation.Observe -> libelle(o.etat).lowercase()
        Observation.NonObserve -> NON_OBSERVE
    }

    fun totaux(v: Verdict<Totaux>): VueTotaux = when (v) {
        is Verdict.Accepte -> {
            val t = v.valeur
            val base = "${t.nbTotal} ${if (t.nbTotal > 1) "plants" else "plant"} · ${t.nbVivants} ${if (t.nbVivants > 1) "vivants" else "vivant"} · " +
                "${t.nbAbroutis} ${if (t.nbAbroutis > 1) "abroutis" else "abrouti"}"
            VueTotaux(t.hMoyCm?.let { "$base · hauteur moyenne $it cm" } ?: base, null)
        }
        is Verdict.Refuse -> VueTotaux("", v.refus.message)
        is Verdict.AConfirmer -> VueTotaux("", v.alerte.message)
    }
}
