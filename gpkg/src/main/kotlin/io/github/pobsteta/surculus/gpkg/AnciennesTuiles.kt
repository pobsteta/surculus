/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Les tuiles d'ortho IGN préchargées avant la mission 055, qui ne sont plus lues (mission 058).
 *
 * Le nom est celui de `TuilesHorsLigne` (`carte/Ortho.kt`, avant 52b457e, ligne 91) : `racine(dossierTournee) =
 * File(dossierTournee, "tuiles")`. La marque de fin était `marqueFin(racine) = File(racine, "ortho-terminee.txt")`
 * (ligne 101), donc **dans** `tuiles/` : elle part avec lui. [effacer] ne touche que `<tournée>/tuiles/` : jamais
 * `travaux.gpkg`, `DCIM/`, `ortho.mbtiles`, `ortho.gpkg`, le journal ni rien d'autre. L'export, lui, exclut aussi
 * `ortho-terminee.txt` (garde-fou gratuit).
 */
object AnciennesTuiles {
    const val DOSSIER = "tuiles"
    const val MARQUE_FIN = "ortho-terminee.txt"

    private fun estLien(f: File) = Files.isSymbolicLink(f.toPath())

    /** Vrai si le chemin relatif (séparateurs `/`) d'une entrée de tournée est l'un des deux chemins à exclure. */
    fun estAncienne(cheminRelatif: String): Boolean {
        val c = cheminRelatif.trimEnd('/')
        return c == MARQUE_FIN || c == DOSSIER || c.startsWith("$DOSSIER/")
    }

    /**
     * Efface `tuiles/` du [dossierTournee]. Idempotent. Aucun lien symbolique n'est jamais suivi : ni [dossierTournee]
     * s'il en est un (rien n'est fait), ni `tuiles`, ni un lien à l'intérieur ; le lien lui-même est supprimé, sa cible
     * reste. Renvoie vrai si `tuiles/` a été effacé.
     */
    fun effacer(dossierTournee: File): Boolean {
        if (estLien(dossierTournee)) return false
        val tuiles = File(dossierTournee, DOSSIER).toPath()
        if (!Files.exists(tuiles, LinkOption.NOFOLLOW_LINKS)) return false
        Files.walkFileTree(tuiles, object : SimpleFileVisitor<Path>() {
            // Sans FOLLOW_LINKS, un lien (même vers un dossier) arrive ici comme un fichier : supprimé, jamais traversé.
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.deleteIfExists(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: java.io.IOException?): FileVisitResult {
                Files.deleteIfExists(dir)
                return FileVisitResult.CONTINUE
            }
        })
        return !Files.exists(tuiles, LinkOption.NOFOLLOW_LINKS)
    }

    private fun tournees(racineTournees: File): List<File> =
        (racineTournees.listFiles { f -> f.isDirectory && !estLien(f) } ?: emptyArray()).toList()

    /** Applique [effacer] à chaque sous-dossier direct (hors liens) de [racineTournees]. Renvoie le nombre de tournées nettoyées. */
    fun effacerPartout(racineTournees: File): Int = tournees(racineTournees).count { effacer(it) }

    /** Vrai s'il reste un `<tournée>/tuiles` (même lien cassé) dans une tournée de [racineTournees]. */
    fun resteDesTuiles(racineTournees: File): Boolean =
        tournees(racineTournees).any { Files.exists(File(it, DOSSIER).toPath(), LinkOption.NOFOLLOW_LINKS) }
}
