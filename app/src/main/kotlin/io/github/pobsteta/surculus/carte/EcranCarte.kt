/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import android.graphics.Color as CouleurAndroid
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.core.carte.EtatOrtho
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.guidage.Guidage
import io.github.pobsteta.surculus.core.guidage.Libelles
import io.github.pobsteta.surculus.gpkg.Geometrie
import io.github.pobsteta.surculus.gpkg.ObjetSurfacique
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
import io.github.pobsteta.surculus.tournee.TourneeOuverte
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

/**
 * Carte de la tournée (spec § 4.2) : forêt, UG, parcelles, placettes, ortho hors ligne, position et badge du fix.
 * Une question : où suis-je par rapport aux placettes ? Un appui sur une placette (ou sur la liste) ouvre le guidage.
 * Pas de couleur de placette selon `etat_suivi` ni de travaux ni de vides à ce stade : voir le rendu de la mission.
 */
@Composable
fun EcranCarte(id: String, onRetour: () -> Unit, onGuider: (String) -> Unit) {
    when (val c = chargerTournee(id)) {
        Chargement.EnCours -> Column(Modifier.padding(24.dp)) { Text(stringResource(R.string.carte_ouverture), fontSize = 20.sp) }
        is Chargement.Refus -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.carte_refus, c.message), fontSize = 18.sp, color = Palette.Rouge)
            OutlinedButton(onClick = onRetour, modifier = Modifier.height(56.dp)) { Text(stringResource(R.string.tournees_retour), fontSize = 18.sp) }
        }
        is Chargement.Pret -> CartePrete(c.tournee, onRetour, onGuider)
    }
}

private fun GeoPoint(p: io.github.pobsteta.surculus.core.geo.PointLambert93): GeoPoint {
    val (lon, lat) = Projections.versWgs84(p.xMetres, p.yMetres)
    return GeoPoint(lat, lon)
}

private fun disque(couleur: Int, diametrePx: Int) = ShapeDrawable(OvalShape()).apply {
    paint.color = couleur
    intrinsicWidth = diametrePx
    intrinsicHeight = diametrePx
}

private fun polygones(carte: MapView, objets: List<ObjetSurfacique>, contour: Int, remplissage: Int, epaisseur: Float): List<Overlay> =
    objets.flatMap { o ->
        val surfaces = when (val g = o.geometrie) {
            is Geometrie.Polygone -> listOf(g)
            is Geometrie.MultiPolygone -> g.polygones
            is Geometrie.Point -> emptyList()
        }
        surfaces.map { s ->
            Polygon(carte).apply {
                points = s.anneaux.first().map { GeoPoint(it) }
                holes = s.anneaux.drop(1).map { anneau -> anneau.map { GeoPoint(it) } }
                outlinePaint.color = contour
                outlinePaint.strokeWidth = epaisseur
                fillPaint.color = remplissage
                title = null
                setOnClickListener { _, _, _ -> true } // pas de bulle d'information sur une surface
            }
        }
    }

private class Couches(val foret: List<Overlay>, val ug: List<Overlay>, val parcelles: List<Overlay>, val placettes: List<Marker>, val position: Marker)

@Composable
private fun CartePrete(ouverte: TourneeOuverte, onRetour: () -> Unit, onGuider: (String) -> Unit) {
    val ctx = LocalContext.current
    val (fix, refusee) = rememberFix()
    val carto = ouverte.cartographie
    val placettes = remember(ouverte) { ouverte.tournee.placettes.filter { it.position != null && it.code != null } }

    var voirForet by remember { mutableStateOf(true) }
    var voirUg by remember { mutableStateOf(true) }
    var voirParcelles by remember { mutableStateOf(false) }
    var voirPlacettes by remember { mutableStateOf(true) }
    var choisie by remember { mutableStateOf<PlacetteTournee?>(null) }
    var liste by remember { mutableStateOf(false) }
    var centrerSurMoi by remember { mutableStateOf(0) }

    val ortho = remember(ouverte) { io.github.pobsteta.surculus.carte.OrthoDeTournee.etat(ouverte.dossier, ouverte.tournee.empriseForet) }
    val distances: Map<String, Double> = remember(fix, placettes) {
        val pos = fix?.position?.let { Guidage.versLambert93(it) }
        if (pos == null) emptyMap() else placettes.associate { p -> p.uuid to kotlin.math.hypot(p.position!!.xMetres - pos.xMetres, p.position!!.yMetres - pos.yMetres) }
    }

    Column(Modifier.fillMaxSize()) {
        // Une ligne : retour à gauche, badge du fix à droite.
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onRetour, modifier = Modifier.height(56.dp)) { Text(stringResource(R.string.tournees_retour), fontSize = 18.sp) }
            BadgeFix(fix)
        }
        if (fix != null && Guidage.versLambert93(fix.position) == null) {
            Text(stringResource(R.string.carte_hors_france), color = Palette.Rouge, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        }
        if (refusee) Text(stringResource(R.string.carte_permission), color = Palette.Rouge, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (liste) {
                ListePlacettes(placettes, distances, onChoisir = { onGuider(it.uuid) }, onFermer = { liste = false })
            } else {
                val couchesRef = remember(ouverte) { arrayOfNulls<Couches>(1) }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { contexte ->
                        configurerOsmdroid(contexte)
                        val fichier = FichierOrtho.ouvrir(ouverte.dossier)
                        val carte = MapView(contexte, FournisseurTuiles(contexte, OrthoIgn, ouverte.dossier, fichier))
                        carte.setMultiTouchControls(true)
                        carte.setUseDataConnection(false)
                        carte.maxZoomLevel = ZOOM_MAX
                        carte.isTilesScaledToDpi = true
                        val densite = contexte.resources.displayMetrics.density
                        val l = Couches(
                            foret = polygones(carte, carto.foret, CouleurAndroid.rgb(11, 107, 46), CouleurAndroid.argb(25, 11, 107, 46), 4f * densite / 2),
                            ug = polygones(carte, carto.ug, CouleurAndroid.rgb(180, 83, 0), CouleurAndroid.argb(40, 180, 83, 0), 3f * densite / 2),
                            parcelles = polygones(carte, carto.parcelles, CouleurAndroid.rgb(255, 255, 255), CouleurAndroid.argb(0, 0, 0, 0), 2f * densite / 2),
                            placettes = placettes.map { p ->
                                Marker(carte).apply {
                                    position = GeoPoint(p.position!!)
                                    title = p.code
                                    relatedObject = p
                                    icon = disque(CouleurAndroid.rgb(176, 0, 32), (28 * densite).toInt())
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    setInfoWindow(null)
                                }
                            },
                            position = Marker(carte).apply {
                                icon = disque(CouleurAndroid.rgb(0, 0, 0), (22 * densite).toInt())
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                setInfoWindow(null)
                                isEnabled = false
                            },
                        )
                        (l.foret + l.ug + l.parcelles + l.placettes + l.position).forEach { carte.overlays.add(it) }
                        l.placettes.forEach { m -> m.setOnMarkerClickListener { marqueur, _ -> choisie = marqueur.relatedObject as? PlacetteTournee; true } }
                        couchesRef[0] = l
                        val emprise: EmpriseLambert93? = carto.empriseUg ?: ouverte.tournee.empriseForet
                        if (emprise != null) {
                            val nordEst = GeoPoint(io.github.pobsteta.surculus.core.geo.PointLambert93(emprise.xMaxMetres, emprise.yMaxMetres))
                            val sudOuest = GeoPoint(io.github.pobsteta.surculus.core.geo.PointLambert93(emprise.xMinMetres, emprise.yMinMetres))
                            carte.post { carte.zoomToBoundingBox(BoundingBox(nordEst.latitude, nordEst.longitude, sudOuest.latitude, sudOuest.longitude), false, (48 * densite).toInt()) }
                        }
                        carte
                    },
                    update = { carte ->
                        val l = couchesRef[0] ?: return@AndroidView
                        l.foret.forEach { it.isEnabled = voirForet }
                        l.ug.forEach { it.isEnabled = voirUg }
                        l.parcelles.forEach { it.isEnabled = voirParcelles }
                        l.placettes.forEach { it.isEnabled = voirPlacettes }
                        l.position.isEnabled = fix != null
                        fix?.let { l.position.position = GeoPoint(it.position.latitude, it.position.longitude) }
                        if (centrerSurMoi > 0 && fix != null) {
                            carte.controller.animateTo(GeoPoint(fix.position.latitude, fix.position.longitude))
                            centrerSurMoi = 0
                        }
                        carte.invalidate()
                    },
                    onRelease = { it.onDetach() },
                )
            }
        }

        // Couches à cocher, avec leur nombre d'objets.
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CaseCouche(stringResource(R.string.carte_couche_foret, carto.foret.size), voirForet) { voirForet = it }
            CaseCouche(stringResource(R.string.carte_couche_ug, carto.ug.size), voirUg) { voirUg = it }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CaseCouche(stringResource(R.string.carte_couche_parcelles, carto.parcelles.size), voirParcelles) { voirParcelles = it }
            CaseCouche(stringResource(R.string.carte_couche_placettes, placettes.size), voirPlacettes) { voirPlacettes = it }
        }
        if (carto.anomalies.isNotEmpty()) {
            Text(stringResource(R.string.carte_anomalies, carto.anomalies.size), fontSize = 14.sp, color = Palette.Orange, modifier = Modifier.padding(horizontal = 8.dp))
        }

        // L'action : guider vers la placette touchée, sinon ouvrir la liste.
        val p = choisie
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { centrerSurMoi++ }, enabled = fix != null, modifier = Modifier.height(64.dp)) { Text(stringResource(R.string.carte_ma_position), fontSize = 16.sp) }
            if (p != null) {
                val d = distances[p.uuid]
                Button(
                    onClick = { onGuider(p.uuid) },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Vert),
                    modifier = Modifier.weight(1f).height(64.dp),
                ) {
                    Text(
                        if (d != null) stringResource(R.string.carte_guider_distance, p.code.orEmpty(), Libelles.distance(d)) else stringResource(R.string.carte_guider, p.code.orEmpty()),
                        fontSize = 18.sp,
                    )
                }
            } else {
                Button(onClick = { liste = true }, modifier = Modifier.weight(1f).height(64.dp)) { Text(stringResource(R.string.carte_placettes_liste), fontSize = 18.sp) }
            }
        }

        // Source et date, en petit (ADR 0009 : attribution datée).
        Text(attribution(ortho), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun attribution(o: OrthoTournee): String = when {
    o.fichierLocal != null -> stringResource(R.string.ortho_attribution_fichier, OrthoIgn.ATTRIBUTION)
    o.etat is EtatOrtho.Partielle -> stringResource(R.string.ortho_attribution_partielle, OrthoIgn.ATTRIBUTION)
    o.telechargeeLe != null -> stringResource(R.string.ortho_attribution_datee, OrthoIgn.ATTRIBUTION, o.telechargeeLe)
    else -> stringResource(R.string.ortho_attribution_sans_date, OrthoIgn.ATTRIBUTION)
}

@Composable
private fun CaseCouche(libelle: String, coche: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.clickable { onChange(!coche) }.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = coche, onCheckedChange = onChange)
        Text(libelle, fontSize = 16.sp)
    }
}

@Composable
private fun ListePlacettes(placettes: List<PlacetteTournee>, distances: Map<String, Double>, onChoisir: (PlacetteTournee) -> Unit, onFermer: () -> Unit) {
    val triees = remember(placettes, distances) { placettes.sortedWith(compareBy({ distances[it.uuid] ?: Double.MAX_VALUE }, { it.code })) }
    Column(Modifier.fillMaxSize()) {
        OutlinedButton(onClick = onFermer, modifier = Modifier.fillMaxWidth().padding(8.dp).height(56.dp)) { Text(stringResource(R.string.carte_fermer_liste), fontSize = 18.sp) }
        LazyColumn {
            items(triees, key = { it.uuid }) { p ->
                Row(
                    Modifier.fillMaxWidth().clickable { onChoisir(p) }.height(64.dp).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.code.orEmpty(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(distances[p.uuid]?.let { Libelles.distance(it) } ?: "—", fontSize = 22.sp)
                }
                HorizontalDivider()
            }
        }
    }
}
