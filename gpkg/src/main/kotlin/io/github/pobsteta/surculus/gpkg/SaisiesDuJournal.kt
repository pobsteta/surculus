/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.CodeEssence
import io.github.pobsteta.surculus.core.placette.EtatPlant
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.placette.Placette
import io.github.pobsteta.surculus.core.placette.Pointage
import io.github.pobsteta.surculus.core.placette.Regarnis
import io.github.pobsteta.surculus.core.placette.RegarniPose
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.Resultat
import io.github.pobsteta.surculus.core.session.FormatValeurs
import io.github.pobsteta.surculus.core.session.LigneCourante
import io.github.pobsteta.surculus.core.session.ValeurControle
import io.github.pobsteta.surculus.core.session.ValeurEtat
import io.github.pobsteta.surculus.core.session.ValeurPointage
import java.time.Instant
import java.time.LocalDate
import kotlin.math.hypot

/**
 * Une faute qui renvoie à la saisie (spec § 4.8 ; missions 068 et 082) : le code de la placette (`P35-01`), le numéro
 * du plant s'il s'agit d'un plant, et un texte en français de la forme « placette[, plant n° N] : ce qui ne va pas :
 * quoi faire » (décision de Pascal du 2026-11-03), sans motif technique ni UUID.
 */
data class FauteDeSaisie(val placette: String?, val plant: Int?, val texte: String) {
    companion object {
        /** Compose le texte « P35-01, plant n° 3 : constat : consigne » ; sans placette connue, « Une saisie : … ». */
        fun de(placette: String?, plant: Int?, constat: String, consigne: String): FauteDeSaisie {
            val qui = when {
                placette == null -> "Une saisie"
                plant == null -> placette
                else -> "$placette, plant n° $plant"
            }
            return FauteDeSaisie(placette, plant, "$qui : $constat : $consigne")
        }
    }
}

/**
 * Ce que le rendu sait de chaque identifiant écrit, pour qu'une faute nomme la placette et le plant au lieu d'un UUID :
 * contrôle → placette, plant → (placette, numéro), état → plant.
 */
class ReperesSaisies internal constructor(
    private val placetteDuControle: Map<String, String>,
    private val placetteDuPlant: Map<String, String>,
    private val numeroDuPlant: Map<String, Int>,
    private val plantDeEtat: Map<String, String>,
    private val codeDePlacette: Map<String, String>,
) {
    fun placetteDuControle(uuid: String): String? = placetteDuControle[uuid.lowercase()]
    fun placetteDuPlant(uuid: String): String? = placetteDuPlant[uuid.lowercase()]
    fun numeroDuPlant(uuid: String): Int? = numeroDuPlant[uuid.lowercase()]
    fun plantDeEtat(uuid: String): String? = plantDeEtat[uuid.lowercase()]
    fun codeDePlacette(uuid: String): String? = codeDePlacette[uuid.lowercase()]
}

/** Résultat du passage du journal aux saisies : les saisies, les repères pour nommer les fautes, et les fautes déjà vues. */
data class SaisiesConverties(
    val saisies: Saisies,
    val reperes: ReperesSaisies,
    val fautes: List<FauteDeSaisie>,
    /** Instant le plus récent des saisies exportées (visite ou pointage) ; `null` sans contrôle exporté. */
    val derniereSaisie: Instant?,
)

/**
 * Mission 090 : le passage des lignes courantes du journal d'**une** tournée aux [Saisies] de l'export (spec § 5).
 *
 * - Un contrôle est exporté s'il a une valeur courante, lisible, sur une placette de la tournée, et s'il est **terminé**.
 *   Un contrôle commencé mais non terminé est une faute (« le terminer ou l'annuler », décision de Pascal du 2026-11-03) :
 *   ses numéros ne sont pas encore figés (spec § 6).
 * - Un plant est exporté si son pointage appartient à un contrôle exporté ; il porte le numéro figé par ce contrôle. Les
 *   plants d'un contrôle annulé ne sont pas exportés : ils restent au journal (décision de Pascal du 2026-11-03).
 * - La position d'un plant est recalculée par la chaîne des repères (piquet de la tournée, observation brute du journal) ;
 *   un écart de plus de [ECART_MAX_M] avec la position du journal est une faute, jamais corrigé en silence.
 * - Les totaux sont déduits des états ([Totaux.deduire]), les plants déjà remplacés par un regarni d'un jour antérieur
 *   en sortent ([Regarnis.remplacesAvant]).
 *
 * Le journal n'est que lu : l'export ne touche pas au journal (spec § 5).
 */
object SaisiesDuJournal {
    /** Écart toléré entre la position recalculée d'un plant et celle du journal, en mètres. */
    const val ECART_MAX_M = 0.001

    private class Controle(val ligne: LigneCourante, val valeur: ValeurControle, val code: String) {
        val uuid: String get() = ligne.objetUuid
        val jour: LocalDate get() = LocalDate.parse(valeur.jourCivil)
    }

    fun construire(tournee: Tournee, lignes: List<LigneCourante>): SaisiesConverties {
        val fautes = ArrayList<FauteDeSaisie>()
        val placettes = tournee.placettes.associateBy { it.uuid.lowercase() }
        val codeDePlacette = tournee.placettes.associate { it.uuid.lowercase() to (it.code ?: "Placette sans code") }

        // 1. Contrôles : lisibles, sur une placette de la tournée, terminés.
        val controles = ArrayList<Controle>()
        val tousLesControles = HashMap<String, ValeurControle>()
        for (l in lignes.filter { it.type == FormatValeurs.TYPE_CONTROLE }) {
            val v = ValeurControle.lire(l.valeur)
            if (v == null) {
                fautes += FauteDeSaisie.de(null, null, "un contrôle n'a pas pu être relu", "mettre l'application à jour, puis rendre de nouveau")
                continue
            }
            tousLesControles[l.objetUuid] = v
            val p = placettes[v.placetteUuid.lowercase()]
            val code = p?.code ?: v.codePlacette.ifBlank { null }
            when {
                p == null -> fautes += FauteDeSaisie.de(code, null, "cette placette n'est pas dans la tournée", "annuler ce contrôle")
                !v.termine -> fautes += FauteDeSaisie.de(code, null, "contrôle non terminé", "le terminer ou l'annuler")
                v.repereEpsg != tournee.epsg.code ->
                    fautes += FauteDeSaisie.de(code, null, "la position du contrôle n'est pas dans le repère de la tournée", "annuler ce contrôle et le refaire")
                else -> controles += Controle(l, v, code ?: "Placette sans code")
            }
        }

        // 2. Pointages et états, rangés par contrôle.
        val pointages = lignes.filter { it.type == FormatValeurs.TYPE_POINTAGE }.mapNotNull { l -> ValeurPointage.lire(l.valeur)?.let { l to it } }
        val etats = lignes.filter { it.type == FormatValeurs.TYPE_ETAT }.mapNotNull { l -> ValeurEtat.lire(l.valeur)?.let { l to it } }
        val pointagesParControle = pointages.groupBy { it.second.controleUuid }
        val etatsParControle = etats.groupBy { it.second.controleUuid }
        val illisibles = lignes.count { it.type == FormatValeurs.TYPE_POINTAGE || it.type == FormatValeurs.TYPE_ETAT } - pointages.size - etats.size
        if (illisibles > 0) {
            fautes += FauteDeSaisie.de(null, null, "un plant n'a pas pu être relu", "mettre l'application à jour, puis rendre de nouveau")
        }

        // Regarnis posés : ceux de la tournée livrée (avant tout contrôle), puis ceux du journal, au jour de leur contrôle.
        val poses = ArrayList<RegarniPose>()
        tournee.plants.forEach { p -> p.remplaceUuid?.let { poses += RegarniPose(UuidPlant(p.uuid), UuidPlant(it), LocalDate.MIN) } }
        pointages.forEach { (l, v) ->
            val jour = tousLesControles[v.controleUuid]?.jourCivil?.let(LocalDate::parse) ?: return@forEach
            v.remplaceUuid?.let { poses += RegarniPose(UuidPlant(l.objetUuid), UuidPlant(it), jour) }
        }

        // Repères pour nommer les fautes : plants livrés par la tournée d'abord.
        val placetteDuControle = HashMap<String, String>()
        val placetteDuPlant = HashMap<String, String>()
        val numeroDuPlant = HashMap<String, Int>()
        val plantDeEtat = HashMap<String, String>()
        tournee.plants.forEach { p ->
            codeDePlacette[p.placetteUuid.lowercase()]?.let { placetteDuPlant[p.uuid.lowercase()] = it }
            numeroDuPlant[p.uuid.lowercase()] = p.numero
        }

        val controlesSaisis = ArrayList<ControleSaisi>()
        val plantsSaisis = ArrayList<PlantNumerote>()
        var derniere: Instant? = null
        fun plusRecent(i: Instant) { if (derniere == null || i.isAfter(derniere)) derniere = i }

        for (c in controles.sortedBy { it.uuid }) {
            val v = c.valeur
            val p = placettes.getValue(v.placetteUuid.lowercase())
            placetteDuControle[c.uuid.lowercase()] = c.code
            val piquet = p.position
            val rayon = p.rayonM?.takeIf { it.isFinite() && it > 0.0 }
            if (piquet == null || rayon == null) {
                fautes += FauteDeSaisie.de(c.code, null, "la placette n'a pas de position ou de rayon dans la tournée", "l'importer de nouveau depuis sommieR")
                continue
            }
            val domaine = Placette(UuidPlacette(v.placetteUuid), rayon)
            var faute = false

            // Plants pointés pendant ce contrôle.
            for ((l, pv) in pointagesParControle[c.uuid].orEmpty()) {
                val numero = v.numeros?.get(l.objetUuid)
                if (numero != null) numeroDuPlant[l.objetUuid.lowercase()] = numero
                placetteDuPlant[l.objetUuid.lowercase()] = c.code
                fun fauteDuPlant(constat: String, consigne: String) {
                    fautes += FauteDeSaisie.de(c.code, numero, constat, consigne)
                    faute = true
                }
                if (numero == null) { fauteDuPlant("le plant n'a pas de numéro", "annuler ce contrôle et le refaire"); continue }
                val unite = (CodesUniteAzimut.lire(pv.uniteAzimut) as? Resultat.Accepte)?.valeur
                val azimut = unite?.let { (AzimutSaisi.valider(pv.azimutMag, it) as? Resultat.Accepte)?.valeur }
                val jourDeclinaison = runCatching { LocalDate.parse(pv.jourDeclinaison) }.getOrNull()
                val derive = if (azimut != null && jourDeclinaison != null) {
                    (PointDerive.deriver(piquet, tournee.epsg, pv.distanceHorizontaleM, azimut, jourDeclinaison) as? Resultat.Accepte)?.valeur
                } else {
                    null
                }
                if (derive == null) { fauteDuPlant("la position du plant ne peut pas être recalculée", "annuler ce pointage et le refaire"); continue }
                if (hypot(derive.position.xMetres - pv.x, derive.position.yMetres - pv.y) > ECART_MAX_M) {
                    fauteDuPlant("la position du plant ne correspond pas à sa distance et à son azimut", "annuler ce pointage et le refaire")
                    continue
                }
                val pointeLe = Instant.ofEpochMilli(pv.pointeLeUtcMs)
                when (val r = Pointage.pointer(UuidPlant(l.objetUuid), domaine, derive, pointeLe, pv.essence?.let(::CodeEssence), pv.remplaceUuid?.let(::UuidPlant), pv.horsRayonConfirme)) {
                    is Verdict.Accepte -> { plantsSaisis += PlantNumerote(r.valeur, numero); plusRecent(pointeLe) }
                    is Verdict.AConfirmer -> fauteDuPlant("le plant est hors du rayon de la placette sans confirmation", "annuler ce pointage et le refaire")
                    else -> fauteDuPlant("le pointage du plant est refusé", "annuler ce pointage et le refaire")
                }
            }

            // États de ce contrôle.
            val lignesEtat = ArrayList<LigneEtat>()
            for ((l, ev) in etatsParControle[c.uuid].orEmpty()) {
                plantDeEtat[l.objetUuid.lowercase()] = ev.plantUuid
                val o = Observations.valider(ev.etat, ev.causeGroupe, ev.causeDetail, ev.hauteurCm)
                if (o is Verdict.Accepte) {
                    lignesEtat += LigneEtat(l.objetUuid, UuidPlant(ev.plantUuid), o.valeur)
                } else {
                    fautes += FauteDeSaisie.de(c.code, numeroDuPlant[ev.plantUuid.lowercase()], "l'état dit du plant n'est pas admis", "corriger l'état du plant")
                    faute = true
                }
            }
            if (faute) continue

            val remplaces = Regarnis.remplacesAvant(poses, c.jour)
            val totaux = when (val t = Totaux.deduire(lignesEtat.map { EtatPlant(it.plant, it.observation) }, remplaces)) {
                is Verdict.Accepte -> t.valeur
                else -> {
                    fautes += FauteDeSaisie.de(c.code, null, "les états des plants sont incohérents (un plant dit deux fois ?)", "annuler l'état en trop")
                    continue
                }
            }
            val mesuree = if (v.sansPositionMesuree) {
                PositionMesuree(position = null, rattacheParIdentite = true, sansPositionMesuree = true)
            } else {
                PositionMesuree(PointLambert93(xMetres = v.positionMesureeX!!, yMetres = v.positionMesureeY!!), rattacheParIdentite = v.rattacheParIdentite)
            }
            val visite = Instant.ofEpochMilli(v.visiteLeUtcMs)
            plusRecent(visite)
            controlesSaisis += ControleSaisi(
                uuid = UuidControle(c.uuid), placette = domaine.uuid, visiteLe = visite, jour = c.jour,
                position = PointLambert93(xMetres = v.positionX, yMetres = v.positionY), positionMesuree = mesuree,
                etats = lignesEtat, remplaces = remplaces, totaux = totaux, operateur = v.operateur.ifBlank { null },
            )
        }
        // Les plants exportés sont ceux des contrôles exportés (et sans faute) : ceux d'un contrôle refusé ne partent pas seuls.
        val gardes = controlesSaisis.map { it.uuid.valeur }.toSet()
        val controleDuPointage = pointages.associate { it.first.objetUuid to it.second.controleUuid }
        val plantsGardes = plantsSaisis.filter { controleDuPointage[it.plant.uuid.valeur] in gardes }
        val operateur = controlesSaisis.firstNotNullOfOrNull { it.operateur } ?: tournee.projet?.operateur
        return SaisiesConverties(
            Saisies(operateur, controles = controlesSaisis, plants = plantsGardes),
            ReperesSaisies(placetteDuControle, placetteDuPlant, numeroDuPlant, plantDeEtat, codeDePlacette),
            fautes,
            derniere,
        )
    }
}
