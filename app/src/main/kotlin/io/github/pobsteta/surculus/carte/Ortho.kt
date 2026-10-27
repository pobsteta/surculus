/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), carte/Fonds.kt (lu le 2026-10-21) pour la configuration
 *           d'osmdroid ; dépôt Marculus, `ui/carte/CarteScreen.kt` (`SourceGeoplateforme`, l. 195-200) et
 *           `GpkgTileModule.kt` (lus le 2026-10-27) pour les fonds en ligne et l'ortho du chantier.
 * Modifications : mission 055 : plus de préchargement des tuiles IGN (ni WorkManager, ni « Wi-Fi seulement », ni état de
 *           l'ortho par tournée) ; les fonds IGN et OSM se chargent en ligne, au fur et à mesure (cache d'osmdroid) ;
 *           l'ortho du chantier est le fichier `ortho.mbtiles` ou `ortho.gpkg` en EPSG:3857 du dossier de la tournée,
 *           lu hors ligne (ADR 0009, lecture prioritaire de l'option (g)).
 */
package io.github.pobsteta.surculus.carte

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.Drawable
import io.github.pobsteta.surculus.core.carte.CoucheWmts
import io.github.pobsteta.surculus.core.carte.Fond
import io.github.pobsteta.surculus.core.carte.Geoplateforme
import io.github.pobsteta.surculus.core.carte.Tuile
import io.github.pobsteta.surculus.gpkg.OrthoTuilee
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.MapTileApproximater
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.overlay.TilesOverlay
import java.io.ByteArrayInputStream
import java.io.File

/** Dernier zoom avec des tuiles ; la carte sur-zoome au-delà. */
const val ZOOM_TUILES = 19

/** Niveaux de surzoom au-delà des tuiles natives : les dernières tuiles sont agrandies (comme Marculus). */
const val SURZOOM = 5
const val ZOOM_MAX_OSM = 19.0

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

/**
 * `ortho.mbtiles` ou `ortho.gpkg` du dossier de la tournée : l'« Ortho chantier » du menu des fonds (ADR 0009). Le
 * prédicat d'exploitabilité est celui de `:gpkg` ([OrthoTuilee]) : un fichier illisible ou tuilé hors EPSG:3857 n'est
 * pas une ortho. SQLite d'Android ne sert qu'à lire les tuiles.
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

/** Lit les tuiles d'un fichier `ortho.mbtiles` ou `ortho.gpkg`, hors ligne (comme le `GpkgTileModule` de Marculus). */
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

/** Source osmdroid d'une couche WMTS de la Géoplateforme (URL construites par `core.carte.CoucheWmts`). */
private class SourceGeoplateforme(private val couche: CoucheWmts) :
    OnlineTileSourceBase(couche.nom, couche.zoomMin, couche.zoomMax, 256, couche.extension, arrayOf(CoucheWmts.BASE)) {
    override fun getTileURLString(index: Long): String =
        couche.url(MapTileIndex.getZoom(index), MapTileIndex.getX(index), MapTileIndex.getY(index))
}

private val SOURCE_PLAN_IGN: OnlineTileSourceBase = SourceGeoplateforme(Geoplateforme.PLAN_IGN)
private val SOURCE_ORTHO_IGN: OnlineTileSourceBase = SourceGeoplateforme(Geoplateforme.ORTHO_IGN)
private val SOURCE_CADASTRE: OnlineTileSourceBase = SourceGeoplateforme(Geoplateforme.CADASTRE)

/** Cadastre IGN : couche de tuiles transparente, posée sous les surcouches de la tournée (Marculus, `CarteScreen.kt` l. 271-277). */
fun coucheCadastre(ctx: Context): TilesOverlay =
    TilesOverlay(MapTileProviderBasic(ctx.applicationContext, SOURCE_CADASTRE), ctx.applicationContext).apply {
        loadingBackgroundColor = android.graphics.Color.TRANSPARENT
        loadingLineColor = android.graphics.Color.TRANSPARENT
    }

/**
 * Fournisseur de tuiles d'un fond, comme Marculus (`CarteScreen.kt` l. 407-421) : un fournisseur NEUF à chaque changement.
 * Plan IGN, Ortho IGN et OpenStreetMap se chargent en ligne, au fur et à mesure, avec le cache d'osmdroid. L'ortho du
 * chantier ne lit que le fichier de la tournée, hors ligne, et agrandit ses dernières tuiles au-delà de [ZOOM_TUILES].
 * Sans fichier exploitable, le fond « Ortho chantier » retombe sur le Plan IGN.
 */
fun fournisseurDeFond(ctx: Context, fond: Fond, fichier: FichierOrtho?): MapTileProviderBase = when (fond) {
    Fond.OSM -> MapTileProviderBasic(ctx.applicationContext, TileSourceFactory.MAPNIK)
    Fond.PLAN_IGN -> MapTileProviderBasic(ctx.applicationContext, SOURCE_PLAN_IGN)
    Fond.IGN -> MapTileProviderBasic(ctx.applicationContext, SOURCE_ORTHO_IGN)
    Fond.ORTHO ->
        if (fichier == null) {
            MapTileProviderBasic(ctx.applicationContext, SOURCE_PLAN_IGN)
        } else {
            val source = XYTileSource("ortho", 0, ZOOM_TUILES, 256, ".png", emptyArray())
            val module = ModuleFichier(fichier, source)
            val approximateur = MapTileApproximater().apply { addProvider(module) }
            object : MapTileProviderArray(source, null, arrayOf(module, approximateur)) {
                override fun detach() {
                    fichier.fermer()
                    super.detach()
                }
            }
        }
}

/** Zoom maximal de la carte pour [fond] : OSM à ses tuiles natives, les autres avec [SURZOOM] niveaux d'agrandissement. */
fun zoomMaxDeFond(fond: Fond): Double = if (fond == Fond.OSM) ZOOM_MAX_OSM else (ZOOM_TUILES + SURZOOM).toDouble()
