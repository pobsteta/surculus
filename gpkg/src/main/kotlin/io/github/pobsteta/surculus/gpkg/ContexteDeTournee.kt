/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.session.ContexteControle
import io.github.pobsteta.surculus.core.session.PlacetteControlable
import io.github.pobsteta.surculus.core.session.PlantAnterieur

/**
 * Ce que la session de contrôle (mission 012-A) lit de la tournée ouverte : ses placettes, son repère, l'emprise de la
 * forêt, les plants et leur dernier état au passage précédent (couches `plants` et `etats_plants` de sommieR).
 */
object ContexteDeTournee {
    /**
     * Une placette sans position ou sans rayon n'est pas contrôlable : elle est ignorée, sans position ni rayon inventés.
     * L'opérateur est `projet.operateur` (décision de Pascal du 2026-10-27), vide si la tournée n'en porte pas.
     */
    fun de(t: Tournee): ContexteControle = ContexteControle(
        operateur = t.projet?.operateur.orEmpty(),
        placettes = t.placettes.mapNotNull { p ->
            val position = p.position ?: return@mapNotNull null
            val rayon = p.rayonM?.takeIf { it > 0.0 } ?: return@mapNotNull null
            PlacetteControlable(Piquet(UuidPlacette(p.uuid), p.code.orEmpty(), position), rayon)
        },
        epsg = t.epsg,
        empriseForet = t.empriseForet,
        plantsAnterieurs = plantsAnterieurs(t),
    )

    /**
     * Plants du passage précédent par placette, avec leur dernier état (le contrôle le plus récent d'après `visite_le`,
     * texte ISO 8601 : l'ordre des textes est celui des instants). Un état illisible est ignoré, jamais corrigé.
     */
    fun plantsAnterieurs(t: Tournee): Map<UuidPlacette, List<PlantAnterieur>> {
        val visiteDe = t.controles.associate { it.uuid to it.visiteLe.orEmpty() }
        val dernierEtat = t.etats.groupBy { it.plantUuid }
            .mapValues { (_, l) -> l.maxByOrNull { visiteDe[it.controleUuid].orEmpty() } }
        return t.plants.groupBy { UuidPlacette(it.placetteUuid) }.mapValues { (_, plants) ->
            plants.map { p ->
                val e = dernierEtat[p.uuid]
                PlantAnterieur(
                    UuidPlant(p.uuid), p.numero, p.essence, p.remplaceUuid?.let(::UuidPlant),
                    e?.let { (Observations.valider(it.etat, it.causeGroupe, it.causeDetail, it.hauteurCm) as? Verdict.Accepte)?.valeur },
                )
            }
        }
    }
}
