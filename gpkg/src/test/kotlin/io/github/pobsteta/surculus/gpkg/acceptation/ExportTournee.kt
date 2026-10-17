package io.github.pobsteta.surculus.gpkg.acceptation

import com.google.gson.JsonObject
import java.nio.file.Path

/** Résultat d'un export : un .zip écrit, ou un refus avec un motif en code stable (fixtures/README.md). */
sealed interface ResultatExport {
    data class Accepte(val zip: Path) : ResultatExport
    data class Refuse(val motif: String) : ResultatExport
}

/**
 * Interface de l'export, déclarée dans le code de test (journal, 2026-10-16) : la mission 006 la branche
 * sur son écrivain GeoPackage dans [BranchementExport], sans toucher aux cas de la mission 020.
 *
 * [entree] est l'objet `entree` d'un cas de `fixtures/acceptation-export/`, tel quel (tournée de départ et
 * saisies). L'export écrit son .zip sous [dossier].
 */
fun interface ExportTournee {
    fun exporter(entree: JsonObject, dossier: Path): ResultatExport
}
