/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Marculus (pobsteta/marculus), `ui/carte/CarteScreen.kt` (enum `Fond`, l. 185-209 ; choix mémorisé,
 *           l. 235-238 ; menu, l. 655) et bibliothèque `terrain-carte` (`Fonds.kt` : `Attributions`, `CoucheWmts`,
 *           `Geoplateforme`), lus en l'état de la copie locale (2026-10-27). `terrain-carte` n'est pas une dépendance de
 *           Surculus (mission 055) : ses URL de la Géoplateforme sont reprises ici, sans autre code.
 * Modifications : paquet ; noms et libellés inchangés ; la logique du choix (fonds proposés, fond retenu) est sortie de
 *           l'écran pour être testée sur la JVM.
 */
package io.github.pobsteta.surculus.core.carte

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.guidage.Guidage
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance

/** Mentions exigées par les licences des fonds, à afficher sur la carte (celles de Marculus). */
object Attributions {
    const val IGN = "© IGN Géoplateforme, Licence Ouverte Etalab 2.0"
    const val OSM = "© contributeurs OpenStreetMap (ODbL)"
    const val CADASTRE = "cadastre © IGN"
}

/**
 * Fonds de carte, dans l'ordre du menu (celui de Marculus). [attribution] est la mention exigée par la licence de la
 * source ; l'ortho du chantier vient du fichier de la tournée, attribué par celui qui l'a produit.
 */
enum class Fond(val libelle: String, val attribution: String?) {
    PLAN_IGN("Plan IGN", Attributions.IGN),
    IGN("Ortho IGN", Attributions.IGN),
    OSM("OpenStreetMap", Attributions.OSM),
    ORTHO("Ortho chantier", null),
}

/**
 * Couche WMTS de la Géoplateforme IGN (`data.geopf.fr`, sans clé, Licence Ouverte Etalab 2.0). Le jeu de matrices
 * `PM_0_19` est la grille Web Mercator XYZ d'osmdroid : TILEROW et TILECOL sont directement Y et X.
 */
data class CoucheWmts(val nom: String, val couche: String, val style: String, val format: String, val zoomMin: Int = 0, val zoomMax: Int = 19) {
    /** Extension des fichiers de tuiles, pour le cache d'osmdroid. */
    val extension: String get() = if (format == "image/png") ".png" else ".jpg"

    /** URL de la tuile (zoom [z], colonne [x], rangée [y]). */
    fun url(z: Int, x: Int, y: Int): String =
        "$BASE?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=$couche&STYLE=${style.replace(" ", "%20")}&FORMAT=$format" +
            "&TILEMATRIXSET=PM_0_19&TILEMATRIX=$z&TILEROW=$y&TILECOL=$x"

    companion object {
        const val BASE = "https://data.geopf.fr/wmts"
    }
}

/** Les deux couches de la Géoplateforme que montre le menu (les mêmes que Marculus). */
object Geoplateforme {
    /** Plan IGN v2 : le fond par défaut, lisible en forêt (routes, lieux-dits, courbes). */
    val PLAN_IGN = CoucheWmts("PlanIGN", "GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2", "normal", "image/png")

    /** Orthophotos IGN (BD ORTHO®, 20 cm). */
    val ORTHO_IGN = CoucheWmts("OrthoIGN", "ORTHOIMAGERY.ORTHOPHOTOS", "normal", "image/jpeg")

    /** Parcellaire Express (cadastre IGN), transparent : se superpose à n'importe quel fond. */
    val CADASTRE = CoucheWmts("CadastreIGN", "CADASTRALPARCELS.PARCELLAIRE_EXPRESS", "PCI vecteur", "image/png")
}

/** Choix du fond : ce que le menu propose, et le fond retrouvé à l'ouverture de la carte. */
object FondsCarte {
    /** Fond à la première ouverture quand la tournée n'a pas d'ortho : le Plan IGN, comme Marculus (`CarteScreen.kt` l. 237). */
    val PAR_DEFAUT = Fond.PLAN_IGN

    /**
     * Fond sans choix mémorisé : l'Ortho chantier quand le fichier de la tournée existe (lisible sans réseau, mission 057,
     * écart voulu avec Marculus), sinon le Plan IGN.
     */
    fun parDefaut(orthoChantier: Boolean): Fond = if (orthoChantier) Fond.ORTHO else PAR_DEFAUT

    /** L'ortho du chantier n'est pas mémorisée : à la réouverture, la tournée peut ne plus en avoir (Marculus, l. 235 et 406). */
    fun memorisable(fond: Fond) = fond != Fond.ORTHO

    /** Fonds du menu : les trois en ligne, plus l'ortho chantier seulement si le fichier de la tournée existe. */
    fun proposes(orthoChantier: Boolean): List<Fond> = Fond.entries.filter { it != Fond.ORTHO || orthoChantier }

    /** Le cadastre IGN est masqué à la première ouverture (Marculus : `prefsCarte.getBoolean(CLE_CADASTRE, false)`). */
    const val CADASTRE_PAR_DEFAUT = false

    /**
     * Mentions affichées sous l'échelle (Marculus, l. 731-733) : celle du fond, plus celle du cadastre quand il est
     * affiché sur un fond qui n'est pas déjà de l'IGN.
     */
    fun mentions(fond: Fond, cadastre: Boolean): List<String> {
        val mentionCadastre = cadastre && fond != Fond.PLAN_IGN && fond != Fond.IGN
        return listOfNotNull(fond.attribution, if (mentionCadastre) Attributions.CADASTRE else null)
    }

    /** Fond à l'ouverture : le choix mémorisé s'il est connu et mémorisable, sinon [parDefaut]. */
    fun retenu(nomMemorise: String?, orthoChantier: Boolean): Fond =
        Fond.entries.firstOrNull { it.name == nomMemorise && memorisable(it) && it in proposes(orthoChantier) } ?: parDefaut(orthoChantier)
}

/** La carte ne se cadre que sur une emprise en France métropolitaine (ADR 0009, garde-fou conservé ; `CLAUDE.md`). */
object EmpriseCarte {
    sealed interface Resultat {
        data object EnFrance : Resultat

        /** Un coin de l'emprise tombe hors de France métropolitaine : axes inversés ou oubli de reprojection. */
        data class HorsFrance(val coin: PointLambert93, val inversionDetectee: Boolean) : Resultat
    }

    fun controler(e: EmpriseLambert93): Resultat {
        val coins = listOf(
            PointLambert93(e.xMinMetres, e.yMinMetres), PointLambert93(e.xMaxMetres, e.yMaxMetres),
            PointLambert93(e.xMinMetres, e.yMaxMetres), PointLambert93(e.xMaxMetres, e.yMinMetres),
        )
        for (c in coins) {
            val ctl = EmpriseEtFrance.controler(c)
            if (!ctl.enFrance) return Resultat.HorsFrance(c, ctl.inversionDetectee)
        }
        return Resultat.EnFrance
    }
}

/**
 * Où va le bouton « recentrer » : sur le fix GNSS s'il tombe en France, sinon sur l'emprise de la tournée si elle y tombe,
 * sinon nulle part (règle géographique de `CLAUDE.md` : un fix aux axes inversés tomberait en Somalie).
 */
object Recentrage {
    sealed interface Cible {
        data class Fix(val position: Position) : Cible

        data object Emprise : Cible

        data object Rien : Cible
    }

    fun cible(fix: Position?, emprise: EmpriseLambert93?): Cible = when {
        fix != null && Guidage.versLambert93(fix) != null -> Cible.Fix(fix)
        emprise != null && EmpriseCarte.controler(emprise) == EmpriseCarte.Resultat.EnFrance -> Cible.Emprise
        else -> Cible.Rien
    }
}

/** Le marqueur de position n'est dessiné que pour un fix en France : un fix hors de France (Somalie, axes inversés) est masqué. */
object MarqueurPosition {
    /** Le fix à dessiner, ou `null` (pas de fix, ou fix hors de France : [Guidage.versLambert93] est alors `null`). */
    fun aDessiner(fix: Position?): Position? = fix?.takeIf { Guidage.versLambert93(it) != null }
}

/** Une tuile Web Mercator (schéma XYZ, comme osmdroid et la grille `PM_0_19` de l'IGN), pour lire le fichier d'ortho. */
data class Tuile(val z: Int, val x: Int, val y: Int)
