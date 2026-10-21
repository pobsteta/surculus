package io.github.pobsteta.surculus.core.dictee

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.pobsteta.surculus.core.fixtures.CasFixture
import io.github.pobsteta.surculus.core.fixtures.Depot
import io.github.pobsteta.surculus.core.fixtures.FichierFixture
import java.io.File

/** Lecture de `fixtures/dictee/` (mission 025). */
object DicteeFixtures {
    val dossier: File get() = File(Depot.racine, "fixtures/dictee")

    val fichiers: List<FichierFixture> by lazy {
        dossier.listFiles { f -> f.extension == "json" }!!.sortedBy { it.name }.map {
            FichierFixture(it.relativeTo(Depot.racine).path, JsonParser.parseString(it.readText()).asJsonObject)
        }
    }

    fun famille(nom: String): FichierFixture = fichiers.single { it.famille == nom }

    fun cas(nom: String): List<CasFixture> = famille(nom).cas

    /** Mots du vocabulaire de vosk-model-small-fr-0.22, un par ligne (fixture versionnée, ADR 0004). */
    val vocabulaire: List<String> by lazy {
        File(dossier, "vocabulaire-vosk-model-small-fr-0.22.txt").readLines(Charsets.UTF_8)
    }

    fun objet(c: CasFixture, cle: String): JsonObject = c.json.getAsJsonObject(cle)
}
