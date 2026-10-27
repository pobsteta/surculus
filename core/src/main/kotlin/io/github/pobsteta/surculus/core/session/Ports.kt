/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

/** Dernier fix GNSS connu : son heure (ms Unix UTC) et l'instant monotone (ms) où il a été reçu. */
data class FixSession(val heureUtcMs: Long, val monotoneMs: Long)

/** Horloges de l'appareil, injectées pour que les tests rejouent minuit, un fix périmé ou un téléphone déréglé. */
interface HorlogeSession {
    /** Heure du téléphone, ms Unix UTC. */
    fun maintenantUtcMs(): Long

    /** Horloge monotone (`SystemClock.elapsedRealtime`), ms. L'âge du fix se mesure dessus. */
    fun monotoneMs(): Long

    /** Dernier fix GNSS connu, `null` s'il n'y en a pas. */
    fun fix(): FixSession?
}

/**
 * Une ligne **courante** du journal de la tournée ouverte : tête de sa clé (tournée, objet, type), non annulée,
 * hors lignes d'annulation. La liste est dans l'**ordre total** du journal.
 */
data class LigneCourante(
    val uuid: String,
    val objetUuid: String,
    val type: String,
    val valeur: String?,
    val remplaceUuid: String?,
)

/**
 * Port du journal de la **tournée ouverte** : le code de la tournée est lié à l'instance (il n'est jamais un
 * paramètre de chaque appel, pour qu'on ne puisse pas l'oublier et lire la tournée vide `""`, relecture de la 032).
 * Les méthodes sont bloquantes : à appeler hors du fil principal.
 */
interface JournalControle {
    /** Nouvel identifiant (UUID v7 sur l'appareil) pour un objet : contrôle, plant, ligne d'état. */
    fun nouvelUuid(): String

    /** Ajoute une saisie ; rend l'`uuid` de la ligne écrite. */
    fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?): String

    /** Annule une ligne (la valeur remplacée redevient courante). */
    fun annuler(uuidCible: String, operateur: String)

    /** Lignes courantes de la tournée, dans l'ordre total. */
    fun lignesCourantes(): List<LigneCourante>
}

/** Ce que la session dit à la sauvegarde (spec § 4.8) : chaque ligne écrite est une saisie, « terminer » une fin de placette. */
interface EcouteSauvegarde {
    fun surSaisie()

    fun surFinPlacette()

    companion object {
        val AUCUNE = object : EcouteSauvegarde {
            override fun surSaisie() = Unit
            override fun surFinPlacette() = Unit
        }
    }
}

/**
 * Placettes « Contrôlées » d'une tournée (décisions de Pascal du 2026-10-27) : celles dont un contrôle est
 * **terminé** et non annulé. Une placette terminée reste contrôlée pour toute la tournée, même les jours suivants ;
 * une placette commencée mais non terminée, ou dont la fin est annulée, reste « à faire ».
 */
object PlacettesControlees {
    /** `uuid` des placettes de la tournée dont un contrôle courant porte `termine = true`. */
    fun de(lignes: List<LigneCourante>): Set<String> =
        lignes.asSequence()
            .filter { it.type == FormatValeurs.TYPE_CONTROLE }
            .mapNotNull { ValeurControle.lire(it.valeur) }
            .filter { it.termine }
            .map { it.placetteUuid }
            .toSet()
}
