/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import io.github.pobsteta.surculus.core.reperes.RefusRepere
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.tournee.MagasinAffichage

/** Lecture d'un nombre tapé : la valeur, un texte illisible (en clair, sans code), ou le refus d'une règle de `:core` (avec son code). */
sealed interface LectureSaisie<out T> {
    data class Valeur<T>(val valeur: T) : LectureSaisie<T>

    /** Rien d'un nombre : on le dit et on montre un exemple. Ce n'est pas un code de la spec § 6. */
    data class Illisible(val message: String) : LectureSaisie<Nothing>

    data class Refus(val refus: RefusRepere) : LectureSaisie<Nothing>
}

/** Ce que l'agent tape pour pointer un plant : la distance horizontale, et en secours l'azimut (mission 012-B2). */
object SaisiePointage {
    const val DISTANCE_ILLISIBLE = "Distance illisible : tapez des mètres, par exemple 6,2."
    const val AZIMUT_ILLISIBLE = "Cap illisible : tapez un nombre, par exemple 143."

    /** Nombre décimal avec point **ou virgule** (clavier français) ; ni vide, ni infini. */
    fun nombre(texte: String): Double? =
        texte.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    /** Distance horizontale en mètres, strictement positive. */
    fun distance(texte: String): LectureSaisie<Double> {
        val d = nombre(texte)?.takeIf { it > 0.0 } ?: return LectureSaisie.Illisible(DISTANCE_ILLISIBLE)
        return LectureSaisie.Valeur(d)
    }

    /** Azimut tapé, dans [unite] : hors de [0, 360[ (degrés) ou [0, 400[ (grades), refusé avec son code (`AzimutSaisi`). */
    fun azimut(texte: String, unite: UniteAzimut): LectureSaisie<AzimutSaisi> {
        val v = nombre(texte) ?: return LectureSaisie.Illisible(AZIMUT_ILLISIBLE)
        return when (val r = AzimutSaisi.valider(v, unite)) {
            is Resultat.Accepte -> LectureSaisie.Valeur(r.valeur)
            is Resultat.Refuse -> LectureSaisie.Refus(r.refus)
        }
    }

    /** Azimut lu par la boussole du téléphone : toujours en degrés, comme le capteur (`unite_azimut` vaut `deg`). */
    fun azimutBoussole(cap: CapMagnetique): AzimutSaisi =
        (AzimutSaisi.valider(cap.degres, UniteAzimut.DEGRES) as Resultat.Accepte).valeur
}

/**
 * Réglages du contrôle gardés **sur le téléphone** : l'unité de l'azimut tapé, **mémorisée par tournée** (décision de Pascal du
 * 2026-10-27), et la demande de permission de stockage, faite une seule fois.
 */
class ReglagesControle(private val magasin: MagasinAffichage) {
    /** Degrés tant que l'agent n'a pas choisi les grades pour cette tournée. */
    fun uniteAzimut(tournee: String): UniteAzimut =
        (CodesUniteAzimut.lire(magasin.lireTexte(cleUnite(tournee))) as? Resultat.Accepte)?.valeur ?: UniteAzimut.DEGRES

    fun choisirUniteAzimut(tournee: String, unite: UniteAzimut) = magasin.ecrireTexte(cleUnite(tournee), CodesUniteAzimut.code(unite))

    /** Vrai si la permission de stockage a déjà été demandée une fois : on ne harcèle pas. */
    var stockageDejaDemande: Boolean
        get() = magasin.lireBooleen(CLE_STOCKAGE, false)
        set(v) = magasin.ecrireBooleen(CLE_STOCKAGE, v)

    private fun cleUnite(tournee: String) = "unite_azimut/$tournee"

    private companion object {
        const val CLE_STOCKAGE = "stockage_demande"
    }
}
