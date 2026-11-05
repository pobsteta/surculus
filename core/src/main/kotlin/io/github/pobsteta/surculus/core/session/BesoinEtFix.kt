/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.OrigineFix

/**
 * Ce que le récepteur dit de son fix à l'arrivée (spec § 5, `positions_controles` ; ADR 0003, ligne « Fix » ; mission 091,
 * décision de Pascal du 2026-11-04, question 4).
 *
 * - [precisionM] : précision horizontale en mètres, la **plus mauvaise** des fix moyennés ; `null` si un fix n'en donne
 *   pas (récepteur sans trame GST), jamais 0.
 * - [sourceGnss] : un code de [SourcesGnss] ; une source hors de ce domaine n'est jamais écrite au journal (la session
 *   la refuse : qualité inconnue, tracée au journal technique) et rend illisible une valeur v3 qui la porterait.
 * - [simulee] : vrai si un fix est simulé (`Location.isMock()`, ou qualité GGA 8 « simulation »).
 */
data class QualiteFix(val precisionM: Double?, val sourceGnss: String, val simulee: Boolean) {
    companion object {
        /** Les cinq valeurs de `source_gnss` ([SourcesGnss]), du moins bon au meilleur. */
        val SOURCES_GNSS: List<String> get() = SourcesGnss.RANG

        /**
         * La qualité d'une arrivée moyennée sur [fixes] : la plus mauvaise source, la plus mauvaise précision, simulée si
         * un seul fix l'est. `null` sans fix. Une précision non finie ou nulle est une précision inconnue (`null`).
         */
        fun deFix(fixes: List<FixGnss>): QualiteFix? {
            if (fixes.isEmpty()) return null
            val source = fixes.map(SourcesGnss::de).minBy { SourcesGnss.RANG.indexOf(it) }
            val precisions = fixes.map { f -> f.precisionHorizontaleM?.takeIf { it.isFinite() && it > 0.0 } }
            val precision = if (precisions.any { it == null }) null else precisions.maxOf { it!! }
            val simulee = fixes.any { it.simule || it.qualite == GGA_SIMULATION || it.qualiteAnnoncee == GGA_SIMULATION }
            return QualiteFix(precision, source, simulee)
        }

        private val GGA_SIMULATION = io.github.pobsteta.surculus.core.gnss.QualiteFix.SIMULATION
    }
}

/**
 * Domaine de `source_gnss` (décision de Pascal du 2026-11-04, question 4) : le GNSS du téléphone, ou le récepteur avec
 * sa qualité, tirée de l'indicateur de qualité de la trame GGA (NMEA 0183, champ 6 ; lu par `core.gnss.Nmea`), **après**
 * rétrogradation par `PolitiqueFix` (un RTK aux corrections trop vieilles compte autonome).
 */
object SourcesGnss {
    const val TELEPHONE = "telephone"
    const val RECEPTEUR_AUTONOME = "recepteur_autonome"
    const val RECEPTEUR_DGPS = "recepteur_dgps"
    const val RECEPTEUR_RTK_FLOTTANT = "recepteur_rtk_flottant"
    const val RECEPTEUR_RTK_FIXE = "recepteur_rtk_fixe"

    /** Du moins bon au meilleur : la source d'une moyenne est la moins bonne de ses fix. */
    val RANG: List<String> = listOf(TELEPHONE, RECEPTEUR_AUTONOME, RECEPTEUR_DGPS, RECEPTEUR_RTK_FLOTTANT, RECEPTEUR_RTK_FIXE)

    /**
     * La source d'un fix. Les qualités GGA hors du domaine (3 PPS, 6 estimée, 7 manuelle, 8 simulation) ne sont jamais
     * dites meilleures qu'elles ne sont : elles comptent « autonome » (la simulation est dite par `simulee`).
     */
    fun de(fix: FixGnss): String = when (fix.origine) {
        OrigineFix.TELEPHONE -> TELEPHONE
        OrigineFix.RECEPTEUR -> when (fix.qualite) {
            io.github.pobsteta.surculus.core.gnss.QualiteFix.RTK_FIXE -> RECEPTEUR_RTK_FIXE
            io.github.pobsteta.surculus.core.gnss.QualiteFix.RTK_FLOTTANT -> RECEPTEUR_RTK_FLOTTANT
            io.github.pobsteta.surculus.core.gnss.QualiteFix.DGPS -> RECEPTEUR_DGPS
            else -> RECEPTEUR_AUTONOME
        }
    }
}

/** Le besoin « aucun » : le contrôle ne demande aucun travail (sommieR 0.31.1, `R/travaux_qfield.R` l. 61-64). */
object Besoins {
    const val AUCUN = "aucun"

    /** Un besoin est permis s'il est, au caractère près, un code de la couche `codes` de la tournée, ou « aucun ». */
    fun permis(code: String, codesTravaux: Set<String>): Boolean = code == AUCUN || code in codesTravaux
}

/**
 * Niveaux de concurrence d'une plantation : le référentiel `docs/referentiels/concurrences.csv` (mission 092, décision de
 * Pascal du 2026-11-04), qui reflète `SOMMIER_CONCURRENCES` de sommieR 0.31.1 (`R/registre6.R` l. 64), comparés tels quels.
 * Recopiés ici dans l'ordre du fichier : un test (`ConcurrencesReferentielTest`) vérifie l'égalité, codes, libellés et
 * ordre, avec le fichier ; si l'un change sans l'autre, la construction échoue.
 */
object Concurrences {
    /** Codes écrits au journal et dans la couche `controles`, dans l'ordre de la colonne `ordre` du référentiel. */
    val NIVEAUX: List<String> = listOf("faible", "moyenne", "forte")

    /** Libellés d'écran, colonne `libelle` du référentiel (décision de Pascal du 2026-11-04, écart E4). */
    val LIBELLES: Map<String, String> = linkedMapOf("faible" to "Faible", "moyenne" to "Moyenne", "forte" to "Forte")

    fun permise(niveau: String): Boolean = niveau in NIVEAUX
}
