/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.session.LigneCourante
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant

/** Issue d'un rendu de tournée (mission 090). */
sealed interface ResultatRendu {
    /** Le `.zip` est écrit à [zip] et la tournée est marquée rendue. */
    data class Rendue(val zip: Path, val bilan: BilanExport) : ResultatRendu

    /** Une faute au moins : rien n'est écrit, la tournée reste ouverte ; chaque faute renvoie à la saisie. */
    data class Refusee(val fautes: List<FauteDeSaisie>) : ResultatRendu

    /** L'écriture a échoué (place, fichier) : rien n'est laissé, la tournée reste ouverte ; [texte] dit quoi faire. */
    data class Impossible(val texte: String) : ResultatRendu
}

/**
 * Mission 090 : rendre la tournée (spec § 4.8 ; ADR 0007) : saisies du journal → vérification complète (§ 6) →
 * `<tournée>.zip` (`travaux.gpkg` complété et `DCIM/`, rien d'autre) → marque « rendue ».
 *
 * - **Tout ou rien** : une faute, et rien n'est écrit ; la vérification est faite contrôle par contrôle, pour lister
 *   une faute par placette au lieu de s'arrêter à la première, puis sur toute la tournée.
 * - La copie privée de la tournée n'est **jamais modifiée** (§ 4.1) : la marque est un fichier à côté
 *   ([MARQUE]), jamais mis dans le zip.
 * - Le journal n'est que lu (§ 5) ; à lignes égales et même [exporteLe], deux rendus ont le même contenu.
 * - Interruption : le `travaux.gpkg` et le zip sont écrits dans un dossier de travail, le zip est déplacé d'un coup à
 *   [destination], la marque est posée en dernier. Une coupure laisse la tournée ouverte, à rendre de nouveau.
 */
object RenduTournee {
    /** Marque « rendue », dans le dossier privé de la tournée. */
    const val MARQUE = "rendue.json"

    /** Décompte d'un rendu, pour la confirmation (« 2 contrôles, 8 plants : rendre ? ») ; le journal n'est que lu. */
    data class Decompte(val controles: Int, val plants: Int, val derniereSaisie: Instant?)

    fun decompter(dossierTournee: Path, lignes: List<LigneCourante>): Decompte {
        val c = SaisiesDuJournal.construire(LecteurTournee.lire(dossierTournee.resolve(ImportTournee.NOM_GPKG)), lignes)
        return Decompte(c.saisies.controles.size, c.saisies.plants.size, c.derniereSaisie)
    }

    /**
     * @param dossierTournee la copie privée de la tournée (`travaux.gpkg`, `DCIM/`), jamais modifiée.
     * @param lignes lignes courantes du journal de **cette** tournée.
     * @param destination le `.zip` à écrire (remplacé s'il existe : le nom est choisi par l'appelant).
     * @param exporteLe horloge injectée (export déterministe, § 5).
     */
    fun rendre(
        dossierTournee: Path,
        lignes: List<LigneCourante>,
        destination: Path,
        versionSurculus: String,
        exporteLe: Instant,
        essences: Essences? = null,
    ): ResultatRendu {
        val source = dossierTournee.resolve(ImportTournee.NOM_GPKG)
        val tournee = try {
            LecteurTournee.lire(source)
        } catch (e: GpkgRefuse) {
            return ResultatRendu.Refusee(listOf(FautesDuRendu.de(e.refus, ReperesSaisies(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap()), null)))
        }
        val conversion = SaisiesDuJournal.construire(tournee, lignes)
        val fautes = verifier(tournee, conversion, essences)
        if (fautes.isNotEmpty()) return ResultatRendu.Refusee(fautes)

        val travail = try {
            Files.createTempDirectory("rendu-")
        } catch (e: IOException) {
            return ResultatRendu.Impossible(TEXTE_ECRITURE)
        }
        try {
            val gpkg = travail.resolve(ImportTournee.NOM_GPKG)
            val bilan = ExportTravaux.exporter(source, gpkg, conversion.saisies, versionSurculus, exporteLe, essences)
            val zip = travail.resolve(destination.fileName.toString())
            ExportZip.ecrireRendu(gpkg, dossierTournee.resolve(ExportZip.DCIM), zip)
            deplacer(zip, destination)
            ecrireMarque(dossierTournee, destination, bilan, versionSurculus, exporteLe)
            return ResultatRendu.Rendue(destination, bilan)
        } catch (e: GpkgRefuse) {
            // Après la vérification, un refus d'écriture SQLite est une panne d'écriture (disque plein…), pas une faute de saisie.
            if (e.refus is RefusGpkg.PasUnGeoPackage) return ResultatRendu.Impossible(if (estPlein(e)) TEXTE_PLACE else TEXTE_ECRITURE)
            return ResultatRendu.Refusee(listOf(FautesDuRendu.de(e.refus, conversion.reperes, null)))
        } catch (e: IOException) {
            if (estPlein(e)) return ResultatRendu.Impossible(TEXTE_PLACE)
            return ResultatRendu.Impossible(TEXTE_ECRITURE)
        } finally {
            travail.toFile().deleteRecursively()
        }
    }

    /** La tournée est-elle rendue (état « rendue » de la liste des tournées, § 4.1) ? */
    fun estRendue(dossierTournee: Path): Boolean = Files.isRegularFile(dossierTournee.resolve(MARQUE))

    /**
     * « Rouvrir la tournée » (décision de Pascal du 2026-11-03) : la marque est retirée, la tournée redevient ouverte. Les
     * `.zip` déjà écrits restent où ils sont ; l'appelant a averti l'agent qu'un contrôle déjà importé dans sommieR ne sera
     * pas mis à jour (l'import ignore un identifiant déjà présent, `R/travaux_qfield.R` l. 183-192).
     */
    fun rouvrir(dossierTournee: Path) {
        Files.deleteIfExists(dossierTournee.resolve(MARQUE))
    }

    const val TEXTE_ECRITURE = "le fichier n'a pas pu être écrit : réessayer ; si cela recommence, redémarrer le téléphone"
    const val TEXTE_PLACE = "place insuffisante sur le téléphone : libérer de la place, puis réessayer"

    /** Vérification contrôle par contrôle (une faute par placette au plus), puis de toute la tournée. */
    private fun verifier(tournee: Tournee, c: SaisiesConverties, essences: Essences?): List<FauteDeSaisie> {
        val fautes = ArrayList(c.fautes)
        val s = c.saisies
        val plantsParPlacette = s.plants.groupBy { it.plant.placette }
        for (controle in s.controles) {
            val seul = Saisies(s.operateur, controles = listOf(controle), plants = plantsParPlacette[controle.placette].orEmpty())
            try {
                ValidationExport.verifier(tournee, seul, essences)
            } catch (e: GpkgRefuse) {
                fautes += FautesDuRendu.de(e.refus, c.reperes, c.reperes.placetteDuControle(controle.uuid.valeur))
            }
        }
        if (fautes.isEmpty()) {
            try {
                ValidationExport.verifier(tournee, s, essences)
            } catch (e: GpkgRefuse) {
                fautes += FautesDuRendu.de(e.refus, c.reperes, null)
            }
        }
        return fautes.distinct()
    }

    private fun deplacer(de: Path, vers: Path) {
        Files.createDirectories(vers.toAbsolutePath().parent)
        // Copie à côté de la destination sous un nom caché, puis déplacement d'un coup : jamais de zip à moitié écrit.
        val voisin = vers.toAbsolutePath().resolveSibling(".${vers.fileName}.part")
        try {
            Files.move(de, voisin, StandardCopyOption.REPLACE_EXISTING)
            try {
                Files.move(voisin, vers, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: AtomicMoveNotSupportedException) {
                Files.move(voisin, vers, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(voisin)
        }
    }

    private fun ecrireMarque(dossier: Path, zip: Path, bilan: BilanExport, version: String, exporteLe: Instant) {
        fun texte(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        val json = "{\"rendue_le\":${texte(exporteLe.toString())},\"zip\":${texte(zip.fileName.toString())}," +
            "\"version_surculus\":${texte(version)},\"controles\":${bilan.controles},\"plants\":${bilan.plants}}\n"
        val tmp = dossier.resolve(".$MARQUE.part")
        try {
            Files.writeString(tmp, json)
            try {
                Files.move(tmp, dossier.resolve(MARQUE), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: AtomicMoveNotSupportedException) {
                Files.move(tmp, dossier.resolve(MARQUE), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    private fun estPlein(e: Throwable): Boolean =
        generateSequence<Throwable>(e) { it.cause }.any { (it.message ?: "").contains("No space left", ignoreCase = true) || (it.message ?: "").contains("ENOSPC") }
}
