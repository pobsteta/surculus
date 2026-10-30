/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.placette

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Mission 074, reprise 1/3 : l'alerte « hors rayon » écrit ses distances au décimètre, virgule française. */
class AlerteHorsRayonTest {
    @Test
    fun distancesAuDecimetreAvecVirgule() {
        val a = AlertePlacette.HorsRayon(7.912345678901, 5.64, 1.0)
        assertEquals(
            "Plant à 7,9 m du piquet, au-delà du rayon de la placette (5,6 m + 1,0 m) : confirmer le pointage.",
            a.message,
        )
    }

    @Test
    fun entierEtRayonArrondis() {
        val a = AlertePlacette.HorsRayon(12.0, 11.28, 1.0)
        assertEquals(
            "Plant à 12,0 m du piquet, au-delà du rayon de la placette (11,3 m + 1,0 m) : confirmer le pointage.",
            a.message,
        )
    }
}
