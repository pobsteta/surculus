/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import io.github.pobsteta.surculus.carte.OrthoDeTournee
import io.github.pobsteta.surculus.carte.OrthoTournee
import io.github.pobsteta.surculus.carte.TelechargementTuiles
import io.github.pobsteta.surculus.core.carte.EtatOrtho
import io.github.pobsteta.surculus.gpkg.Cartographie
import io.github.pobsteta.surculus.gpkg.GpkgRefuse
import io.github.pobsteta.surculus.gpkg.ImportTournee
import io.github.pobsteta.surculus.gpkg.LecteurTournee
import io.github.pobsteta.surculus.gpkg.Tournee
import io.github.pobsteta.surculus.sauvegarde.JournalApp
import java.io.File
import java.time.Instant

/** Une ligne de la liste des tournées (spec § 4.1). */
data class LigneTournee(
    val dossier: File,
    /** `null` si le `travaux.gpkg` est refusé : [refus] dit pourquoi. */
    val tournee: Tournee?,
    val refus: String?,
    val ortho: OrthoTournee,
) {
    val id: String get() = dossier.name
    val foret: String? get() = tournee?.projet?.foret ?: tournee?.projet?.nom

    /**
     * Tournée rendue ? Point unique de l'état (mission 041). Toutes sont « ouvertes » tant que l'export n'écrit pas la
     * marque « rendue » (spec § 4.1) : on ne l'invente pas.
     */
    val rendue: Boolean get() = false

    val champs: ChampsTournee
        get() = ChampsTournee(foret, tournee?.projet?.operateur, tournee?.projet?.prepareLe, dossier.name, tournee?.projet?.nom)
}

/** La tournée ouverte sur la carte : le fichier, ses placettes et ses surfaces. */
data class TourneeOuverte(val dossier: File, val tournee: Tournee, val cartographie: Cartographie)

/**
 * Les tournées de Surculus : le stockage **privé** de l'application (`files/tournees/<nom>/`), jamais le dossier
 * d'origine ; une tournée n'est jamais écrasée (spec § 4.1, ADR 0001).
 */
object DepotTournees {
    fun racine(ctx: Context) = File(ctx.filesDir, "tournees")

    /** À appeler hors du fil principal. */
    fun lister(ctx: Context): List<LigneTournee> =
        (racine(ctx).listFiles { f -> f.isDirectory && File(f, ImportTournee.NOM_GPKG).isFile } ?: emptyArray())
            .sortedByDescending { it.lastModified() }
            .map { ligne(it) }

    /**
     * Nombre de saisies encore valables de chaque tournée listée, lu dans la projection du journal.
     * HYPOTHÈSE (Q2 de la relecture, à trancher avec la mission 012) : `code_tournee` = nom du dossier de la tournée.
     * À appeler hors du fil principal. Journal illisible : 0 (la vue Kanban ne sert que d'aide, elle ne bloque rien).
     */
    fun saisies(ctx: Context, lignes: List<LigneTournee>): Map<String, Int> {
        val depot = runCatching { JournalApp.depot(ctx) }.getOrNull() ?: return emptyMap()
        return lignes.associate { l ->
            l.id to (runCatching { compterSaisies(depot.projection(l.id)) }.getOrDefault(0))
        }
    }

    /** Réglages Kanban, gardés dans les préférences privées de l'application (jamais dans le journal ni les fichiers de tournée). */
    fun reglagesKanban(ctx: Context): ReglagesKanban {
        val prefs = ctx.applicationContext.getSharedPreferences("kanban", Context.MODE_PRIVATE)
        return ReglagesKanban(object : MagasinKanban {
            override fun lireTexte(cle: String) = prefs.getString(cle, null)
            override fun ecrireTexte(cle: String, valeur: String) { prefs.edit().putString(cle, valeur).apply() }
            override fun lireBooleen(cle: String, parDefaut: Boolean) = prefs.getBoolean(cle, parDefaut)
            override fun ecrireBooleen(cle: String, valeur: Boolean) { prefs.edit().putBoolean(cle, valeur).apply() }
        })
    }

    /** Réglages d'affichage (plein écran, thème), dans les préférences privées de l'application (mission 044). */
    fun reglagesAffichage(ctx: Context): ReglagesAffichage {
        val prefs = ctx.applicationContext.getSharedPreferences("affichage", Context.MODE_PRIVATE)
        return ReglagesAffichage(object : MagasinAffichage {
            override fun lireBooleen(cle: String, parDefaut: Boolean) = prefs.getBoolean(cle, parDefaut)
            override fun ecrireBooleen(cle: String, valeur: Boolean) { prefs.edit().putBoolean(cle, valeur).apply() }
            override fun lireTexte(cle: String) = prefs.getString(cle, null)
            override fun ecrireTexte(cle: String, valeur: String) { prefs.edit().putString(cle, valeur).apply() }
        })
    }

    fun ligne(dossier: File): LigneTournee {
        val gpkg = File(dossier, ImportTournee.NOM_GPKG)
        return try {
            val t = LecteurTournee.lire(gpkg.toPath())
            LigneTournee(dossier, t, null, OrthoDeTournee.etat(dossier, t.empriseForet))
        } catch (e: GpkgRefuse) {
            LigneTournee(dossier, null, e.refus.message, OrthoTournee(EtatOrtho.Absente, null, null))
        } catch (e: Exception) {
            LigneTournee(dossier, null, e.message ?: e.javaClass.simpleName, OrthoTournee(EtatOrtho.Absente, null, null))
        }
    }

    /** Ouvre la tournée pour la carte ; les refus de lecture (point hors de France…) remontent en [GpkgRefuse]. */
    fun ouvrir(dossier: File): TourneeOuverte {
        val gpkg = File(dossier, ImportTournee.NOM_GPKG).toPath()
        return TourneeOuverte(dossier, LecteurTournee.lire(gpkg), LecteurTournee.lireCartographie(gpkg))
    }

    /** Copie le `.zip` choisi dans le sélecteur de fichiers, le contrôle et le range. Renvoie le dossier de la tournée. */
    fun importerZip(ctx: Context, zip: Uri): File {
        val tmp = nouveauTemporaire(ctx)
        try {
            ctx.contentResolver.openInputStream(zip)!!.use { ImportTournee.extraireZip(it, tmp.toPath()) }
        } catch (e: Exception) {
            tmp.deleteRecursively() // zip refusé ou coupé : rien ne reste dans le cache
            throw e
        }
        return terminer(ctx, tmp)
    }

    /** Copie le dossier choisi dans le sélecteur (`ACTION_OPEN_DOCUMENT_TREE`), le contrôle et le range. */
    fun importerDossier(ctx: Context, arbre: Uri): File {
        val tmp = nouveauTemporaire(ctx)
        val racineDoc = DocumentsContract.getTreeDocumentId(arbre)
        try {
            copierArbre(ctx, arbre, racineDoc, tmp)
        } catch (e: Exception) {
            tmp.deleteRecursively()
            throw e
        }
        return terminer(ctx, tmp)
    }

    private fun nouveauTemporaire(ctx: Context): File {
        val tmp = File(ctx.cacheDir, "import-${System.nanoTime()}")
        check(tmp.mkdirs()) { "Dossier temporaire impossible : $tmp" }
        return tmp
    }

    private fun terminer(ctx: Context, tmp: File): File {
        val rangee = ImportTournee.finaliser(tmp.toPath(), racine(ctx).toPath(), Instant.now()).toFile()
        // L'ortho est préchargée à l'ouverture de la tournée (ADR 0009) ; l'emprise hors de France ou trop grande reste « absente ».
        planifierOrtho(ctx, rangee, remplacer = false)
        return rangee
    }

    /** Planifie le préchargement ; renvoie la raison d'un refus d'emprise, `null` sinon. */
    fun planifierOrtho(ctx: Context, dossier: File, remplacer: Boolean) =
        runCatching {
            val t = LecteurTournee.lire(File(dossier, ImportTournee.NOM_GPKG).toPath())
            TelechargementTuiles.planifier(ctx, dossier, t.empriseForet, remplacer)
        }.getOrNull()

    private fun copierArbre(ctx: Context, arbre: Uri, idDoc: String, cible: File) {
        val enfants = DocumentsContract.buildChildDocumentsUriUsingTree(arbre, idDoc)
        val colonnes = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        ctx.contentResolver.query(enfants, colonnes, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val nom = c.getString(1).replace('/', '_').takeIf { it != ".." && it != "." } ?: continue
                if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    copierArbre(ctx, arbre, id, File(cible, nom).apply { mkdirs() })
                } else {
                    val doc = DocumentsContract.buildDocumentUriUsingTree(arbre, id)
                    ctx.contentResolver.openInputStream(doc)?.use { entree -> File(cible, nom).outputStream().use { entree.copyTo(it) } }
                }
            }
        }
    }
}
