/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

/**
 * Ouverture d'une tournée de sommieR (spec § 4.1) : le dossier de `sommier_projet_qfield_travaux()` ou son `.zip` est
 * copié dans le stockage privé de Surculus, sous un nom neuf : **une tournée n'est jamais écrasée**.
 * Pas d'Android ici : la copie depuis le sélecteur de fichiers est faite par l'application, ce code range et contrôle.
 */
object ImportTournee {
    const val NOM_GPKG = "travaux.gpkg"

    /** Au-delà, un `.zip` est refusé (bombe de décompression) : une tournée avec ses photos tient largement dedans. */
    const val OCTETS_MAX = 4L * 1024 * 1024 * 1024

    private val HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.of("Europe/Paris"))

    /** Décompresse [entree] dans [cible] ; refuse toute entrée qui sortirait de [cible] (zip slip). */
    fun extraireZip(entree: InputStream, cible: Path) {
        val racine = cible.toAbsolutePath().normalize()
        Files.createDirectories(racine)
        var total = 0L
        ZipInputStream(entree).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val sortie = racine.resolve(e.name).normalize()
                if (!sortie.startsWith(racine)) throw IOException("Entrée du zip hors du dossier cible : ${e.name}")
                if (e.isDirectory) {
                    Files.createDirectories(sortie)
                    continue
                }
                Files.createDirectories(sortie.parent)
                Files.newOutputStream(sortie).use { out ->
                    val tampon = ByteArray(64 * 1024)
                    while (true) {
                        val n = zip.read(tampon)
                        if (n < 0) break
                        total += n
                        if (total > OCTETS_MAX) throw IOException("Zip trop volumineux (plus de ${OCTETS_MAX / (1024 * 1024)} Mo décompressés).")
                        out.write(tampon, 0, n)
                    }
                }
            }
        }
    }

    /** Dossier qui porte `travaux.gpkg` : [dossier] lui-même, ou un sous-dossier (un zip contient souvent un dossier racine). */
    fun trouverTournee(dossier: Path): Path? {
        if (Files.isRegularFile(dossier.resolve(NOM_GPKG))) return dossier
        Files.list(dossier).use { enfants ->
            return enfants.filter { Files.isDirectory(it) && Files.isRegularFile(it.resolve(NOM_GPKG)) }.sorted().findFirst().orElse(null)
        }
    }

    /**
     * Contrôle la copie [temporaire] (lecture du `travaux.gpkg` par [LecteurTournee], mêmes refus) puis la range dans
     * [racine] sous un nom neuf `<forêt>-<date>`. Renvoie le dossier de la tournée. Rien n'est laissé en cas de refus.
     */
    fun finaliser(temporaire: Path, racine: Path, maintenant: Instant): Path {
        try {
            val tournee = trouverTournee(temporaire) ?: throw GpkgRefuse(RefusGpkg.PasUnGeoPackage("aucun $NOM_GPKG dans ce dossier"))
            val lue = LecteurTournee.lire(tournee.resolve(NOM_GPKG))
            val nom = nomUnique(racine, lue.projet?.foret ?: lue.projet?.nom ?: "tournee", maintenant)
            Files.createDirectories(racine)
            val cible = racine.resolve(nom)
            Files.move(tournee, cible, StandardCopyOption.ATOMIC_MOVE)
            return cible
        } finally {
            temporaire.toFile().deleteRecursively()
        }
    }

    /** `<forêt>-aaaammjj-hhmmss`, suffixé `-2`, `-3`… si ce nom existe déjà : jamais d'écrasement. */
    fun nomUnique(racine: Path, base: String, maintenant: Instant): String {
        val propre = Normalizer.normalize(base, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9]+"), "-").trim('-').take(40).ifEmpty { "tournee" }
        val brut = "$propre-${HORODATAGE.format(maintenant)}"
        if (!Files.exists(racine.resolve(brut))) return brut
        return generateSequence(2) { it + 1 }.map { "$brut-$it" }.first { !Files.exists(racine.resolve(it)) }
    }
}
