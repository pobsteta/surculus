/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.placette

/** Identifiant (UUID) d'un plant. Le format UUID v7 est garanti par `:data` (ADR 0005), pas ici. */
@JvmInline
value class UuidPlant(val valeur: String) {
    init { require(valeur.isNotBlank()) { "UUID de plant vide" } }
    override fun toString() = valeur
}

/** Identifiant (UUID) d'une placette (`placettes.uuid`). Le code lu sur le piquet est porté à part ([Piquet.code]). */
@JvmInline
value class UuidPlacette(val valeur: String) {
    init { require(valeur.isNotBlank()) { "UUID de placette vide" } }
    override fun toString() = valeur
}

/** Identifiant (UUID) d'un contrôle. */
@JvmInline
value class UuidControle(val valeur: String) {
    init { require(valeur.isNotBlank()) { "UUID de contrôle vide" } }
    override fun toString() = valeur
}

/**
 * Issue d'une règle du contrôle de placette : acceptée, refusée avec motif, ou en attente d'une
 * confirmation explicite de l'agent (alerte qui ne refuse pas, spec § 6).
 */
sealed interface Verdict<out T> {
    data class Accepte<out T>(val valeur: T) : Verdict<T>
    data class Refuse(val refus: RefusPlacette) : Verdict<Nothing>
    data class AConfirmer(val alerte: AlertePlacette) : Verdict<Nothing>

    val estAccepte: Boolean get() = this is Accepte

    fun valeurOuNull(): T? = (this as? Accepte<T>)?.valeur

    /** Valeur acceptée ; lève [IllegalStateException] avec le motif sinon. */
    fun valeurOuEchec(): T = when (this) {
        is Accepte -> valeur
        is Refuse -> throw IllegalStateException("${refus.motif} : ${refus.message}")
        is AConfirmer -> throw IllegalStateException("${alerte.motif} : ${alerte.message}")
    }
}

inline fun <T, R> Verdict<T>.puis(etape: (T) -> Verdict<R>): Verdict<R> = when (this) {
    is Verdict.Accepte -> etape(valeur)
    is Verdict.Refuse -> this
    is Verdict.AConfirmer -> this
}

/** Alerte qui demande une confirmation de l'agent avant d'enregistrer. */
sealed interface AlertePlacette {
    val motif: String
    val message: String

    /** Plant au-delà du rayon de sa placette plus la tolérance (alerte et confirmation, jamais de refus ; arbitrage de Pascal du 2026-10-16 (spec § 6)). */
    data class HorsRayon(val distanceHorizontaleM: Double, val rayonM: Double, val toleranceM: Double) : AlertePlacette {
        override val motif = "plant_hors_rayon"
        override val message =
            "Plant à $distanceHorizontaleM m du piquet, au-delà du rayon de la placette ($rayonM m + $toleranceM m) : confirmer le pointage."
    }
}

/**
 * Motifs de refus des règles du contrôle de placette. [motif] est un identifiant stable (snake_case),
 * lu par les fixtures et les écrans ; [message] l'explique à l'agent en français.
 */
sealed interface RefusPlacette {
    val motif: String
    val message: String

    data class EtatInconnu(val code: String) : RefusPlacette {
        override val motif = "etat_inconnu"
        override val message = "État « $code » hors de la nomenclature du DSF (sain, atteint, mort, absent) ni « non_observe »."
    }

    data class GroupeCauseInconnu(val code: String) : RefusPlacette {
        override val motif = "cause_inconnue"
        override val message = "Groupe de cause « $code » hors des quatre groupes du DSF."
    }

    data class CauseSurEtat(val etat: EtatDsf) : RefusPlacette {
        override val motif = "cause_sur_etat"
        override val message = "Un plant ${etat.code} ne porte pas de groupe de cause."
    }

    data class DetailSurEtat(val etat: EtatDsf, val detail: String) : RefusPlacette {
        override val motif = "detail_sur_etat"
        override val message = "Détail « $detail » refusé sur un plant ${etat.code}."
    }

    data class HauteurSurAbsent(val hauteurCm: Int) : RefusPlacette {
        override val motif = "hauteur_sur_absent"
        override val message = "Hauteur $hauteurCm cm refusée : un plant absent n'a pas de hauteur."
    }

    data class PlantEnDoubleDansControle(val uuid: UuidPlant) : RefusPlacette {
        override val motif = "plant_en_double_dans_controle"
        override val message = "Le plant $uuid a deux états dans le même contrôle."
    }

    data class HauteurInvalide(val hauteurCm: Int) : RefusPlacette {
        override val motif = "hauteur_invalide"
        override val message = "Hauteur $hauteurCm cm refusée : entier strictement positif attendu."
    }

    data object MesureSurNonObserve : RefusPlacette {
        override val motif = "mesure_sur_non_observe"
        override val message = "Un plant non observé ne porte ni cause, ni détail, ni hauteur."
    }

    data class EssenceInconnue(val code: String) : RefusPlacette {
        override val motif = "essence_inconnue"
        override val message = "Code d'essence « $code » absent de essences.csv."
    }

    data class UuidEnDouble(val uuid: UuidPlant) : RefusPlacette {
        override val motif = "uuid_en_double"
        override val message = "UUID de plant $uuid en double."
    }

    data class PlantDansDeuxPlacettes(val uuid: UuidPlant) : RefusPlacette {
        override val motif = "plant_dans_deux_placettes"
        override val message = "Le plant $uuid appartient à deux placettes."
    }

    data class NumeroEnDouble(val placette: UuidPlacette, val numero: Int) : RefusPlacette {
        override val motif = "numero_en_double"
        override val message = "Numéro $numero déjà pris dans la placette $placette."
    }

    data class NumeroChange(val uuid: UuidPlant, val avant: Int, val apres: Int) : RefusPlacette {
        override val motif = "numero_change"
        override val message = "Le plant $uuid avait le numéro $avant, il ne peut pas prendre le numéro $apres."
    }

    data class PlacetteChangee(val uuid: UuidPlant) : RefusPlacette {
        override val motif = "placette_changee"
        override val message = "Le plant $uuid ne peut pas changer de placette."
    }

    data object NumerosDejaFiges : RefusPlacette {
        override val motif = "numeros_deja_figes"
        override val message = "Les numéros de la placette sont déjà figés : un plant ajouté prend le numéro suivant."
    }

    data object RemplaceObligatoire : RefusPlacette {
        override val motif = "remplace_uuid_obligatoire"
        override val message = "Un regarni doit nommer le plant mort ou absent qu'il remplace."
    }

    data class PlantRemplaceInconnu(val uuid: UuidPlant) : RefusPlacette {
        override val motif = "plant_remplace_inconnu"
        override val message = "Le plant remplacé $uuid est inconnu."
    }

    data class PlantRemplaceAutrePlacette(val uuid: UuidPlant) : RefusPlacette {
        override val motif = "plant_remplace_autre_placette"
        override val message = "Le plant remplacé $uuid est dans une autre placette."
    }

    data class PlantRemplaceNiMortNiAbsent(val uuid: UuidPlant, val dernierEtat: EtatDsf?) : RefusPlacette {
        override val motif = "plant_remplace_ni_mort_ni_absent"
        override val message = "Le plant $uuid n'a pas été vu mort ou absent (dernier état observé : ${dernierEtat?.code ?: "aucun"})."
    }

    data class PlantDejaRemplace(val uuid: UuidPlant, val par: UuidPlant) : RefusPlacette {
        override val motif = "plant_deja_remplace"
        override val message = "Le plant $uuid est déjà remplacé par le regarni $par."
    }

    data object AucunePlacette : RefusPlacette {
        override val motif = "aucune_placette"
        override val message = "Aucune placette dans la tournée."
    }

    data class HorsQuinzeMetres(val distanceM: Double) : RefusPlacette {
        override val motif = "hors_15_m"
        override val message = "À ${metresEnFrancais(distanceM)} m de la placette la plus proche (plus de 15 m). " +
            "Lisez le code sur le piquet, saisissez-le dans le champ « Code lu sur le piquet », puis touchez « Rattacher »."
    }

    data class CodeInconnu(val code: String) : RefusPlacette {
        override val motif = "code_inconnu"
        override val message = "Code de piquet « $code » inconnu de la tournée."
    }
}

/**
 * Code de refus de la spec § 6 pour chaque [RefusPlacette] (mission 012-A) : celui que la saisie porte jusqu'à
 * l'écran et que l'export reconnaît. Un refus du domaine garde son [RefusPlacette.motif], sauf deux familles que
 * l'export nomme autrement : un état, un groupe de cause ou une essence hors des référentiels (`hors_referentiel`),
 * et un UUID de plant redonné ou dans deux contrôles (`uuid_en_double`). `hors_france` vient de la chaîne des
 * repères ([RefusRepere.PiquetHorsFrance]) ou du contrôle d'emprise, il n'est pas un [RefusPlacette].
 *
 * Le `when` est **exhaustif sans `else`** : ajouter un cas à [RefusPlacette] sans dire son code ne compile plus.
 */
object CodesRefus {
    const val HORS_REFERENTIEL = "hors_referentiel"
    const val UUID_EN_DOUBLE = "uuid_en_double"
    const val HORS_FRANCE = "hors_france"
    const val POINT_HORS_FORET = "point_hors_foret"

    fun code(refus: RefusPlacette): String = when (refus) {
        is RefusPlacette.EtatInconnu, is RefusPlacette.GroupeCauseInconnu, is RefusPlacette.EssenceInconnue -> HORS_REFERENTIEL
        is RefusPlacette.PlantEnDoubleDansControle, is RefusPlacette.PlacetteChangee, is RefusPlacette.UuidEnDouble -> UUID_EN_DOUBLE
        is RefusPlacette.CauseSurEtat, is RefusPlacette.DetailSurEtat, is RefusPlacette.HauteurSurAbsent,
        is RefusPlacette.HauteurInvalide, RefusPlacette.MesureSurNonObserve, is RefusPlacette.PlantDansDeuxPlacettes,
        is RefusPlacette.NumeroEnDouble, is RefusPlacette.NumeroChange, RefusPlacette.NumerosDejaFiges,
        RefusPlacette.RemplaceObligatoire, is RefusPlacette.PlantRemplaceInconnu, is RefusPlacette.PlantRemplaceAutrePlacette,
        is RefusPlacette.PlantRemplaceNiMortNiAbsent, is RefusPlacette.PlantDejaRemplace, RefusPlacette.AucunePlacette,
        is RefusPlacette.HorsQuinzeMetres, is RefusPlacette.CodeInconnu -> refus.motif
    }

    /** Code d'un refus de la chaîne des repères : `hors_france` pour un piquet hors de France, le motif sinon. */
    fun code(refus: io.github.pobsteta.surculus.core.reperes.RefusRepere): String =
        if (refus is io.github.pobsteta.surculus.core.reperes.RefusRepere.PiquetHorsFrance) HORS_FRANCE else refus.motif
}

/**
 * Distance arrondie au mètre entier, milliers séparés par l'espace fine insécable U+202F : 9979,54 -> « 9 980 ». U+202F est le
 * séparateur de milliers du français dans le CLDR d'Unicode (et du JDK récent pour `fr-FR`) ; on l'écrit à la main, sans `Locale`,
 * pour que le texte soit le même sur tout appareil et dans le test du `core`, qui n'a pas Android.
 */
internal fun metresEnFrancais(distanceM: Double): String =
    Math.round(distanceM).toString().reversed().chunked(3).joinToString("\u202F").reversed()
