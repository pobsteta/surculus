/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.sauvegarde

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.github.pobsteta.surculus.data.sauvegarde.FormatSursync
import io.github.pobsteta.surculus.data.sauvegarde.StockagePhotos
import io.github.pobsteta.surculus.data.sauvegarde.StockageSauvegardes
import java.io.File
import java.io.IOException

/**
 * Sauvegardes dans `Documents/Surculus/` du stockage partagé (ADR 0005), ce qui survit à la désinstallation.
 *
 * - Android 10 et plus : MediaStore avec `IS_PENDING` (le fichier n'est visible qu'une fois complet, jamais à
 *   0 octet : c'est pourquoi on n'emploie pas le SAF). MediaStore ne remplace jamais un fichier existant : un
 *   nom pris reçoit un suffixe du système.
 * - Android 8 et 9 : pas de MediaStore pour les documents ; écriture directe dans le dossier public
 *   `Documents/Surculus/` (permission `WRITE_EXTERNAL_STORAGE`, limitée à l'API 28 par le manifeste), sous un
 *   nom `.part` renommé une fois complet, et le fichier survit aussi à la désinstallation.
 *   **À faire** : `WRITE_EXTERNAL_STORAGE` doit être demandée à l'exécution sur les API 26 à 28 ; ce n'est pas
 *   encore fait, ce sera la mission qui branchera la sauvegarde sur les saisies. Sans elle, l'écriture échoue.
 *
 * Limite de l'API 29 et plus : sans autorisation étendue, l'application ne voit que les fichiers qu'elle a
 * elle-même créés ; après une réinstallation, `noms()` ne liste pas ceux de l'installation précédente. La
 * rotation ne touche donc que les fichiers visibles, et MediaStore suffixe un nom en collision.
 */
class StockageMediaStore(context: Context) : StockageSauvegardes {
    private val resolver = context.applicationContext.contentResolver

    private val dossierLegacy: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), DOSSIER)

    override fun noms(): Set<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return dossierLegacy.list()?.filter { it.endsWith(FormatSursync.EXTENSION) }?.toSet().orEmpty()
        }
        val noms = mutableSetOf<String>()
        resolver.query(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?", arrayOf("Documents/$DOSSIER/"), null,
        )?.use { c -> while (c.moveToNext()) noms += c.getString(0) }
        return noms
    }

    override fun ecrire(nom: String, octets: ByteArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dossier = dossierLegacy
            if (!dossier.isDirectory && !dossier.mkdirs()) throw IOException("dossier $dossier non créé")
            val cible = File(dossier, nom)
            if (cible.exists()) throw IOException("$nom existe déjà : jamais écrasé")
            val partiel = File(dossier, "$nom.part")
            partiel.outputStream().use { it.write(octets); it.fd.sync() }
            if (!partiel.renameTo(cible)) throw IOException("renommage de $nom impossible")
            return
        }
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val valeurs = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nom)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/gzip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/$DOSSIER/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, valeurs) ?: throw IOException("MediaStore a refusé $nom")
        try {
            resolver.openOutputStream(uri)!!.use { it.write(octets); it.flush() }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null) // jamais de fichier à moitié écrit
            throw e
        }
    }

    override fun supprimer(nom: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            File(dossierLegacy, nom).delete()
            return
        }
        resolver.delete(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf("Documents/$DOSSIER/", nom),
        )
    }

    private companion object { const val DOSSIER = "Surculus" }
}

/** Photos écrites dès la prise dans `Pictures/Surculus/<code-tournée>/` (MediaStore ; dossier public avant l'API 29). */
class StockagePhotosMediaStore(context: Context) : StockagePhotos {
    private val resolver = context.applicationContext.contentResolver

    override fun ecrire(dossier: String, nom: String, octets: ByteArray): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val racine = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).parentFile
            val d = File(racine, dossier)
            if (!d.isDirectory && !d.mkdirs()) throw IOException("dossier $d non créé")
            val f = File(d, nom)
            if (f.exists()) throw IOException("$nom existe déjà : jamais écrasé")
            f.outputStream().use { it.write(octets); it.fd.sync() }
            return dossier + nom
        }
        val valeurs = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nom)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, dossier)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), valeurs)
            ?: throw IOException("MediaStore a refusé $nom")
        try {
            resolver.openOutputStream(uri)!!.use { it.write(octets); it.flush() }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return dossier + nom
    }
}
