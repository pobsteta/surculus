package io.github.pobsteta.surculus.core.geo

/**
 * Point en Lambert-93 (RGF93 v1 / Lambert-93, **EPSG:2154**), en **mètres**.
 *
 * Ordre explicite : [xMetres] est l'abscisse (vers l'est, « E »), [yMetres] l'ordonnée
 * (vers le nord, « N »). C'est l'ordre des axes déclaré par EPSG:2154 et celui du
 * `travaux.gpkg` de sommieR. Aucune valeur en degrés ne doit transiter par ce type.
 */
data class PointLambert93(val xMetres: Double, val yMetres: Double) {
    init {
        require(xMetres.isFinite() && yMetres.isFinite()) {
            "Coordonnées Lambert-93 non finies : x = $xMetres, y = $yMetres"
        }
    }
}

/**
 * Emprise rectangulaire en Lambert-93 (EPSG:2154), bornes en mètres, incluses.
 */
data class EmpriseLambert93(
    val xMinMetres: Double,
    val yMinMetres: Double,
    val xMaxMetres: Double,
    val yMaxMetres: Double,
) {
    init {
        require(xMinMetres < xMaxMetres && yMinMetres < yMaxMetres) { "Emprise vide ou inversée" }
    }

    fun contient(p: PointLambert93): Boolean =
        p.xMetres in xMinMetres..xMaxMetres && p.yMetres in yMinMetres..yMaxMetres

    companion object {
        /**
         * Domaine d'usage d'EPSG:2154 (France métropolitaine, Corse comprise, terre et mer).
         *
         * Source : registre EPSG via `projinfo EPSG:2154` (PROJ 9.4.0),
         * `BBOX[41.15,-9.86,51.56,10.38]` (lat min, lon min, lat max, lon max, WGS84),
         * projeté en Lambert-93 avec pyproj 3.6.1 :
         * `Transformer.from_crs(4326, 2154, always_xy=True).transform_bounds(-9.86, 41.15, 10.38, 51.56, densify_pts=50)`.
         *
         * C'est un rectangle : il déborde sur la mer et les pays voisins. Il attrape l'inversion
         * des axes et l'oubli de reprojection, pas un point en Suisse (arbitrage de Pascal,
         * 2026-10-16 ; le contour IGN est une question ouverte de la spec).
         */
        val FRANCE_METROPOLITAINE = EmpriseLambert93(
            xMinMetres = -378_305.81,
            yMinMetres = 6_005_294.71,
            xMaxMetres = 1_320_649.57,
            yMaxMetres = 7_235_612.72,
        )
    }
}
