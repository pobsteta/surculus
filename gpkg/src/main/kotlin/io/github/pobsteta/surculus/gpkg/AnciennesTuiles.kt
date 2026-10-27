/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import java.io.File

/**
 * Les tuiles d'ortho IGN préchargées avant la mission 055, qui ne sont plus lues (mission 058).
 *
 * Les noms sont ceux de `TuilesHorsLigne` (`carte/Ortho.kt`, avant 52b457e, lignes 91 et 101) :
 * `racine(dossierTournee) = File(dossierTournee, "tuiles")` et `marqueFin(racine) = File(racine, "ortho-terminee.txt")`.
 * La marque réellement écrite était donc `tuiles/ortho-terminee.txt` (partie avec `tuiles/`) ; la fiche 058 nomme aussi
 * `<dossier de tournée>/ortho-terminee.txt`, effacé s'il existe. Ce sont les **seuls** chemins que [effacer] touche :
 * jamais `travaux.gpkg`, `DCIM/`, `ortho.mbtiles`, `ortho.gpkg`, le journal ni rien d'autre.
 */
object AnciennesTuiles {
    const val DOSSIER = "tuiles"
    const val MARQUE_FIN = "ortho-terminee.txt"

    /** Vrai si le chemin relatif (séparateurs `/`) d'une entrée de tournée est l'un des deux chemins à exclure. */
    fun estAncienne(cheminRelatif: String): Boolean {
        val c = cheminRelatif.trimEnd('/')
        return c == MARQUE_FIN || c == DOSSIER || c.startsWith("$DOSSIER/")
    }

    /**
     * Efface `tuiles/` et `ortho-terminee.txt` du [dossierTournee]. Idempotent : sans eux, ne fait rien.
     * Un lien symbolique n'est jamais suivi (il est supprimé lui-même, sa cible reste). Renvoie vrai si quelque chose a été effacé.
     */
    fun effacer(dossierTournee: File): Boolean {
        var efface = false
        val tuiles = File(dossierTournee, DOSSIER)
        if (java.nio.file.Files.isSymbolicLink(tuiles.toPath())) {
            efface = tuiles.delete() || efface
        } else if (tuiles.isDirectory) {
            efface = tuiles.deleteRecursively() || efface
        }
        val marque = File(dossierTournee, MARQUE_FIN)
        if (marque.isFile) efface = marque.delete() || efface
        return efface
    }

    /** Applique [effacer] à chaque sous-dossier direct de [racineTournees]. Renvoie le nombre de tournées nettoyées. */
    fun effacerPartout(racineTournees: File): Int =
        (racineTournees.listFiles { f -> f.isDirectory } ?: emptyArray()).count { effacer(it) }
}
