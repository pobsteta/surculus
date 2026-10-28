/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.session.ActionGeste
import io.github.pobsteta.surculus.core.session.Geste
import io.github.pobsteta.surculus.core.session.GestesPlacette
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

    val tous: List<LongArray> = listOf(SAIN, ATTEINT, MORT, ANNULE, REFUS, TIC, BOURDON, ENTREE, DEJA_TERMINEE)
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

    val sansVoixFrancaise: Boolean get() = !moteur.francaisDisponible

    /** Dernière annonce écrite faute de voix : tenue ici, hors de Compose, pour traverser la rotation comme le mode poche. */
    var annonceEcrite: AnnoncePoche? = null
        private set

    fun effacer() { annonceEcrite = null }

    fun annoncer(a: AnnoncePoche) {
        a.motif?.let(::vibrer)
        val dit = moteur.francaisDisponible && try { moteur.dire(a.texte) } catch (_: Exception) { false }
        if (!dit) { annonceEcrite = a; surTexte?.invoke(a) }
    }

    /** Les signaux du classeur qui ne se disent pas : le tic (« atteint armé ») et le bourdon de sortie. */
    fun signal(sortie: SortieClasseur) {
        when (sortie) {
            SortieClasseur.Tic -> vibrer(MotifsVibration.TIC)
            SortieClasseur.Bourdon -> vibrer(MotifsVibration.BOURDON)
            is SortieClasseur.Geste, SortieClasseur.DeuxTouches -> {}
        }
    }

    private fun vibrer(motif: LongArray) {
        try { vibreur.vibrer(motif) } catch (_: Exception) { /* appareil sans vibreur : la voix et l'écran restent */ }
    }
}
