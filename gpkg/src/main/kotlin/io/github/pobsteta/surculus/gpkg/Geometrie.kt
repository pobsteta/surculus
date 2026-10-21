/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gpkg

import io.github.pobsteta.surculus.core.geo.PointLambert93
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Géométries écrites dans un `travaux.gpkg` : points, polygones, multipolygones, en Lambert-93
 * (x = Est, y = Nord, mètres : jamais de degrés, [PointLambert93]).
 */
sealed interface Geometrie {
    data class Point(val position: PointLambert93) : Geometrie

    /** Premier anneau : contour extérieur ; les suivants : trous. Chaque anneau est fermé (premier = dernier point). */
    data class Polygone(val anneaux: List<List<PointLambert93>>) : Geometrie {
        init {
            require(anneaux.isNotEmpty()) { "Polygone sans anneau" }
            for (a in anneaux) {
                require(a.size >= 4) { "Anneau de ${a.size} points : 4 au moins (fermé)" }
                require(a.first() == a.last()) { "Anneau non fermé" }
            }
        }
    }

    data class MultiPolygone(val polygones: List<Polygone>) : Geometrie {
        init { require(polygones.isNotEmpty()) { "Multipolygone vide" } }
    }

    /** Tous les points, dans l'ordre. */
    fun points(): List<PointLambert93> = when (this) {
        is Point -> listOf(position)
        is Polygone -> anneaux.flatten()
        is MultiPolygone -> polygones.flatMap { it.points() }
    }
}

/** Enveloppe en mètres (OGC 12-128 § 2.1.3 : ordre minx, maxx, miny, maxy dans l'en-tête). */
data class Enveloppe(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double)

/** Contenu décodé d'un blob `StandardGeoPackageBinary`. */
data class GeometrieLue(
    val srsId: Int,
    val vide: Boolean,
    /** `null` si la géométrie est vide. */
    val enveloppe: Enveloppe?,
)

/**
 * Codage et décodage de la géométrie GeoPackage (`StandardGeoPackageBinary`, OGC 12-128 § 2.1.3, Req. 19-21) :
 * en-tête `GP`, version 0, indicateurs, `srs_id`, enveloppe facultative, puis WKB (ISO 13249, little endian).
 * Les points sont écrits sans enveloppe, les polygones avec l'enveloppe [xmin, xmax, ymin, ymax], comme GDAL.
 */
object GeometrieGpkg {
    private const val INDICATEUR_LITTLE_ENDIAN = 0x01
    private const val INDICATEUR_VIDE = 0x10
    private const val WKB_POINT = 1
    private const val WKB_LINESTRING = 2
    private const val WKB_POLYGON = 3
    private const val WKB_MULTIPOINT = 4
    private const val WKB_MULTILINESTRING = 5
    private const val WKB_MULTIPOLYGON = 6
    private const val WKB_COLLECTION = 7

    fun encoder(g: Geometrie, srsId: Int): ByteArray {
        val corps = ByteBuffer.allocate(taille(g)).order(ByteOrder.LITTLE_ENDIAN)
        ecrireWkb(corps, g)
        val avecEnveloppe = g !is Geometrie.Point
        val enteteTaille = 8 + if (avecEnveloppe) 32 else 0
        val b = ByteBuffer.allocate(enteteTaille + corps.capacity()).order(ByteOrder.LITTLE_ENDIAN)
        b.put('G'.code.toByte()).put('P'.code.toByte()).put(0)
        b.put((INDICATEUR_LITTLE_ENDIAN or (if (avecEnveloppe) (1 shl 1) else 0)).toByte())
        b.putInt(srsId)
        if (avecEnveloppe) {
            val pts = g.points()
            b.putDouble(pts.minOf { it.xMetres }).putDouble(pts.maxOf { it.xMetres })
            b.putDouble(pts.minOf { it.yMetres }).putDouble(pts.maxOf { it.yMetres })
        }
        b.put(corps.array())
        return b.array()
    }

    private fun taille(g: Geometrie): Int = when (g) {
        is Geometrie.Point -> 5 + 16
        is Geometrie.Polygone -> 5 + 4 + g.anneaux.sumOf { 4 + 16 * it.size }
        is Geometrie.MultiPolygone -> 5 + 4 + g.polygones.sumOf { taille(it) }
    }

    private fun ecrireWkb(b: ByteBuffer, g: Geometrie) {
        b.put(1) // little endian
        when (g) {
            is Geometrie.Point -> {
                b.putInt(WKB_POINT)
                b.putDouble(g.position.xMetres).putDouble(g.position.yMetres)
            }
            is Geometrie.Polygone -> {
                b.putInt(WKB_POLYGON).putInt(g.anneaux.size)
                for (a in g.anneaux) {
                    b.putInt(a.size)
                    for (p in a) b.putDouble(p.xMetres).putDouble(p.yMetres)
                }
            }
            is Geometrie.MultiPolygone -> {
                b.putInt(WKB_MULTIPOLYGON).putInt(g.polygones.size)
                for (p in g.polygones) ecrireWkb(b, p)
            }
        }
    }

    /**
     * Décode l'en-tête et l'enveloppe. Sans enveloppe dans l'en-tête, elle est calculée sur le WKB
     * (tous types OGC, 2D, Z, M ou ZM). Lève [IllegalArgumentException] si le blob n'est pas du GeoPackage binaire.
     */
    fun decoder(blob: ByteArray): GeometrieLue {
        require(blob.size >= 8 && blob[0] == 'G'.code.toByte() && blob[1] == 'P'.code.toByte()) { "Blob sans en-tête GeoPackage « GP »" }
        require(blob[2].toInt() == 0) { "Version de géométrie GeoPackage ${blob[2]} non prise en charge" }
        val indicateurs = blob[3].toInt() and 0xFF
        require(indicateurs and 0x20 == 0) { "Géométrie GeoPackage étendue (ExtendedGeoPackageBinary) non prise en charge" }
        val ordre = if (indicateurs and INDICATEUR_LITTLE_ENDIAN != 0) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
        val b = ByteBuffer.wrap(blob).order(ordre)
        val srsId = b.getInt(4)
        val codeEnveloppe = (indicateurs shr 1) and 0x07
        val tailleEnveloppe = when (codeEnveloppe) {
            0 -> 0
            1 -> 32
            2, 3 -> 48
            4 -> 64
            else -> throw IllegalArgumentException("Indicateur d'enveloppe $codeEnveloppe invalide")
        }
        if (indicateurs and INDICATEUR_VIDE != 0) return GeometrieLue(srsId, true, null)
        if (tailleEnveloppe >= 32) {
            return GeometrieLue(srsId, false, Enveloppe(b.getDouble(8), b.getDouble(16), b.getDouble(24), b.getDouble(32)))
        }
        val env = enveloppeWkb(b, 8 + tailleEnveloppe)
        return GeometrieLue(srsId, env == null, env)
    }

    /**
     * Le point d'un blob de type POINT (Z ou M ignorés) ; `null` si la géométrie est vide ou n'est pas un point.
     * x = Est, y = Nord (Lambert-93) : l'appelant garantit le repère par `srs_id`.
     */
    fun point(blob: ByteArray): PointLambert93? {
        val lue = decoder(blob)
        if (lue.vide) return null
        val code = ((blob[3].toInt() and 0xFF) shr 1) and 0x07
        val debut = 8 + when (code) {
            0 -> 0
            1 -> 32
            2, 3 -> 48
            else -> 64
        }
        val b = ByteBuffer.wrap(blob)
        b.order(if (b.get(debut).toInt() == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        if (b.getInt(debut + 1) % 1000 != WKB_POINT) return null
        val x = b.getDouble(debut + 5)
        val y = b.getDouble(debut + 13)
        return if (x.isFinite() && y.isFinite()) PointLambert93(x, y) else null
    }

    /**
     * Décode la géométrie surfacique complète d'un blob : [Geometrie.Polygone] (trous gardés) ou [Geometrie.MultiPolygone]
     * (toutes les parties). x = Est, y = Nord (Lambert-93, mètres) : l'appelant garantit le repère par `srs_id`.
     * Rend `null` si la géométrie est vide. Lève [IllegalArgumentException] si le blob est corrompu, tronqué,
     * ou d'un type non polygonal (point, ligne, collection).
     */
    fun lire(blob: ByteArray): Geometrie? {
        val lue = decoder(blob)
        if (lue.vide) return null
        val code = ((blob[3].toInt() and 0xFF) shr 1) and 0x07
        val debut = 8 + when (code) {
            0 -> 0
            1 -> 32
            2, 3 -> 48
            else -> 64
        }
        val b = ByteBuffer.wrap(blob)
        try {
            return lireSurface(b, debut).first
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Géométrie tronquée")
        }
    }

    private fun lireSurface(b: ByteBuffer, debut: Int): Pair<Geometrie, Int> {
        b.order(if (b.get(debut).toInt() == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        val codeWkb = b.getInt(debut + 1)
        val dimensions = when (codeWkb / 1000) {
            0 -> 2
            1, 2 -> 3
            3 -> 4
            else -> throw IllegalArgumentException("Type WKB $codeWkb inconnu")
        }
        var pos = debut + 5
        when (codeWkb % 1000) {
            WKB_POLYGON -> {
                val nbAnneaux = b.getInt(pos)
                pos += 4
                require(nbAnneaux in 1..1_000_000) { "Nombre d'anneaux invalide" }
                val anneaux = ArrayList<List<PointLambert93>>()
                repeat(nbAnneaux) {
                    val n = b.getInt(pos)
                    pos += 4
                    require(n in 0..100_000_000 && pos + 8L * dimensions * n <= b.limit()) { "Anneau tronqué" }
                    val pts = ArrayList<PointLambert93>(n)
                    repeat(n) {
                        pts.add(PointLambert93(b.getDouble(pos), b.getDouble(pos + 8)))
                        pos += 8 * dimensions
                    }
                    anneaux.add(pts)
                }
                return Geometrie.Polygone(anneaux) to pos
            }
            WKB_MULTIPOLYGON -> {
                val n = b.getInt(pos)
                pos += 4
                require(n in 1..1_000_000) { "Nombre de parties invalide" }
                val parties = ArrayList<Geometrie.Polygone>()
                repeat(n) {
                    val (g, fin) = lireSurface(b, pos)
                    parties.add(g as? Geometrie.Polygone ?: throw IllegalArgumentException("Partie non polygonal"))
                    pos = fin
                }
                return Geometrie.MultiPolygone(parties) to pos
            }
            else -> throw IllegalArgumentException("Type WKB $codeWkb non polygonal")
        }
    }

    private class Cumul {
        var xMin = Double.POSITIVE_INFINITY
        var xMax = Double.NEGATIVE_INFINITY
        var yMin = Double.POSITIVE_INFINITY
        var yMax = Double.NEGATIVE_INFINITY
        var n = 0

        fun ajouter(x: Double, y: Double) {
            if (x.isNaN() || y.isNaN()) return
            xMin = minOf(xMin, x)
            xMax = maxOf(xMax, x)
            yMin = minOf(yMin, y)
            yMax = maxOf(yMax, y)
            n++
        }
    }

    private fun enveloppeWkb(b: ByteBuffer, debut: Int): Enveloppe? {
        val c = Cumul()
        lireWkb(b, debut, c)
        return if (c.n == 0) null else Enveloppe(c.xMin, c.xMax, c.yMin, c.yMax)
    }

    /** Lit une géométrie WKB à partir de [debut] ; rend la position après elle. */
    private fun lireWkb(b: ByteBuffer, debut: Int, c: Cumul): Int {
        val ordreInitial = b.order()
        try {
            var pos = debut
            b.order(if (b.get(pos).toInt() == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
            pos += 1
            val code = b.getInt(pos)
            pos += 4
            val type = code % 1000
            val dimensions = when (code / 1000) {
                0 -> 2
                1, 2 -> 3
                3 -> 4
                else -> throw IllegalArgumentException("Type WKB $code inconnu")
            }

            fun point(p: Int): Int {
                c.ajouter(b.getDouble(p), b.getDouble(p + 8))
                return p + 8 * dimensions
            }

            fun suite(p0: Int): Int {
                var p = p0
                val n = b.getInt(p)
                p += 4
                repeat(n) { p = point(p) }
                return p
            }
            when (type) {
                WKB_POINT -> pos = point(pos)
                WKB_LINESTRING -> pos = suite(pos)
                WKB_POLYGON -> {
                    val n = b.getInt(pos)
                    pos += 4
                    repeat(n) { pos = suite(pos) }
                }
                WKB_MULTIPOINT, WKB_MULTILINESTRING, WKB_MULTIPOLYGON, WKB_COLLECTION -> {
                    val n = b.getInt(pos)
                    pos += 4
                    repeat(n) { pos = lireWkb(b, pos, c) }
                }
                else -> throw IllegalArgumentException("Type WKB $code non pris en charge")
            }
            return pos
        } finally {
            b.order(ordreInitial)
        }
    }
}
