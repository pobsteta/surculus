/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Écrit pour Surculus (mission 012-D2, plan 012d-plan.md § 3, § 6, § 7 et § 8) ; le principe est celui de Marculus
 * (PttController, DicteeVocale : lecture seule), le code est neuf.
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.session.EffetEcoute
import io.github.pobsteta.surculus.core.session.EtatEcoute
import io.github.pobsteta.surculus.core.session.MachineEcoute

/** Où en est la permission `RECORD_AUDIO` (plan § 6). */
enum class EtatPermissionMicro {
    ACCORDEE,

    /** Jamais refusée : la demande se fait à l'appui sur « Mode poche » ou sur le micro, l'écran sous les yeux. */
    A_DEMANDER,

    /** Refusée une fois : le prochain appui sur le micro redemande. */
    REFUSEE,

    /** Refusée pour de bon (la fenêtre du système ne s'ouvre plus) : il faut passer par les réglages de l'application. */
    REFUSEE_DEFINITIVEMENT,
}

/** La permission du micro : fonctions pures (la fenêtre du système, elle, ne se teste qu'au téléphone). */
object PermissionMicro {
    /** Après une demande : refus définitif quand la fenêtre ne propose plus de redemander (`shouldShowRequestPermissionRationale` faux). */
    fun apresDemande(accordee: Boolean, rationale: Boolean): EtatPermissionMicro = when {
        accordee -> EtatPermissionMicro.ACCORDEE
        rationale -> EtatPermissionMicro.REFUSEE
        else -> EtatPermissionMicro.REFUSEE_DEFINITIVEMENT
    }

    /** Faut-il ouvrir la fenêtre du système ? Pas en refus définitif (elle ne s'ouvrirait pas) ni une fois accordée. */
    fun aDemander(etat: EtatPermissionMicro): Boolean = etat == EtatPermissionMicro.A_DEMANDER || etat == EtatPermissionMicro.REFUSEE

    /** Le repère vers les réglages de l'application, dit en toutes lettres. */
    const val REGLAGES = "Réglages du téléphone, applications, Surculus, autorisations."

    /** Ce que l'écran dit de la permission ; `null` : rien à dire (accordée, ou pas encore demandée). */
    fun ligne(etat: EtatPermissionMicro): String? = when (etat) {
        EtatPermissionMicro.ACCORDEE, EtatPermissionMicro.A_DEMANDER -> null
        EtatPermissionMicro.REFUSEE -> "Micro refusé : la dictée est indisponible, le volume marche."
        EtatPermissionMicro.REFUSEE_DEFINITIVEMENT -> "Micro refusé pour de bon : $REGLAGES"
    }
}

/** Ce que la bande montre de l'écoute : une seule chose en grand (plan § 2). */
enum class AffichageEcoute {
    /** Rien n'a été écouté depuis l'entrée dans l'écran : la bande ne dit rien du micro. */
    RIEN,
    OUVERTURE,
    ECOUTE,
    FERMEE,
}

/** Les mots de l'écoute, à l'écran et à la voix : fonctions pures, un seul endroit. */
object TextesEcoute {
    const val UN_INSTANT = "un instant."
    const val J_ECOUTE_OUVERTURE = "J'ÉCOUTE…"
    const val J_ECOUTE = "J'ÉCOUTE"
    const val ECOUTE_FERMEE = "ÉCOUTE FERMÉE"
    const val MODELE_ABSENT_LIGNE = "Dictée non installée : Paramètres, Dictée vocale."
    const val MODELE_ABSENT_VOIX = "Dictée non installée. Paramètres, dictée vocale."
    const val MICRO_REFUSE_VOIX = "Dictée indisponible : micro refusé."
    const val ERREUR_VOIX = "Dictée impossible : micro pris ou modèle illisible. Rechargez le modèle dans Paramètres, Dictée vocale."
    const val BRUIT = "(bruit)"
    const val DICTEE_MODELE_ILLISIBLE_ECRAN = "Dictée impossible : micro pris ou modèle illisible. Rechargez-le dans Paramètres, Dictée vocale."

    fun grand(a: AffichageEcoute): String? = when (a) {
        AffichageEcoute.RIEN -> null
        AffichageEcoute.OUVERTURE -> J_ECOUTE_OUVERTURE
        AffichageEcoute.ECOUTE -> J_ECOUTE
        AffichageEcoute.FERMEE -> ECOUTE_FERMEE
    }

    /** « entendu : « atteint » » ; un `[unk]` seul est du bruit, pas un mot. */
    fun entendu(phrase: String): String = "entendu : « ${if (phrase.trim() == "[unk]") BRUIT else phrase.trim()} »"

    /** « compris : Numéro 7, atteint. » ou, en erreur, « refusé : Pas compris. » (le même texte que la voix). */
    fun compris(texte: String, erreur: Boolean): String = (if (erreur) "refusé : " else "compris : ") + texte

    /** Le refus de départ (micro refusé, modèle absent) : à la voix. */
    fun refusVoix(permission: EtatPermissionMicro, modeleInstalle: Boolean): String? = when {
        !modeleInstalle -> MODELE_ABSENT_VOIX
        permission == EtatPermissionMicro.REFUSEE_DEFINITIVEMENT -> "$MICRO_REFUSE_VOIX ${PermissionMicro.REGLAGES}"
        permission == EtatPermissionMicro.REFUSEE || permission == EtatPermissionMicro.A_DEMANDER -> MICRO_REFUSE_VOIX
        else -> null
    }

    /** La ligne d'état de la dictée à l'écran (bande, ou sous le bouton micro) ; `null` : tout va bien. */
    fun ligneEtat(permission: EtatPermissionMicro, modeleInstalle: Boolean): String? =
        if (!modeleInstalle) MODELE_ABSENT_LIGNE else PermissionMicro.ligne(permission)
}

/** Le bouton plein (couleurs inversées) pendant l'écoute : c'est la **seule** chose qui change sur le bouton micro. */
object AspectMicro {
    fun plein(a: AffichageEcoute): Boolean = a == AffichageEcoute.ECOUTE || a == AffichageEcoute.OUVERTURE
}

/** Les boutons de réparation sous le message de la dictée (hors du mode poche seulement). */
enum class ActionDictee {
    /** Réglages Android de l'application : le micro est refusé pour de bon. */
    REGLAGES,

    /** Paramètres, Dictée vocale : le modèle manque. */
    INSTALLER,
}

object ActionsDictee {
    fun pour(permission: EtatPermissionMicro, modeleInstalle: Boolean): List<ActionDictee> = buildList {
        if (!modeleInstalle) add(ActionDictee.INSTALLER)
        if (permission == EtatPermissionMicro.REFUSEE_DEFINITIVEMENT) add(ActionDictee.REGLAGES)
    }
}

/** Le micro vu du pilote : ouvre (hors du fil principal, le chargement du modèle est long) et ferme. */
interface PortMicro {
    /** Charge le modèle si besoin et ouvre le micro. Rend la main tout de suite ; la suite revient par `moteurPret`, `phrase` ou `erreur`. */
    fun ouvrir(generation: Int)

    /** Ferme le micro ; le moteur rend alors son résultat final par `phrase`. */
    fun fermer()
}

/**
 * Le pilote de l'écoute (mission 012-D2) : relie la [MachineEcoute] (fonction pure) au micro, à la voix et au vibreur, et tient
 * ce que la bande montre. Kotlin pur, horloge injectée, un seul fil (le fil principal) : testé sans téléphone. Le câblage
 * Android est dans `EcouteApp`.
 */
class PilotageEcoute(
    private val micro: PortMicro,
    private val sorties: SortiesPoche,
    private val horloge: () -> Long,
    private val modeleInstalle: () -> Boolean,
    private val permission: () -> EtatPermissionMicro,
    private val machine: MachineEcoute = MachineEcoute(),
) {
    /** L'écran prend la phrase, l'applique (hors du fil principal) puis rend l'annonce par [resultat]. `null` : pas d'écran, pas d'écoute. */
    var surPhrase: ((String) -> Unit)? = null

    /** Quelque chose a changé pour la bande ou l'échéance. */
    var surChangement: (() -> Unit)? = null

    var affichage: AffichageEcoute = AffichageEcoute.RIEN
        private set
    var entendu: String? = null
        private set
    var compris: String? = null
        private set
    var comprisErreur: Boolean = false
        private set

    val etat: EtatEcoute get() = machine.etat

    init {
        sorties.surDebutAnnonce = { executer(machine.annonceDebut(horloge())) }
        sorties.surFinAnnonce = { executer(machine.annonceTerminee(horloge())) }
    }

    /** Appui long sur volume bas, ou bouton micro appuyé. Rend `false` si le micro ne s'ouvre pas (dit à la voix et à l'écran). */
    fun tenir(): Boolean {
        if (surPhrase == null) return false
        val refus = TextesEcoute.refusVoix(permission(), modeleInstalle())
        if (refus != null) {
            entendu = null
            compris = TextesEcoute.ligneEtat(permission(), modeleInstalle()) ?: refus
            comprisErreur = true
            sorties.annoncer(AnnoncePoche(refus, MotifsVibration.REFUS, erreur = true), couperLeMicro = false)
            surChangement?.invoke()
            return false
        }
        entendu = null; compris = null; comprisErreur = false
        executer(machine.tenir(horloge()))
        return true
    }

    /** Relâchement du volume bas (après un long) ou du bouton micro. */
    fun relacher() = executer(machine.relacher(horloge()))

    /** Le moteur a ouvert le micro. */
    fun moteurPret(generation: Int) = executer(machine.moteurPret(generation, horloge()))

    /** Le moteur rend une phrase (en cours d'écoute, ou son résultat final après le relâchement). */
    fun phrase(texte: String) = executer(machine.phrase(texte, horloge(), voixDisponible = !sorties.sansVoixFrancaise))

    /** Le moteur a échoué : fermé, jamais rouvert seul, et on le dit. */
    fun erreur() {
        executer(machine.erreur(horloge()))
        entendu = null
        compris = TextesEcoute.DICTEE_MODELE_ILLISIBLE_ECRAN
        comprisErreur = true
        sorties.annoncer(AnnoncePoche(TextesEcoute.ERREUR_VOIX, MotifsVibration.REFUS, erreur = true), couperLeMicro = false)
        surChangement?.invoke()
    }

    /** L'écran a appliqué la phrase : montre ce qui est compris (ou refusé), et le dit. Le micro est déjà coupé s'il y a une voix. */
    fun resultat(annonce: AnnoncePoche) {
        compris = annonce.texte
        comprisErreur = annonce.erreur
        surChangement?.invoke()
        sorties.annoncer(annonce)
    }

    /** Un geste de volume prend la bande : les lignes de la dictée s'effacent. */
    fun effacerLignes() {
        if (entendu == null && compris == null) return
        entendu = null; compris = null; comprisErreur = false
        surChangement?.invoke()
    }

    /** Sortie du mode poche, `onPause`/`onStop`, écran quitté : le micro se ferme, rien ne se rouvre. */
    fun arreter() = executer(machine.arretForce())

    /** Comme [arreter], et la bande oublie tout (le mode poche est levé, ou l'écran quitté). */
    fun arreterEtOublier() {
        arreter()
        affichage = AffichageEcoute.RIEN
        entendu = null; compris = null; comprisErreur = false
        surChangement?.invoke()
    }

    /** Le temps passe : « un instant », garde-fou de l'annonce, fin d'attente du résultat final. */
    fun avancer() = executer(machine.avancer(horloge(), sorties.parle))

    /** Prochain instant où [avancer] aura quelque chose à faire ; `null` : rien n'attend. */
    fun echeance(): Long? = machine.echeance()

    private fun executer(effets: List<EffetEcoute>) {
        for (e in effets) when (e) {
            is EffetEcoute.Ouvrir -> { sorties.taire(); micro.ouvrir(e.generation) }
            EffetEcoute.Fermer -> micro.fermer()
            EffetEcoute.VibrerOuverture -> sorties.vibrer(MotifsVibration.OUVERTE)
            EffetEcoute.VibrerFermeture -> sorties.vibrer(MotifsVibration.FERMEE)
            EffetEcoute.DireUnInstant -> sorties.annoncer(AnnoncePoche(TextesEcoute.UN_INSTANT, null), couperLeMicro = false)
            is EffetEcoute.Appliquer -> {
                entendu = TextesEcoute.entendu(e.phrase)
                compris = null; comprisErreur = false
                surPhrase?.invoke(e.phrase)
            }
        }
        affichage = when (machine.etat) {
            EtatEcoute.OUVERTURE -> AffichageEcoute.OUVERTURE
            EtatEcoute.OUVERTE -> AffichageEcoute.ECOUTE
            EtatEcoute.FERMEE, EtatEcoute.SUSPENDUE -> if (affichage == AffichageEcoute.RIEN) AffichageEcoute.RIEN else AffichageEcoute.FERMEE
        }
        surChangement?.invoke()
    }
}
