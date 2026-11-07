/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.placette.RefusPlacette

/**
 * Mission 090 : un refus de la vérification de l'export ([RefusGpkg]) traduit en [FauteDeSaisie] que l'agent lit en
 * forêt : la placette et le plant (jamais un UUID), ce qui ne va pas, quoi faire (missions 068 et 082 ; forme décidée par
 * Pascal le 2026-11-03). Le motif technique reste dans le refus ([RefusGpkg.motif]), jamais à l'écran.
 */
internal object FautesDuRendu {
    private const val REFAIRE_CONTROLE = "annuler ce contrôle et le refaire"
    private const val REFAIRE_POINTAGE = "annuler ce pointage et le refaire"

    /**
     * @param controleCourant code de la placette du contrôle vérifié (vérification contrôle par contrôle), ou `null`
     *   pour la vérification de toute la tournée.
     */
    fun de(refus: RefusGpkg, r: ReperesSaisies, controleCourant: String?): FauteDeSaisie {
        fun duControle(uuid: String) = r.placetteDuControle(uuid) ?: controleCourant
        fun duPlant(uuid: String, constat: String, consigne: String) =
            FauteDeSaisie.de(r.placetteDuPlant(uuid) ?: controleCourant, r.numeroDuPlant(uuid), constat, consigne)
        fun deLaPlacette(constat: String, consigne: String) = FauteDeSaisie.de(controleCourant, null, constat, consigne)

        return when (refus) {
            is RefusGpkg.PointHorsFrance -> {
                val indice = if (refus.axesInverses) " (latitude et longitude inversées ?)" else ""
                when (refus.couche) {
                    "plants" -> duPlant(refus.uuid, "la position du plant tombe hors de France$indice", REFAIRE_POINTAGE)
                    "controles", "positions_controles" ->
                        FauteDeSaisie.de(duControle(refus.uuid), null, "la position du contrôle tombe hors de France$indice", REFAIRE_CONTROLE)
                    else -> deLaPlacette("une position tombe hors de France$indice", "annuler cette saisie et la refaire")
                }
            }
            is RefusGpkg.PointHorsForet -> when (refus.couche) {
                "plants" -> duPlant(refus.uuid, "la position du plant tombe hors de la forêt", REFAIRE_POINTAGE)
                "controles", "positions_controles" ->
                    FauteDeSaisie.de(duControle(refus.uuid), null, "la position du contrôle tombe hors de la forêt", REFAIRE_CONTROLE)
                else -> deLaPlacette("une position tombe hors de la forêt", "annuler cette saisie et la refaire")
            }
            is RefusGpkg.EssenceInconnue ->
                duPlant(refus.plant, "essence « ${refus.essence} » absente de la liste des essences", "corriger l'essence")
            is RefusGpkg.PlacetteInconnue -> when (refus.couche) {
                "plants" -> duPlant(refus.uuid, "le plant est rattaché à une placette absente de la tournée", REFAIRE_POINTAGE)
                else -> FauteDeSaisie.de(duControle(refus.uuid), null, "cette placette n'est pas dans la tournée", "annuler ce contrôle")
            }
            is RefusGpkg.DeuxControlesMemeJour ->
                FauteDeSaisie.de(r.codeDePlacette(refus.placette) ?: controleCourant, null, "deux contrôles le même jour", "annuler l'un des deux")
            is RefusGpkg.TotauxDiscordants ->
                FauteDeSaisie.de(duControle(refus.controle), null, "les totaux ne correspondent pas aux états des plants", REFAIRE_CONTROLE)
            is RefusGpkg.JourIncoherent ->
                FauteDeSaisie.de(duControle(refus.controle), null, "le jour du contrôle ne correspond pas à son heure", REFAIRE_CONTROLE)
            is RefusGpkg.ReferentielDuControleDifferent ->
                FauteDeSaisie.de(duControle(refus.controle), null, "la position du contrôle n'est pas dans le repère de la tournée", REFAIRE_CONTROLE)
            is RefusGpkg.ReferentielDuPlantDifferent ->
                duPlant(refus.plant, "la position du plant n'est pas dans le repère de la tournée", REFAIRE_POINTAGE)
            is RefusGpkg.EtatSurObjetInconnu -> {
                val plant = r.plantDeEtat(refus.etat)
                if (plant != null) duPlant(plant, "l'état est dit sur un plant absent du contrôle", "annuler cet état")
                else deLaPlacette("un état est dit sur un plant absent du contrôle", "annuler cet état")
            }
            is RefusGpkg.Identite -> identite(refus.refus, r, controleCourant)
            is RefusGpkg.ChampInvalide -> when (refus.couche) {
                "positions_controles" -> deLaPlacette("la position mesurée du contrôle est incohérente", REFAIRE_CONTROLE)
                "etats_plants" -> deLaPlacette("un état est dit sur un plant d'une autre placette", "annuler cet état")
                else -> deLaPlacette("une saisie est incohérente", "l'annuler et la refaire")
            }
            is RefusGpkg.UuidEnDouble, is RefusGpkg.UuidInvalide ->
                deLaPlacette("une saisie porte un identifiant invalide ou en double", "l'annuler et la refaire")
            is RefusGpkg.BesoinHorsReferentiel ->
                FauteDeSaisie.de(duControle(refus.controle), null, "besoin « ${refus.besoin} » absent de la tournée", "dire de nouveau le besoin")
            is RefusGpkg.ConcurrenceHorsReferentiel ->
                FauteDeSaisie.de(duControle(refus.controle), null, "concurrence « ${refus.concurrence} » inconnue", "dire de nouveau la concurrence")
            is RefusGpkg.CodeTravauxHorsReferentiel ->
                deLaPlacette("un travail porte un code absent de la tournée", "corriger le code du travail")
            is RefusGpkg.CodePlacetteEnDouble ->
                deLaPlacette("ce code de placette est déjà pris", "choisir un autre code")
            is RefusGpkg.PasUnGeoPackage, is RefusGpkg.CoucheAbsente, is RefusGpkg.ReferentielSpatialInconnu,
            is RefusGpkg.Referentiel, is RefusGpkg.VersionIllisible ->
                FauteDeSaisie.de(null, null, "la tournée n'a pas pu être relue", "l'importer de nouveau depuis sommieR")
        }
    }

    private fun identite(refus: RefusPlacette, r: ReperesSaisies, controleCourant: String?): FauteDeSaisie {
        fun plant(u: String, constat: String, consigne: String) =
            FauteDeSaisie.de(r.placetteDuPlant(u) ?: controleCourant, r.numeroDuPlant(u), constat, consigne)
        return when (refus) {
            is RefusPlacette.NumeroEnDouble ->
                FauteDeSaisie.de(r.codeDePlacette(refus.placette.valeur) ?: controleCourant, refus.numero, "ce numéro est déjà pris dans la placette", REFAIRE_POINTAGE)
            is RefusPlacette.NumeroChange -> plant(refus.uuid.valeur, "le numéro du plant a changé depuis la tournée précédente", REFAIRE_POINTAGE)
            is RefusPlacette.PlacetteChangee -> plant(refus.uuid.valeur, "le plant a changé de placette depuis la tournée précédente", REFAIRE_POINTAGE)
            is RefusPlacette.PlantRemplaceInconnu -> plant(refus.uuid.valeur, "le regarni remplace un plant inconnu", "corriger le regarni")
            is RefusPlacette.PlantRemplaceAutrePlacette -> plant(refus.uuid.valeur, "le regarni remplace un plant d'une autre placette", "corriger le regarni")
            is RefusPlacette.PlantEnDoubleDansControle -> plant(refus.uuid.valeur, "le plant est dit deux fois dans le contrôle", "annuler l'état en trop")
            is RefusPlacette.UuidEnDouble -> plant(refus.uuid.valeur, "le plant est enregistré deux fois", REFAIRE_POINTAGE)
            is RefusPlacette.PlantDansDeuxPlacettes -> plant(refus.uuid.valeur, "le plant est enregistré dans deux placettes", REFAIRE_POINTAGE)
            else -> FauteDeSaisie.de(controleCourant, null, "l'identité d'un plant est incohérente", REFAIRE_POINTAGE)
        }
    }
}
