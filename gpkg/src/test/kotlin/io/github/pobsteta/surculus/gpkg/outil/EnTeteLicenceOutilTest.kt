/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg.outil

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertTrue

/**
 * Mission 091, repris de la relecture de la 090 : l'outil de test de la CI d'import `RendreScenario.kt` porte
 * l'en-tête de licence GPL des autres sources (par exemple `core/.../session/Ports.kt`). Écrit par le `testeur`.
 */
class EnTeteLicenceOutilTest {
    private val racine: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }.first { Files.exists(it.resolve("settings.gradle.kts")) }

    @Test
    fun `RendreScenario porte l'en-tete GPL du projet`() {
        val f = racine.resolve("gpkg/src/test/kotlin/io/github/pobsteta/surculus/gpkg/outil/RendreScenario.kt")
        val texte = Files.readString(f)
        val entete = texte.substringBefore("package ")
        assertTrue(texte.startsWith("/*\n * Surculus — suivi des travaux de plantation en forêt."), "l'en-tête ouvre le fichier")
        assertTrue("Copyright (C) 2026 Pascal Obstétar" in entete, "titulaire des droits")
        assertTrue("licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE)" in entete, "licence nommée")
    }
}
