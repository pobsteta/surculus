/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 091, décision de Pascal du 2026-11-04, point 7 : le domaine de la concurrence du code ([Concurrences]) est
 * **égal** au référentiel `docs/referentiels/concurrences.csv` (mission 092) : mêmes codes, même ordre (colonne `ordre`),
 * mêmes libellés d'écran. Le fichier est lu, jamais recopié dans le test.
 */
class ConcurrencesReferentielTest {
    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }

    /** Lignes de données du CSV (sans commentaires `#` ni en-tête) : champs entre guillemets, séparés par des virgules. */
    private fun lignes(): List<Map<String, String>> {
        val texte = Files.readAllLines(racine.resolve("docs/referentiels/concurrences.csv")).filter { it.isNotBlank() && !it.startsWith("#") }
        val entete = texte.first().split(',')
        return texte.drop(1).map { l ->
            val champs = Regex("\"((?:[^\"]|\"\")*)\"").findAll(l).map { it.groupValues[1].replace("\"\"", "\"") }.toList()
            assertEquals(entete.size, champs.size, "ligne mal formée : $l")
            entete.zip(champs).toMap()
        }
    }

    @Test
    fun `les codes et leur ordre sont ceux du referentiel`() {
        val l = lignes().sortedBy { it.getValue("ordre").toInt() }
        assertTrue(l.isNotEmpty(), "référentiel vide")
        assertEquals(l.map { it.getValue("code") }, Concurrences.NIVEAUX)
    }

    @Test
    fun `les libelles d'ecran sont ceux du referentiel`() {
        assertEquals(lignes().associate { it.getValue("code") to it.getValue("libelle") }, Concurrences.LIBELLES)
        assertEquals(Concurrences.NIVEAUX, Concurrences.LIBELLES.keys.toList())
    }
}
