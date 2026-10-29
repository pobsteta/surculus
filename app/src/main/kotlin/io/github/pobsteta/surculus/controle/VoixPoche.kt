/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.session.ActionGeste
import io.github.pobsteta.surculus.core.session.DelaisEcoute
import io.github.pobsteta.surculus.core.session.Geste
import io.github.pobsteta.surculus.core.session.GestesPlacette
import io.github.pobsteta.surculus.core.session.IssueDictee
import io.github.pobsteta.surculus.core.session.SortieClasseur
import io.github.pobsteta.surculus.core.session.SuiteSession
import java.util.Locale

/**
 * Les motifs de vibration du mode poche (mission 012-C2, plan § 8) : `createWaveform`, en millisecondes
 * (pause, vibre, pause, vibre…). Un motif par événement, tous distincts, tous de moins d'une seconde.
 */
object MotifsVibration {
    val SAIN = longArrayOf(0, 60)
    val ATTEINT = longArrayOf(0, 60, 80, 60)
    val MORT = longArrayOf(0, 400)
    val ANNULE = longArrayOf(0, 60, 80, 400)
    val REFUS = longArrayOf(0, 40, 40, 40, 40, 40)
    val TIC = longArrayOf(0, 30)
    val BOURDON = longArrayOf(0, 600)
    val ENTREE = longArrayOf(0, 100, 100, 100)
    val DEJA_TERMINEE = longArrayOf(0, 400, 100, 60)

    /** Le micro est ouvert (012-D2) : un coup, [DelaisEcoute.VIBRATION_OUVERTURE_MS]. */
    val OUVERTE = longArrayOf(0, DelaisEcoute.VIBRATION_OUVERTURE_MS)

    /** Le micro est fermé (012-D2) : deux coups de 200 ms. */
    val FERMEE = longArrayOf(0, 200, 150, 200)

    val tous: List<LongArray> = listOf(SAIN, ATTEINT, MORT, ANNULE, REFUS, TIC, BOURDON, ENTREE, DEJA_TERMINEE, OUVERTE, FERMEE)
}

/** Ce que le téléphone dit et fait sentir : le [texte] (voix, ou bande à défaut), le [motif] (`null` : pas de vibration). */
data class AnnoncePoche(val texte: String, val motif: LongArray?, val erreur: Boolean = false) {
    override fun equals(other: Any?) = other is AnnoncePoche && texte == other.texte && erreur == other.erreur && motif.contentEquals(other.motif)
    override fun hashCode() = texte.hashCode() * 31 + (motif?.contentHashCode() ?: 0)
}

/** Les textes exacts des annonces (plan § 7), fonctions pures. « Atteint » : la voix ne dit jamais « cause ». */
object AnnoncesPoche {
    const val BANDEAU_SANS_VOIX = "Pas de voix française : regardez l'écran."
    private const val DEJA_TERMINEE = "placette déjà terminée. "

    fun entree(numero: Int?) = AnnoncePoche(if (numero != null) "mode poche actif. Numéro $numero." else "mode poche actif.", MotifsVibration.ENTREE)

    /** Sortie : sans motif, pour ne pas couvrir le bourdon des 3 s. */
    fun sortie() = AnnoncePoche("mode poche terminé.", null)

    /** Retour d'appel ou de verrouillage : le mode poche est resté actif. */
    fun retour() = AnnoncePoche("mode poche actif.", MotifsVibration.ENTREE)

    /** Cinq minutes sans geste (une seule fois par période). */
    fun rappel() = AnnoncePoche("mode poche actif, 3 secondes sur volume haut pour sortir", MotifsVibration.ENTREE)

    fun deuxTouches() = AnnoncePoche("une touche à la fois.", MotifsVibration.REFUS, erreur = true)

    /**
     * L'annonce d'une phrase dictée (012-D2) : le texte est celui de l'`IssueDictee` (TextesDictee), la vibration celle de
     * l'état écrit ([etat], lu après l'écriture) ou du refus. Une confirmation sans écriture (redite, suivant, remplace) ne vibre pas. Après « Terminer », l'alerte passe d'abord, comme pour les gestes.
     */
    fun pourIssue(issue: IssueDictee, etat: EtatDsf?): AnnoncePoche = when (issue) {
        is IssueDictee.Refusee -> AnnoncePoche(issue.annonce, MotifsVibration.REFUS, erreur = true)
        is IssueDictee.Annulee -> AnnoncePoche(issue.annonce, MotifsVibration.ANNULE, erreur = issue.placetteTerminee)
        is IssueDictee.Ecrite -> AnnoncePoche(
            issue.annonce,
            if (issue.placetteTerminee) MotifsVibration.DEJA_TERMINEE else when (etat) {
                EtatDsf.SAIN -> MotifsVibration.SAIN
                EtatDsf.MORT -> MotifsVibration.MORT
                else -> MotifsVibration.ATTEINT
            },
            erreur = issue.placetteTerminee,
        )
        // Une phrase comprise qui n'écrit rien ne vibre pas : la voix suffit (décision de Pascal, 2026-10-28). Le tic de 30 ms
        // garde son seul sens, « atteint armé » au volume haut.
        is IssueDictee.Redite, is IssueDictee.Suivant, is IssueDictee.RemplaceEnAttente ->
            AnnoncePoche(issue.annonce, null, erreur = issue.placetteTerminee)
    }

    /**
     * L'annonce d'un geste. [numeroDit] : le plant courant avant le geste ; [numeroApres] : le plant courant après ;
     * [memePlant] : le courant n'a pas changé ; [termine] : la placette était déjà terminée (l'alerte passe d'abord).
     */
    fun pourSuite(geste: Geste, suite: SuiteSession, numeroDit: Int?, numeroApres: Int?, memePlant: Boolean, termine: Boolean): AnnoncePoche {
        val a = sansAlerte(geste, suite, numeroDit, numeroApres, memePlant)
        if (!termine) return a
        val texte = DEJA_TERMINEE + a.texte.replaceFirstChar { it.titlecase(Locale.FRANCE) }
        return AnnoncePoche(texte, if (suite is SuiteSession.Fait) MotifsVibration.DEJA_TERMINEE else a.motif, erreur = true)
    }

    private fun sansAlerte(geste: Geste, suite: SuiteSession, numeroDit: Int?, numeroApres: Int?, memePlant: Boolean): AnnoncePoche = when (suite) {
        is SuiteSession.Fait -> when (val action = GestesPlacette.action(geste)) {
            is ActionGeste.Dire -> {
                val etat = TextesControle.libelle(action.etat).lowercase(Locale.FRANCE)
                val dit = "numéro ${numeroDit ?: "?"}, $etat."
                val suivant = when {
                    !action.puisSuivant -> ""
                    numeroApres != null && !memePlant -> " Suivant, $numeroApres."
                    else -> " Tous les plants sont dits."
                }
                AnnoncePoche(dit + suivant, motifDe(action))
            }
            ActionGeste.AnnulerDerniereSaisie -> AnnoncePoche("annulé.", MotifsVibration.ANNULE) // la session ne dit pas quel plant est remis : pas de numéro inventé
            ActionGeste.OuvrirDictee -> dicteeAbsente()
        }
        SuiteSession.DicteeDemandee -> dicteeAbsente()
        SuiteSession.RienAAnnuler -> AnnoncePoche("rien à annuler.", MotifsVibration.REFUS)
        SuiteSession.AucunPlantCourant -> AnnoncePoche("plus de plant à dire.", MotifsVibration.REFUS, erreur = true)
        else -> AnnoncePoche("refusé. Regardez l'écran.", MotifsVibration.REFUS, erreur = true)
    }

    private fun dicteeAbsente() = AnnoncePoche("dictée pas encore disponible.", MotifsVibration.REFUS)

    private fun motifDe(action: ActionGeste.Dire): LongArray = when (action.etat) {
        io.github.pobsteta.surculus.core.placette.EtatDsf.SAIN -> MotifsVibration.SAIN
        io.github.pobsteta.surculus.core.placette.EtatDsf.MORT -> MotifsVibration.MORT
        else -> MotifsVibration.ATTEINT
    }
}

/** La voix du système (adaptateur : `VoixAndroid`). */
interface MoteurVoix {
    /** Une voix française est installée et prête. */
    val francaisDisponible: Boolean

    /** Dit [texte] en coupant la précédente ; `false` si la voix n'a pas pu parler. */
    fun dire(texte: String): Boolean

    /**
     * Appelé (sur le fil principal) quand la **dernière** annonce demandée est finie : dite jusqu'au bout, ou en échec.
     * Une annonce coupée par la suivante ou par [arreter] n'appelle rien. Mission 012-D2 : le micro se rouvre là (anti-larsen).
     */
    var surFin: (() -> Unit)?
        get() = null
        set(_) {}

    /** La voix parle encore (le garde-fou de 5 s n'ouvre pas le micro sous une longue annonce). */
    val parle: Boolean get() = false

    /** Coupe net l'annonce en cours, sans appeler [surFin]. */
    fun arreter() {}
}

/** Le vibreur du téléphone (adaptateur : `VoixAndroid`). */
interface Vibreur {
    fun vibrer(motif: LongArray)
}

/**
 * Voix et vibration du mode poche : jamais de silence. Sans voix française (ou voix en échec), le texte part dans la bande
 * ([surTexte]) ; la vibration reste. Sans vibreur, rien ne casse.
 */
class SortiesPoche(private val moteur: MoteurVoix, private val vibreur: Vibreur) {
    /** Reçoit le texte des annonces qui n'ont pas pu être dites. */
    var surTexte: ((AnnoncePoche) -> Unit)? = null

    /** Une annonce **dite** va parler : l'écoute coupe le micro avant (anti-larsen, 012-D2). Jamais appelé sans voix française. */
    var surDebutAnnonce: (() -> Unit)? = null

    /** Une annonce dite est finie (ou n'a pas pu l'être) : l'écoute rouvre le micro si la touche est tenue. */
    var surFinAnnonce: (() -> Unit)? = null

    init { moteur.surFin = { surFinAnnonce?.invoke() } }

    val parle: Boolean get() = moteur.parle

    /** Coupe la voix net (l'écoute s'ouvre : l'agent a décidé de parler). */
    fun taire() { try { moteur.arreter() } catch (_: Exception) { /* voix indisponible : rien à couper */ } }

    /** Une vibration seule (ouverture et fermeture du micro). */
    fun vibrer(motif: LongArray) = vibrerSansErreur(motif)

    val sansVoixFrancaise: Boolean get() = !moteur.francaisDisponible

    /** Dernière annonce écrite faute de voix : tenue ici, hors de Compose, pour traverser la rotation comme le mode poche. */
    var annonceEcrite: AnnoncePoche? = null
        private set

    fun effacer() { annonceEcrite = null }

    /**
     * Dit, vibre, ou écrit. [couperLeMicro] : avec une voix, l'écoute coupe le micro d'abord et le rouvre à la fin (une
     * annonce qui ne coupe pas le micro, comme « un instant », se dit sans ces rappels).
     */
    fun annoncer(a: AnnoncePoche, couperLeMicro: Boolean = true) {
        val voix = moteur.francaisDisponible
        if (voix && couperLeMicro) surDebutAnnonce?.invoke()
        a.motif?.let(::vibrerSansErreur)
        val dit = voix && try { moteur.dire(a.texte) } catch (_: Exception) { false }
        if (!dit) {
            annonceEcrite = a
            surTexte?.invoke(a)
            if (voix && couperLeMicro) surFinAnnonce?.invoke() // la voix a échoué : pas de fin à attendre
        }
    }

    /** Les signaux du classeur qui ne se disent pas : le tic (« atteint armé ») et le bourdon de sortie. */
    fun signal(sortie: SortieClasseur) {
        when (sortie) {
            SortieClasseur.Tic -> vibrerSansErreur(MotifsVibration.TIC)
            SortieClasseur.Bourdon -> vibrerSansErreur(MotifsVibration.BOURDON)
            is SortieClasseur.Geste, SortieClasseur.DeuxTouches, SortieClasseur.BasRelacheApresLong -> {}
        }
    }

    private fun vibrerSansErreur(motif: LongArray) {
        try { vibreur.vibrer(motif) } catch (_: Exception) { /* appareil sans vibreur : la voix et l'écran restent */ }
    }
}
