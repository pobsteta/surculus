package io.github.pobsteta.surculus.gpkg.acceptation

/**
 * Seul point que la mission 006 modifie : rendre ici l'export réel de `:gpkg`, puis retirer le
 * `@Disabled` de [ExportAcceptationTest]. Aucun cas de `fixtures/acceptation-export/` ne se modifie.
 */
fun exportATester(): ExportTournee =
    throw NotImplementedError("mission 006 : brancher l'export de :gpkg ici")
