/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatPlant
import io.github.pobsteta.surculus.core.placette.Identite
import io.github.pobsteta.surculus.core.placette.IdentitePlant
import io.github.pobsteta.surculus.core.placette.JourCivil
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Contrôles faits avant toute écriture : si l'un échoue, rien n'est écrit (export tout ou rien).
 * Chaque règle vient de `docs/spec.md` § 5-6, de l'ADR 0007 ou de l'import de sommieR 0.31.1
 * (`R/travaux_qfield.R`, `controler_tournee_travaux`).
 */
internal object ValidationExport {
    /** Identifiant admis par l'import de sommieR : minuscules seulement (`MOTIF_UUID`, `R/valider.R` l. 10). */
    private val MOTIF_UUID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    fun uuid(couche: String, champ: String, valeur: String) {
        if (!MOTIF_UUID.matches(valeur)) {
            throw GpkgRefuse(RefusGpkg.ChampInvalide(couche, champ, "identifiant « $valeur » invalide (UUID en minuscules attendu, comme l'import de sommieR)"))
        }
    }

    private fun point(tournee: Tournee, couche: String, uuid: String, p: PointLambert93) {
        val c = EmpriseEtFrance.controler(p, tournee.empriseForet)
        if (!c.enFrance) throw GpkgRefuse(RefusGpkg.PointHorsFrance(couche, uuid, p.xMetres, p.yMetres, c.inversionDetectee))
        if (c.dansEmprise == false) throw GpkgRefuse(RefusGpkg.PointHorsForet(couche, uuid, p.xMetres, p.yMetres))
    }

    private fun fini(couche: String, champ: String, v: Double?) {
        if (v != null && !v.isFinite()) throw GpkgRefuse(RefusGpkg.ChampInvalide(couche, champ, "valeur non finie ($v)"))
    }

    /** Résultat : les plants déjà dans le fichier, pour ne pas les réécrire. */
    fun verifier(tournee: Tournee, saisies: Saisies, essences: Essences?) {
        // 1. Identifiants et doublons.
        val connuesPlacettes = tournee.placettes.map { it.uuid.lowercase() }.toMutableSet()
        val codesPris = tournee.placettes.mapNotNull { it.code }.toMutableSet()
        for (p in saisies.placettesInstallees) {
            uuid("placettes", "uuid", p.uuid.valeur)
            if (!connuesPlacettes.add(p.uuid.valeur)) throw GpkgRefuse(RefusGpkg.UuidEnDouble("placettes", p.uuid.valeur))
            if (!codesPris.add(p.code)) throw GpkgRefuse(RefusGpkg.CodePlacetteEnDouble(p.code))
            if (p.code.isBlank()) throw GpkgRefuse(RefusGpkg.ChampInvalide("placettes", "code_placette", "code vide"))
            uuid("placettes", "travaux_id", p.travauxId)
            if (!p.rayonM.isFinite() || p.rayonM <= 0.0) throw GpkgRefuse(RefusGpkg.ChampInvalide("placettes", "rayon_m", "rayon ${p.rayonM} invalide"))
            point(tournee, "placettes", p.uuid.valeur, p.position)
        }
        controlerReferentiel(tournee, saisies)

        // 2. Plants : identité, placette connue, essence connue, numéros.
        val existants = tournee.plants.associateBy { it.uuid }
        val nouveaux = LinkedHashMap<String, PlantNumerote>()
        for (pn in saisies.plants) {
            val id = pn.plant.uuid.valeur
            uuid("plants", "uuid", id)
            uuid("plants", "placette_uuid", pn.plant.placette.valeur)
            if (id in nouveaux) throw GpkgRefuse(RefusGpkg.Identite(RefusPlacette.UuidEnDouble(pn.plant.uuid)))
            nouveaux[id] = pn
            if (pn.plant.placette.valeur !in connuesPlacettes) {
                throw GpkgRefuse(RefusGpkg.PlacetteInconnue("plants", id, pn.plant.placette.valeur))
            }
            point(tournee, "plants", id, pn.plant.derive.position)
            val essence = pn.plant.essence
            if (essence != null && essences != null && essences.valider(essence.code) is Verdict.Refuse) {
                throw GpkgRefuse(RefusGpkg.EssenceInconnue(id, essence.code))
            }
            fini("plants", "distance_m", pn.plant.derive.distanceHorizontaleM)
        }
        val identites = buildList {
            tournee.plants.forEach { add(IdentitePlant(UuidPlant(it.uuid), UuidPlacette(it.placetteUuid), it.numero, it.remplaceUuid?.let(::UuidPlant))) }
            nouveaux.values.filter { it.plant.uuid.valeur !in existants }.forEach {
                add(IdentitePlant(it.plant.uuid, it.plant.placette, it.numero, it.plant.remplace))
            }
        }
        // Un plant déjà dans le fichier et repassé à l'identique n'est pas un doublon : on compare placette et numéro.
        for ((id, pn) in nouveaux) {
            val avant = existants[id] ?: continue
            if (avant.placetteUuid != pn.plant.placette.valeur) throw GpkgRefuse(RefusGpkg.Identite(RefusPlacette.PlacetteChangee(pn.plant.uuid)))
            if (avant.numero != pn.numero) throw GpkgRefuse(RefusGpkg.Identite(RefusPlacette.NumeroChange(pn.plant.uuid, avant.numero, pn.numero)))
        }
        (Identite.verifier(identites) as? Verdict.Refuse)?.let { throw GpkgRefuse(RefusGpkg.Identite(it.refus)) }
        val tousPlants = identites.associateBy { it.uuid.valeur }
        for (pn in nouveaux.values) {
            val cible = pn.plant.remplace ?: continue
            val remplace = tousPlants[cible.valeur] ?: throw GpkgRefuse(RefusGpkg.Identite(RefusPlacette.PlantRemplaceInconnu(cible)))
            if (remplace.placette != pn.plant.placette) throw GpkgRefuse(RefusGpkg.Identite(RefusPlacette.PlantRemplaceAutrePlacette(cible)))
        }

        // 3. Contrôles : UUID, placette, un par jour, états et totaux.
        val controlesExistants = tournee.controles.associateBy { it.uuid }
        val controlesNouveaux = LinkedHashMap<String, ControleSaisi>()
        for (c in saisies.controles) {
            val id = c.uuid.valeur
            uuid("controles", "uuid", id)
            uuid("controles", "placette_uuid", c.placette.valeur)
            if (id in controlesNouveaux) throw GpkgRefuse(RefusGpkg.UuidEnDouble("controles", id))
            controlesNouveaux[id] = c
            if (c.placette.valeur !in connuesPlacettes) throw GpkgRefuse(RefusGpkg.PlacetteInconnue("controles", id, c.placette.valeur))
            val attendu = JourCivil.de(c.visiteLe)
            if (c.jour != attendu) throw GpkgRefuse(RefusGpkg.JourIncoherent(id, c.jour, attendu))
            point(tournee, "controles", id, c.position)
            c.positionMesuree?.let { point(tournee, "positions_controles", id, it.position) }
            fini("positions_controles", "precision_m", c.positionMesuree?.precisionM)
            verifierEtats(c, tousPlants, nouveaux.keys + existants.keys)
        }
        verifierUnParJour(tournee, controlesNouveaux.values, controlesExistants.keys)

        // 3 bis. Travaux : UUID, code présent dans la couche codes de la tournée, point en France.
        val travauxVus = HashSet<String>()
        for (t in saisies.travaux) {
            uuid("travaux", "uuid", t.uuid)
            if (!travauxVus.add(t.uuid)) throw GpkgRefuse(RefusGpkg.UuidEnDouble("travaux", t.uuid))
            if (t.codeTravaux !in tournee.codesTravaux) throw GpkgRefuse(RefusGpkg.CodeTravauxHorsReferentiel(t.uuid, t.codeTravaux))
            when (val g = t.geometrie) {
                is Geometrie.Point -> point(tournee, "travaux_pt", t.uuid, g.position)
                is Geometrie.Polygone -> g.points().forEach { point(tournee, "travaux_surf", t.uuid, it) }
                else -> error("Géométrie de travail non prise en charge")
            }
        }

        // 4. Constats et vides.
        val ids = HashSet<String>()
        for (k in saisies.constats) {
            uuid("constats_reprise", "uuid", k.uuid)
            if (!ids.add("c" + k.uuid)) throw GpkgRefuse(RefusGpkg.UuidEnDouble("constats_reprise", k.uuid))
            k.surface.points().forEach { point(tournee, "constats_reprise", k.uuid, it) }
            k.departsJson?.let { j ->
                val t = j.trim()
                if (!t.startsWith("[") || !t.endsWith("]")) {
                    throw GpkgRefuse(RefusGpkg.ChampInvalide("constats_reprise", "departs", "un tableau JSON est attendu"))
                }
            }
            k.essence?.let { e ->
                if (essences != null && essences.valider(e) is Verdict.Refuse) throw GpkgRefuse(RefusGpkg.EssenceInconnue(k.uuid, e))
            }
            for ((n, v) in listOf("pas_lignes" to k.pasLignes, "taux_brut_pct" to k.tauxBrutPct, "ic_bas_pct" to k.icBasPct, "ic_haut_pct" to k.icHautPct, "seuil_pct" to k.seuilPct)) {
                fini("constats_reprise", n, v)
            }
        }
        for (v in saisies.vides) {
            uuid("vides", "uuid", v.uuid)
            if (!ids.add("v" + v.uuid)) throw GpkgRefuse(RefusGpkg.UuidEnDouble("vides", v.uuid))
            if ((v.constatUuid == null) == (v.placetteUuid == null)) {
                throw GpkgRefuse(RefusGpkg.ChampInvalide("vides", "constat_uuid", "un vide se rattache à un constat ou à une placette, pas aux deux ni à aucun"))
            }
            v.placetteUuid?.let { if (it !in connuesPlacettes) throw GpkgRefuse(RefusGpkg.PlacetteInconnue("vides", v.uuid, it)) }
            v.constatUuid?.let { c ->
                if (saisies.constats.none { it.uuid == c }) throw GpkgRefuse(RefusGpkg.ChampInvalide("vides", "constat_uuid", "constat $c absent de l'export"))
            }
            v.surface.points().forEach { point(tournee, "vides", v.uuid, it) }
            fini("vides", "surface_are", v.surfaceAre)
        }
    }

    /** Un seul repère par tournée : 2154 ou 9794, celui du fichier, lu dans `gpkg_spatial_ref_sys` (ADR 0003). */
    private fun controlerReferentiel(tournee: Tournee, saisies: Saisies) {
        for (pn in saisies.plants) {
            val code = pn.plant.derive.epsg.code
            if (code != tournee.epsg.code) throw GpkgRefuse(RefusGpkg.ReferentielDuPlantDifferent(pn.plant.uuid.valeur, code, tournee.epsg.code))
        }
        for (c in saisies.controles) {
            val code = c.repere?.code ?: continue
            if (code != tournee.epsg.code) throw GpkgRefuse(RefusGpkg.ReferentielDuControleDifferent(c.uuid.valeur, code, tournee.epsg.code))
        }
    }

    private fun verifierEtats(c: ControleSaisi, plants: Map<String, IdentitePlant>, connus: Set<String>) {
        val vus = HashSet<String>()
        for (e in c.etats) {
            ValidationExport.uuid("etats_plants", "uuid", e.uuid)
            if (!vus.add(e.uuid)) throw GpkgRefuse(RefusGpkg.UuidEnDouble("etats_plants", e.uuid))
            val p = plants[e.plant.valeur]
            if (e.plant.valeur !in connus || p == null) throw GpkgRefuse(RefusGpkg.EtatSurObjetInconnu(e.uuid, "le plant ${e.plant}"))
            if (p.placette != c.placette) {
                throw GpkgRefuse(RefusGpkg.ChampInvalide("etats_plants", "plant_uuid", "le plant ${e.plant} appartient à la placette ${p.placette}, le contrôle ${c.uuid} à ${c.placette}"))
            }
        }
        if (c.etats.isEmpty()) return
        val deduits = when (val v = Totaux.deduire(c.etats.map { EtatPlant(it.plant, it.observation) }, c.remplaces)) {
            is Verdict.Accepte -> v.valeur
            is Verdict.Refuse -> throw GpkgRefuse(RefusGpkg.Identite(v.refus))
            else -> error("Verdict inattendu : $v")
        }
        if (deduits != c.totaux) {
            throw GpkgRefuse(RefusGpkg.TotauxDiscordants(c.uuid.valeur, c.totaux.toString(), deduits.toString()))
        }
    }

    /**
     * Un contrôle par placette et par jour, deux fois (décision de Pascal, 2026-10-17) :
     * - le jour civil Europe/Paris du journal (ADR 0005), repris de [ControleSaisi.jour] pour les contrôles à écrire
     *   et calculé depuis `visite_le` pour ceux déjà présents dans la tournée ;
     * - le jour UTC de l'import de sommieR, qui lit `visite_le` en `POSIXct` et refuse la tournée entière si deux
     *   contrôles d'une même placette tombent le même jour UTC (`R/travaux_qfield.R` l. 391-394).
     * Les deux peuvent différer entre minuit et deux heures du matin (heure d'été) : l'export refuse alors,
     * plutôt que de laisser l'import refuser toute la tournée au bureau. La règle UTC tombera quand sommieR
     * alignera son import sur le jour civil (demande au brief vers sommieR).
     */
    private fun verifierUnParJour(tournee: Tournee, nouveaux: Collection<ControleSaisi>, uuidDeja: Set<String>) {
        val aEcrire = nouveaux.filter { it.uuid.valeur !in uuidDeja }
        val parPlacetteUtc = HashMap<Pair<String, LocalDate>, String>()
        val parPlacetteParis = HashMap<Pair<String, LocalDate>, String>()
        for (k in tournee.controles) {
            val p = k.placetteUuid?.lowercase() ?: continue
            val instant = k.visiteLe?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: continue
            parPlacetteUtc[p to instant.atOffset(ZoneOffset.UTC).toLocalDate()] = k.uuid
            parPlacetteParis[p to JourCivil.de(instant)] = k.uuid
        }
        for (c in aEcrire) {
            val jourUtc = c.visiteLe.atOffset(ZoneOffset.UTC).toLocalDate()
            if (parPlacetteUtc.put(c.placette.valeur to jourUtc, c.uuid.valeur) != null) {
                throw GpkgRefuse(RefusGpkg.DeuxControlesMemeJour(c.placette.valeur, jourUtc, "UTC"))
            }
            if (parPlacetteParis.put(c.placette.valeur to c.jour, c.uuid.valeur) != null) {
                throw GpkgRefuse(RefusGpkg.DeuxControlesMemeJour(c.placette.valeur, c.jour, JourCivil.FUSEAU.id))
            }
        }
    }

    /** Instant au format de GDAL pour un champ `DATETIME` : ISO 8601 UTC, millisecondes, « Z ». */
    fun datetime(i: Instant): String = java.time.format.DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC)
        .format(i)
}
