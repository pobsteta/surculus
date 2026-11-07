/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.pobsteta.surculus.controle.ArriveeControle
import io.github.pobsteta.surculus.data.session.JournalDeTournee
import io.github.pobsteta.surculus.gpkg.FauteDeSaisie
import io.github.pobsteta.surculus.gpkg.ImportTournee
import io.github.pobsteta.surculus.gpkg.LecteurTournee
import io.github.pobsteta.surculus.gpkg.RenduTournee
import io.github.pobsteta.surculus.gpkg.ResultatRendu
import io.github.pobsteta.surculus.messages.JournalTechnique
import io.github.pobsteta.surculus.sauvegarde.JournalApp
import io.github.pobsteta.surculus.sauvegarde.StockageMediaStore
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Ce que l'écran montre après « Rendre la tournée » (mission 090). */
sealed interface IssueRendu {
    /** Rendue : [chemin] est ce que l'agent retrouve sur le téléphone (`Documents/Surculus/….zip`) ; [prive] sert à « Partager ». */
    data class Rendue(val controles: Int, val plants: Int, val chemin: String, val prive: File) : IssueRendu

    data class Refusee(val fautes: List<FauteDeSaisie>) : IssueRendu

    data class Impossible(val texte: String) : IssueRendu
}

/**
 * Mission 090 : « Rendre la tournée » sur le téléphone (spec § 4.8 ; décisions de Pascal du 2026-11-03). Lit le journal
 * de **cette** tournée (`code_tournee` = nom du dossier, hypothèse de la 061), rend par [RenduTournee], garde le `.zip`
 * dans le stockage privé (pour « Partager », par `FileProvider`) et le publie dans `Documents/Surculus/` (MediaStore,
 * comme la sauvegarde, ADR 0005). Un nouveau rendu n'écrase rien : le nom prend l'horodatage s'il est déjà pris.
 * Tout ici est bloquant : à appeler hors du fil principal.
 */
object RenduApp {
    /** Dossier privé des `.zip` rendus (`files/rendus/`), partagé par `FileProvider` (`res/xml/chemins_partages.xml`). */
    fun dossierRendus(ctx: Context) = File(ctx.filesDir, "rendus")

    private val HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.of("Europe/Paris"))

    /**
     * Nom du `.zip` : `<tournée>.zip`, ou `<tournée>-<aaaammjj-hhmmss>.zip` si ce nom est déjà pris (un nouveau rendu
     * n'écrase jamais l'ancien, décision de Pascal du 2026-11-03), suffixé `-2`, `-3`… si besoin.
     */
    fun nomDuZip(id: String, maintenant: Instant, pris: (String) -> Boolean): String {
        val simple = "$id.zip"
        if (!pris(simple)) return simple
        val base = "$id-${HORODATAGE.format(maintenant)}"
        return generateSequence(1) { it + 1 }.map { if (it == 1) "$base.zip" else "$base-$it.zip" }.first { !pris(it) }
    }

    private fun dossier(ctx: Context, id: String) = File(DepotTournees.racine(ctx), id)

    private fun lignes(ctx: Context, id: String) = JournalDeTournee(JournalApp.depot(ctx), id).lignesCourantes()

    /** Décompte pour la confirmation (« 2 contrôles, 8 plants : rendre ? »). */
    fun decompter(ctx: Context, id: String): RenduTournee.Decompte = RenduTournee.decompter(dossier(ctx, id).toPath(), lignes(ctx, id))

    fun estRendue(ctx: Context, id: String): Boolean = RenduTournee.estRendue(dossier(ctx, id).toPath())

    /** « Rouvrir la tournée » : après l'avertissement de l'écran. */
    fun rouvrir(ctx: Context, id: String) = RenduTournee.rouvrir(dossier(ctx, id).toPath())

    /** Rend la tournée [id] ; [surEtape] est appelé à l'écriture (après la vérification). */
    fun rendre(ctx: Context, id: String, surEtape: (EtapeRendu) -> Unit = {}): IssueRendu {
        val d = dossier(ctx, id)
        return try {
            surEtape(EtapeRendu.VERIFICATION)
            val lignes = lignes(ctx, id)
            val tournee = LecteurTournee.lire(File(d, ImportTournee.NOM_GPKG).toPath())
            val decompte = RenduTournee.decompter(d.toPath(), lignes)
            // Export déterministe (ADR 0005, « horodatages fixés depuis le journal ») : l'instant de la dernière saisie.
            val exporteLe = decompte.derniereSaisie ?: Instant.now()
            val stockage = StockageMediaStore(ctx)
            val prives = dossierRendus(ctx).apply { mkdirs() }
            val nom = nomDuZip(id, Instant.now()) { stockage.existe(it) || File(prives, it).exists() }
            val prive = File(prives, nom)
            surEtape(EtapeRendu.ECRITURE)
            when (val r = RenduTournee.rendre(d.toPath(), lignes, prive.toPath(), version(ctx), exporteLe, ArriveeControle.essencesDe(tournee))) {
                is ResultatRendu.Refusee -> IssueRendu.Refusee(r.fautes)
                is ResultatRendu.Impossible -> IssueRendu.Impossible(r.texte)
                is ResultatRendu.Rendue -> {
                    val chemin = try {
                        stockage.publier(nom, "application/zip", prive)
                    } catch (e: Exception) {
                        // Le zip est rendu et gardé en privé : « Partager » reste possible ; on le dit sans bloquer.
                        JournalTechnique.avertir("Rendu", "publication dans Documents/Surculus impossible", e)
                        null
                    }
                    IssueRendu.Rendue(r.bilan.controles, r.bilan.plants, chemin ?: TEXTE_NON_PUBLIE, prive)
                }
            }
        } catch (e: Exception) {
            JournalTechnique.avertir("Rendu", "rendu de la tournée $id impossible", e)
            IssueRendu.Impossible(RenduTournee.texteDePanne(e))
        }
    }

    /** Ouvre le partage Android du `.zip` rendu [prive] (courriel, Nextcloud, câble…). */
    fun partager(ctx: Context, prive: File) {
        val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.rendus", prive)
        val envoi = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, prive.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(envoi, prive.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun version(ctx: Context): String =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "inconnue"

    const val TEXTE_NON_PUBLIE = "copie dans Documents/Surculus impossible : utiliser « Partager »"
}

/** Ce que l'écran affiche pendant le rendu : « Vérification… » puis « Écriture… ». */
enum class EtapeRendu { VERIFICATION, ECRITURE }
