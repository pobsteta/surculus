/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.reperes.RefusRepere
import java.time.LocalDate

/**
 * Motifs de refus de la lecture et de l'export. Un refus n'écrit rien : l'export est tout ou rien, le fichier
 * de destination n'est jamais laissé à moitié écrit. [motif] est un identifiant stable (snake_case) lu par les
 * tests ; [message] l'explique en français.
 */
sealed interface RefusGpkg {
    val motif: String
    val message: String

    data class PasUnGeoPackage(val detail: String) : RefusGpkg {
        override val motif = "pas_un_geopackage"
        override val message = "Ce fichier n'est pas un GeoPackage lisible : $detail."
    }

    data class CoucheAbsente(val couche: String) : RefusGpkg {
        override val motif = "couche_absente"
        override val message = "La couche « $couche » manque : ce n'est pas une tournée travaux de sommieR."
    }

    data class ReferentielSpatialInconnu(val srsId: Int) : RefusGpkg {
        override val motif = "referentiel_spatial_inconnu"
        override val message = "Le srs_id $srsId n'est pas décrit dans gpkg_spatial_ref_sys, ou n'est pas un code EPSG."
    }

    /** Refus de la chaîne des repères (ex. 2154 et 9794 mêlés, code hors Lambert-93). */
    data class Referentiel(val refus: RefusRepere) : RefusGpkg {
        override val motif = refus.motif
        override val message = refus.message
    }

    data class PointHorsFrance(val couche: String, val uuid: String, val x: Double, val y: Double, val axesInverses: Boolean) : RefusGpkg {
        override val motif = "point_hors_france"
        override val message = "Point de « $couche » ($uuid) hors de France : x = $x, y = $y (Lambert-93, x = Est, y = Nord)." +
            if (axesInverses) " Une fois les axes permutés, il tombe en France : x et y sont inversés." else ""
    }

    data class PointHorsForet(val couche: String, val uuid: String, val x: Double, val y: Double) : RefusGpkg {
        override val motif = "point_hors_foret"
        override val message = "Point de « $couche » ($uuid) hors de l'emprise de la forêt : x = $x, y = $y (Lambert-93)."
    }

    data class PlacetteInconnue(val couche: String, val uuid: String, val placette: String) : RefusGpkg {
        override val motif = "placette_inconnue"
        override val message = "« $couche » $uuid désigne la placette $placette, absente de la tournée."
    }

    data class CodePlacetteEnDouble(val code: String) : RefusGpkg {
        override val motif = "code_placette_en_double"
        override val message = "Le code de placette « $code » est déjà pris : l'import de sommieR refuse une placette installée deux fois."
    }

    data class DeuxControlesMemeJour(val placette: String, val jour: LocalDate, val fuseau: String) : RefusGpkg {
        override val motif = "deux_controles_meme_jour"
        override val message = "Deux contrôles de la placette $placette le même jour ($jour, $fuseau) : sommieR refuse la tournée entière."
    }

    data class UuidEnDouble(val couche: String, val uuid: String) : RefusGpkg {
        override val motif = "uuid_en_double"
        override val message = "UUID $uuid en double dans « $couche » (même identifiant, contenu différent)."
    }

    data class TotauxDiscordants(val controle: String, val ecrits: String, val deduits: String) : RefusGpkg {
        override val motif = "totaux_discordants"
        override val message = "Les totaux du contrôle $controle ($ecrits) ne concordent pas avec ses plants ($deduits)."
    }

    data class EtatSurObjetInconnu(val etat: String, val objet: String) : RefusGpkg {
        override val motif = "etat_sur_objet_inconnu"
        override val message = "L'état $etat désigne $objet, absent de l'export."
    }

    data class EssenceInconnue(val plant: String, val essence: String) : RefusGpkg {
        override val motif = "essence_inconnue"
        override val message = "Le plant $plant porte l'essence « $essence », absente des essences connues."
    }

    /** Un refus des règles d'identité des plants (UUID, numéro, placette). */
    data class Identite(val refus: RefusPlacette) : RefusGpkg {
        override val motif = refus.motif
        override val message = refus.message
    }

    data class ReferentielDuPlantDifferent(val plant: String, val code: Int, val attendu: Int) : RefusGpkg {
        override val motif = "referentiels_meles"
        override val message = "Le plant $plant est en EPSG:$code, la tournée en EPSG:$attendu : 2154 et 9794 ne se mêlent pas."
    }

    data class ChampInvalide(val couche: String, val champ: String, val detail: String) : RefusGpkg {
        override val motif = "champ_invalide"
        override val message = "« $couche », champ « $champ » : $detail."
    }

    data class VersionIllisible(val valeur: String) : RefusGpkg {
        override val motif = "version_illisible"
        override val message = "Version « $valeur » illisible dans la table projet."
    }
}

/** Exception qui porte un [RefusGpkg]. */
class GpkgRefuse(val refus: RefusGpkg) : Exception(refus.message)
