/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), android/app/src/main/kotlin/groma/app/carte/Fonds.kt,
 *           carte/TuilesHorsLigne.kt et TelechargementTuiles.kt, lus en l'état de la copie locale (2026-10-21).
 *           Reprise prévue par l'ADR 0009 (« avec le code de Groma »), avant l'extraction en bibliothèque commune.
 * Modifications : paquet ; ortho IGN seule (pas de plan ni de cadastre) ; zone de Surculus (`core.carte.ZoneTuiles`,
 *           emprise Lambert-93 testée en France) ; agent HTTP propre ; marque de fin et date du téléchargement
 *           (attribution datée, ADR 0009) ; option « Wi-Fi seulement » ; lecture prioritaire d'un `ortho.mbtiles`
 *           ou `ortho.gpkg` en EPSG:3857 du dossier de la tournée.
 */
package io.github.pobsteta.surculus.carte

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.Drawable
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.pobsteta.surculus.core.carte.EtatOrtho
import io.github.pobsteta.surculus.core.carte.Tuile
import io.github.pobsteta.surculus.core.carte.ZoneTuiles
import io.github.pobsteta.surculus.core.geo.EmpriseDegres
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.gpkg.OrthoTuilee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Ortho IGN Géoplateforme (WMTS, sans clé, Licence Ouverte Etalab 2.0). La grille `PM_0_19` est celle d'osmdroid :
 * TILEROW et TILECOL sont ses Y et X. Même couche que l'ortho de QField (`PM_0_19`, EPSG:3857), ADR 0009.
 */
object OrthoIgn : OnlineTileSourceBase("OrthoIGN", 0, 19, 256, ".jpg", arrayOf("https://data.geopf.fr/wmts")) {
    const val ATTRIBUTION = "© IGN Géoplateforme, Licence Ouverte Etalab 2.0"

    override fun getTileURLString(index: Long): String =
        baseUrl + "?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=ORTHOIMAGERY.ORTHOPHOTOS&STYLE=normal&FORMAT=image/jpeg" +
            "&TILEMATRIXSET=PM_0_19&TILEMATRIX=" + MapTileIndex.getZoom(index) +
            "&TILEROW=" + MapTileIndex.getY(index) + "&TILECOL=" + MapTileIndex.getX(index)
}

/** Dernier zoom avec des tuiles ; la carte sur-zoome au-delà. */
const val ZOOM_TUILES = 19
const val ZOOM_MAX = 22.0

/**
 * osmdroid veut un agent HTTP (règles des serveurs de tuiles) et un cache dans le stockage de l'application : son
 * cache externe par défaut est invisible depuis Android 10.
 */
fun configurerOsmdroid(ctx: Context) {
    val config = Configuration.getInstance()
    config.load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    config.userAgentValue = agentHttp(ctx)
    val base = File(ctx.cacheDir, "osmdroid").apply { mkdirs() }
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tuiles").apply { mkdirs() }
}

private fun agentHttp(ctx: Context): String {
    val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "0"
    return "Surculus/$version (+${ctx.packageName})"
}

/** Tuiles préchargées d'une tournée : `tuiles/<source>/<z>/<x>/<y>.jpg`, dans le dossier de la tournée. */
object TuilesHorsLigne {
    fun racine(dossierTournee: File) = File(dossierTournee, "tuiles")

    fun fichier(racine: File, source: BitmapTileSourceBase, t: Tuile): File =
        File(racine, "${source.name()}/${t.z}/${t.x}/${t.y}${source.imageFilenameEnding()}")

    /** Tuiles écrites pour [source] (les `.tmp` d'un téléchargement interrompu ne comptent pas). */
    fun compter(racine: File, source: ITileSource): Long =
        File(racine, source.name()).walkTopDown().count { it.isFile && !it.name.endsWith(".tmp") && it.extension == "jpg" }.toLong()

    /** Posée à la fin d'un préchargement allé au bout ; son contenu est la date du téléchargement (attribution datée). */
    fun marqueFin(racine: File) = File(racine, "ortho-terminee.txt")

    fun dateTelechargement(racine: File): String? = marqueFin(racine).takeIf { it.isFile }?.readText()?.trim()?.ifEmpty { null }
}

/** Ortho d'une tournée, telle que la liste et la carte la montrent. */
data class OrthoTournee(
    val etat: EtatOrtho,
    /** Nom du fichier tuilé du dossier (`ortho.mbtiles`, `ortho.gpkg`) quand il est lu en priorité, sinon `null`. */
    val fichierLocal: String?,
    /** Date du téléchargement des tuiles IGN (aaaa-mm-jj), `null` si inconnue ou si un fichier local est lu. */
    val telechargeeLe: String?,
)

/**
 * `ortho.mbtiles` ou `ortho.gpkg` du dossier de la tournée, **lu en priorité** (ADR 0009). Le prédicat d'exploitabilité
 * est celui de `:gpkg` ([OrthoTuilee]), le même pour l'état, la planification et la carte : un fichier illisible ou
 * tuilé hors EPSG:3857 n'est pas une ortho. SQLite d'Android ne sert qu'à lire les tuiles.
 */
class FichierOrtho private constructor(val fichier: File, private val bd: SQLiteDatabase, private val description: OrthoTuilee.Description) {
    fun tuile(t: Tuile): ByteArray? {
        val ligne = if (description.tms) (1 shl t.z) - 1 - t.y else t.y
        bd.rawQuery("SELECT tile_data FROM \"${description.table}\" WHERE zoom_level=? AND tile_column=? AND tile_row=?", arrayOf("${t.z}", "${t.x}", "$ligne")).use {
            return if (it.moveToFirst()) it.getBlob(0) else null
        }
    }

    fun fermer() = bd.close()

    companion object {
        /** Nom du fichier tuilé exploitable du dossier, `null` sinon (rien n'est ouvert côté Android). */
        fun nomPresent(dossierTournee: File): String? = OrthoTuilee.dans(dossierTournee.toPath())?.nom

        /** `null` si aucun fichier exploitable. */
        fun ouvrir(dossierTournee: File): FichierOrtho? {
            val d = OrthoTuilee.dans(dossierTournee.toPath()) ?: return null
            val f = File(dossierTournee, d.nom)
            val bd = runCatching { SQLiteDatabase.openDatabase(f.path, null, SQLiteDatabase.OPEN_READONLY) }.getOrNull() ?: return null
            return FichierOrtho(f, bd, d)
        }
    }
}

/** Lit les tuiles d'un fichier `ortho.mbtiles` ou `ortho.gpkg` avant tout le reste. */
private class ModuleFichier(private val ortho: FichierOrtho, private var source: ITileSource) : MapTileModuleProviderBase(2, 40) {
    override fun getUsesDataConnection() = false
    override fun getMinimumZoomLevel() = 0
    override fun getMaximumZoomLevel() = ZOOM_TUILES
    override fun setTileSource(s: ITileSource) { source = s }
    override fun getName() = "Ortho de la tournée (fichier)"
    override fun getThreadGroupName() = "ortho_fichier"

    override fun getTileLoader(): TileLoader = object : TileLoader() {
        override fun loadTile(index: Long): Drawable? {
            val octets = ortho.tuile(Tuile(MapTileIndex.getZoom(index), MapTileIndex.getX(index), MapTileIndex.getY(index))) ?: return null
            return source.getDrawable(ByteArrayInputStream(octets))
        }
    }
}

/** Lit les tuiles préchargées dans le dossier de la tournée, avant le cache d'osmdroid et le réseau. */
private class ModuleDossier(private val racine: File, private var source: ITileSource) : MapTileModuleProviderBase(2, 40) {
    override fun getUsesDataConnection() = false
    override fun getMinimumZoomLevel() = 0
    override fun getMaximumZoomLevel() = ZOOM_TUILES
    override fun setTileSource(s: ITileSource) { source = s }
    override fun getName() = "Tuiles de la tournée"
    override fun getThreadGroupName() = "tuiles_tournee"

    override fun getTileLoader(): TileLoader = object : TileLoader() {
        override fun loadTile(index: Long): Drawable? {
            val s = source as? BitmapTileSourceBase ?: return null
            val f = TuilesHorsLigne.fichier(racine, s, Tuile(MapTileIndex.getZoom(index), MapTileIndex.getX(index), MapTileIndex.getY(index)))
            return if (f.isFile) s.getDrawable(f.path) else null
        }
    }
}

/**
 * Fournisseur de la carte : fichier tuilé de la tournée d'abord, puis tuiles préchargées. La carte ne sort jamais sur le
 * réseau (`setUseDataConnection(false)` côté carte) : le réseau ne sert qu'au préchargement, avec son option « Wi-Fi seulement ».
 */
class FournisseurTuiles(ctx: Context, source: ITileSource, dossierTournee: File, val fichier: FichierOrtho?) : MapTileProviderBasic(ctx, source) {
    init {
        mTileProviderList.add(0, ModuleDossier(TuilesHorsLigne.racine(dossierTournee), source))
        if (fichier != null) mTileProviderList.add(0, ModuleFichier(fichier, source))
    }

    override fun detach() {
        fichier?.fermer()
        super.detach()
    }
}

/** Calcul de l'état de l'ortho d'une tournée (disque seulement : aucune tâche de fond n'est interrogée). */
object OrthoDeTournee {
    fun zone(emprise: EmpriseLambert93?): ZoneTuiles? =
        emprise?.let { (ZoneTuiles.pourTournee(it) as? ZoneTuiles.Resultat.Zone)?.zone }

    /** À appeler hors du fil principal : parcourt le dossier des tuiles. */
    fun etat(dossierTournee: File, emprise: EmpriseLambert93?): OrthoTournee {
        FichierOrtho.nomPresent(dossierTournee)?.let { return OrthoTournee(EtatOrtho.Complete, it, null) }
        val racine = TuilesHorsLigne.racine(dossierTournee)
        val attendues = zone(emprise)?.nombre ?: 0L
        val termine = TuilesHorsLigne.marqueFin(racine).isFile
        val etat = EtatOrtho.depuis(TuilesHorsLigne.compter(racine, OrthoIgn), attendues, termine)
        return OrthoTournee(etat, null, TuilesHorsLigne.dateTelechargement(racine))
    }
}

/** Option « Wi-Fi seulement » du préchargement (ADR 0009) : activée par défaut, pour ne pas consommer le forfait mobile. */
object ReglagesOrtho {
    private const val PREFS = "ortho"
    private const val CLE_WIFI = "wifi_seulement"

    fun wifiSeulement(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CLE_WIFI, true)

    fun definirWifiSeulement(ctx: Context, valeur: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CLE_WIFI, valeur).apply()
    }
}

/**
 * Préchargement de l'ortho IGN d'une tournée pour travailler hors ligne (ADR 0009). Les tuiles déjà sur disque sont
 * sautées : une reprise ou un second passage repart où le premier s'est arrêté. Une requête à la fois, avec l'agent de
 * l'application (usage raisonnable de la Géoplateforme). Trois garde-fous : état visible par tournée, emprise testée en
 * France métropolitaine (avant de planifier : [planifier]), attribution datée (marque de fin).
 */
class TelechargementTuiles(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val racine = TuilesHorsLigne.racine(File(inputData.getString(CLE_DOSSIER)!!))
        val e = inputData.getDoubleArray(CLE_EMPRISE)!!
        val zone = ZoneTuiles(EmpriseDegres(latitudeMinDeg = e[0], longitudeMinDeg = e[1], latitudeMaxDeg = e[2], longitudeMaxDeg = e[3]))
        val total = zone.nombre
        var faites = 0L
        var echecs = 0
        val agent = agentHttp(applicationContext)
        for (t in zone.tuiles()) {
            if (isStopped) return@withContext Result.failure()
            val f = TuilesHorsLigne.fichier(racine, OrthoIgn, t)
            if (!f.isFile) {
                try {
                    telecharger(OrthoIgn.getTileURLString(MapTileIndex.getTileIndex(t.z, t.x, t.y)), f, agent)
                } catch (ex: TuileAbsente) {
                    // Hors de la couverture de la couche (404, tuile vide) : rien à garder.
                } catch (ex: IOException) {
                    // Réseau perdu : WorkManager réessaie plus tard et reprend.
                    if (++echecs >= ECHECS_MAX) return@withContext Result.retry()
                    delay(1000)
                    continue
                }
            }
            faites++
            if (faites % 20 == 0L || faites == total) setProgress(workDataOf(CLE_FAITES to faites, CLE_TOTAL to total))
        }
        // Une tuile en échec (réseau, serveur) : pas de marque de fin, l'ortho reste « partielle » et la tâche reprend.
        if (!EtatOrtho.marqueAEcrire(echecs)) return@withContext Result.retry()
        TuilesHorsLigne.marqueFin(racine).writeText(LocalDate.now().toString())
        Result.success(workDataOf(CLE_FAITES to faites, CLE_TOTAL to total))
    }

    private class TuileAbsente : Exception()

    private fun telecharger(url: String, f: File, agent: String) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", agent)
            when (c.responseCode) {
                200 -> {}
                204, 404 -> throw TuileAbsente()
                else -> throw IOException("HTTP ${c.responseCode}")
            }
            f.parentFile?.mkdirs()
            val tmp = File(f.path + ".tmp")
            c.inputStream.use { entree -> tmp.outputStream().use { entree.copyTo(it) } }
            if (!tmp.renameTo(f)) throw IOException("Écriture impossible : $f")
        } finally {
            c.disconnect()
        }
    }

    companion object {
        const val CLE_DOSSIER = "dossier"
        const val CLE_EMPRISE = "emprise"
        const val CLE_FAITES = "faites"
        const val CLE_TOTAL = "total"
        private const val ECHECS_MAX = 20

        fun nomTravail(dossierTournee: File) = "tuiles_${dossierTournee.name}"

        /**
         * Planifie le préchargement de la zone de la tournée. Rien si la tournée porte son propre fichier tuilé (lu en
         * priorité), ou si [emprise] est refusée (hors de France, trop grande : l'état reste « absente »).
         * Renvoie la raison du refus, `null` si planifié ou inutile. [remplacer] : relance même si une tâche existe.
         */
        fun planifier(ctx: Context, dossierTournee: File, emprise: EmpriseLambert93?, remplacer: Boolean): ZoneTuiles.Resultat? {
            if (FichierOrtho.nomPresent(dossierTournee) != null || emprise == null) return null
            val resultat = ZoneTuiles.pourTournee(emprise)
            val zone = (resultat as? ZoneTuiles.Resultat.Zone)?.zone ?: return resultat
            if (TuilesHorsLigne.marqueFin(TuilesHorsLigne.racine(dossierTournee)).isFile && !remplacer) return null
            val z = zone.emprise
            val req = OneTimeWorkRequestBuilder<TelechargementTuiles>()
                .setInputData(
                    workDataOf(
                        CLE_DOSSIER to dossierTournee.path,
                        CLE_EMPRISE to doubleArrayOf(z.latitudeMinDeg, z.longitudeMinDeg, z.latitudeMaxDeg, z.longitudeMaxDeg),
                    ),
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(if (ReglagesOrtho.wifiSeulement(ctx)) NetworkType.UNMETERED else NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                nomTravail(dossierTournee),
                if (remplacer) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                req,
            )
            return null
        }
    }
}
