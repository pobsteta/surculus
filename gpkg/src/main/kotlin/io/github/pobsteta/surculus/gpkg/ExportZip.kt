/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * L'export rendu à sommieR : un `.zip` **à plat** (ADR 0007) dont les entrées sont à la racine, sans dossier
 * parent : `travaux.gpkg`, puis `DCIM/…` si des photos sont jointes. Décompressé, c'est le dossier que lit
 * `sommier_importer_qfield_travaux()`. Les entrées sont rangées par nom et datées du 1980-01-01 (heure locale),
 * pour que deux exports du même état aient les mêmes entrées.
 */
object ExportZip {
    const val GPKG = "travaux.gpkg"
    private val DATE_FIXE = LocalDateTime.of(1980, 1, 1, 0, 0)

    /** Zippe le [dossier] (qui contient `travaux.gpkg`, et éventuellement `DCIM/`) vers [zip]. */
    fun ecrire(dossier: Path, zip: Path) {
        require(Files.isRegularFile(dossier.resolve(GPKG))) { "Pas de $GPKG dans $dossier" }
        val racine = dossier.toFile()
        // Les dossiers comptent aussi, même vides : `DCIM/` de l'aller reste dans le zip (le dossier attendu par l'import).
        val fichiers = racine.walkTopDown().filter { it != racine && (it.isFile || it.isDirectory) }
            .map { it.relativeTo(racine).invariantSeparatorsPath + if (it.isDirectory) "/" else "" }
            .sortedWith(compareBy<String>({ it != GPKG }, { it }))
            .toList()
        val millis = DATE_FIXE.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Files.createDirectories(zip.toAbsolutePath().parent)
        ZipOutputStream(Files.newOutputStream(zip)).use { out ->
            for (nom in fichiers) {
                val e = ZipEntry(nom)
                e.time = millis
                out.putNextEntry(e)
                if (!nom.endsWith("/")) Files.copy(dossier.resolve(nom), out)
                out.closeEntry()
            }
        }
    }
}
