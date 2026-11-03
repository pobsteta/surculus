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

    /** Le dossier des photos, présent dans le zip même vide (spec § 4.8 ; l'aller de sommieR le livre vide). */
    const val DCIM = "DCIM"

    /**
     * Mission 090 : le zip du rendu de la tournée, à liste fixe (décision de Pascal du 2026-11-03) : [gpkg] sous le nom
     * `travaux.gpkg`, puis `DCIM/` (même vide) et son contenu tel que la tournée l'a livré, rien d'autre (ni ortho, ni
     * projet QGIS, ni marque « rendue »). [dcim] absent : `DCIM/` est écrit vide. Mêmes dates fixes et même ordre que
     * [ecrire] : deux rendus du même état ont les mêmes entrées.
     */
    fun ecrireRendu(gpkg: Path, dcim: Path?, zip: Path) {
        require(Files.isRegularFile(gpkg)) { "Pas de $GPKG à zipper : $gpkg" }
        val photos = dcim?.toFile()?.takeIf { it.isDirectory }?.let { d ->
            d.walkTopDown().filter { it != d && (it.isFile || it.isDirectory) }
                .map { it to "$DCIM/" + it.relativeTo(d).invariantSeparatorsPath + if (it.isDirectory) "/" else "" }
                .sortedBy { it.second }
                .toList()
        }.orEmpty()
        val millis = DATE_FIXE.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Files.createDirectories(zip.toAbsolutePath().parent)
        ZipOutputStream(Files.newOutputStream(zip)).use { out ->
            fun entree(nom: String, source: Path?) {
                out.putNextEntry(ZipEntry(nom).also { it.time = millis })
                source?.let { Files.copy(it, out) }
                out.closeEntry()
            }
            entree(GPKG, gpkg)
            entree("$DCIM/", null)
            for ((f, nom) in photos) entree(nom, if (nom.endsWith("/")) null else f.toPath())
        }
    }

    /** Zippe le [dossier] (qui contient `travaux.gpkg`, et éventuellement `DCIM/`) vers [zip]. */
    fun ecrire(dossier: Path, zip: Path) {
        require(Files.isRegularFile(dossier.resolve(GPKG))) { "Pas de $GPKG dans $dossier" }
        val racine = dossier.toFile()
        // Les dossiers comptent aussi, même vides : `DCIM/` de l'aller reste dans le zip (le dossier attendu par l'import).
        // `tuiles/` et `ortho-terminee.txt` (ortho préchargée avant la 055) ne partent jamais vers sommieR (mission 058).
        val fichiers = racine.walkTopDown().filter { it != racine && (it.isFile || it.isDirectory) }
            .map { it.relativeTo(racine).invariantSeparatorsPath + if (it.isDirectory) "/" else "" }
            .filterNot { AnciennesTuiles.estAncienne(it) }
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
