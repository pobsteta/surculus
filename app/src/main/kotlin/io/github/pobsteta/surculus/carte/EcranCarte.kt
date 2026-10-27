/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import android.graphics.Color as CouleurAndroid
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.core.carte.EtatOrtho
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.guidage.Guidage
import io.github.pobsteta.surculus.core.guidage.Libelles
import io.github.pobsteta.surculus.gpkg.EtatSuivi
import io.github.pobsteta.surculus.gpkg.FormePastille
import io.github.pobsteta.surculus.gpkg.Geometrie
import io.github.pobsteta.surculus.gpkg.ObjetSurfacique
import io.github.pobsteta.surculus.gpkg.Pastille
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
import io.github.pobsteta.surculus.gpkg.decomptes
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
 * Les placettes sont colorées selon `etat_suivi` (rouge `jamais`, orange `a_programmer` (« travaux à prévoir », pilotage/journal.md, 2026-10-22, décision de Pascal, mission 033, libellé), anneau vert `a_jour`, noir « ? »
 * sinon) : pilotage/journal.md, 2026-10-22, décisions de Pascal (mission 033). La couleur vient de sommieR, lue à
 * l'ouverture de la tournée : elle ne change pas pendant la tournée. Ni travaux ni vides à ce stade.
 */
@Composable
fun EcranCarte(id: String, onRetour: () -> Unit, onGuider: (String) -> Unit) {
    when (val c = chargerTournee(id)) {
        Chargement.EnCours -> Column(Modifier.padding(24.dp)) { Text(stringResource(R.string.carte_ouverture)) }
        is Chargement.Refus -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.carte_refus, c.message), color = Palette.Rouge)
            OutlinedButton(onClick = onRetour, modifier = Modifier.height(56.dp)) { Text(stringResource(R.string.tournees_retour)) }
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

/** Pastille d'une placette : filet blanc, puis disque plein, anneau creux ou disque noir marqué d'un « ? ». */
private fun pastille(etat: EtatSuivi, densite: Float): Drawable {
    val p = Pastille.de(etat)
    val taille = (if (p.forme == FormePastille.ANNEAU) 26 else 32) * densite
    val px = taille.toInt()
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val centre = px / 2f
    val couleur = Palette.de(p.teinte).toArgb()
    val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
    val filet = 3 * densite
    when (p.forme) {
        FormePastille.ANNEAU -> {
            val epaisseur = 5 * densite
            pinceau.style = Paint.Style.STROKE
            pinceau.color = CouleurAndroid.WHITE
            pinceau.strokeWidth = epaisseur + 2 * filet
            c.drawCircle(centre, centre, centre - filet - epaisseur / 2 - 1, pinceau)
            pinceau.color = couleur
            pinceau.strokeWidth = epaisseur
            c.drawCircle(centre, centre, centre - filet - epaisseur / 2 - 1, pinceau)
        }
        else -> {
            pinceau.style = Paint.Style.FILL
            pinceau.color = CouleurAndroid.WHITE
            c.drawCircle(centre, centre, centre - 1, pinceau)
            pinceau.color = couleur
            c.drawCircle(centre, centre, centre - filet - 1, pinceau)
            if (p.forme == FormePastille.DISQUE_AVEC_POINT_INTERROGATION) {
                pinceau.color = CouleurAndroid.WHITE
                pinceau.textSize = 20 * densite
                pinceau.isFakeBoldText = true
                pinceau.textAlign = Paint.Align.CENTER
                c.drawText("?", centre, centre - (pinceau.descent() + pinceau.ascent()) / 2, pinceau)
            }
        }
    }
    return BitmapDrawable(null, bmp)
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

    val ortho by produceState<OrthoTournee?>(null, ouverte) {
        value = withContext(Dispatchers.IO) { OrthoDeTournee.etat(ouverte.dossier, ouverte.tournee.empriseForet) }
    }
    val distances: Map<String, Double> = remember(fix, placettes) {
        val pos = fix?.position?.let { Guidage.versLambert93(it) }
        if (pos == null) emptyMap() else placettes.associate { p -> p.uuid to kotlin.math.hypot(p.position!!.xMetres - pos.xMetres, p.position!!.yMetres - pos.yMetres) }
    }

    Column(Modifier.fillMaxSize()) {
        // Une ligne : retour à gauche, badge du fix à droite.
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onRetour, modifier = Modifier.height(56.dp)) { Text(stringResource(R.string.tournees_retour)) }
            BadgeFix(fix)
        }
        if (fix != null && Guidage.versLambert93(fix.position) == null) {
            Text(stringResource(R.string.carte_hors_france), color = Palette.Rouge, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        }
        if (refusee) Text(stringResource(R.string.carte_permission), color = Palette.Rouge, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 8.dp))

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
                                    icon = pastille(p.etatSuivi, densite)
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
        // Légende : un décompte par état, dans la couleur du disque ; « sans état » seulement s'il y en a.
        val nombres = remember(placettes) { decomptes(placettes) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.carte_legende_jamais, nombres.getValue(EtatSuivi.JAMAIS)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Palette.Rouge)
            Text(stringResource(R.string.carte_legende_a_programmer, nombres.getValue(EtatSuivi.A_PROGRAMMER)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Palette.Orange)
            Text(stringResource(R.string.carte_legende_a_jour, nombres.getValue(EtatSuivi.A_JOUR)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Palette.Vert)
            if (nombres.getValue(EtatSuivi.INCONNU) > 0) {
                Text(stringResource(R.string.carte_legende_inconnu, nombres.getValue(EtatSuivi.INCONNU)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Palette.Noir)
            }
        }
        if (carto.anomalies.isNotEmpty()) {
            Text(stringResource(R.string.carte_anomalies, carto.anomalies.size), style = MaterialTheme.typography.bodyMedium, color = Palette.Orange, modifier = Modifier.padding(horizontal = 8.dp))
        }

        // L'action : guider vers la placette touchée, sinon ouvrir la liste.
        val p = choisie
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { centrerSurMoi++ }, enabled = fix != null, modifier = Modifier.height(64.dp)) { Text(stringResource(R.string.carte_ma_position)) }
            if (p != null) {
                val d = distances[p.uuid]
                Button(
                    onClick = { onGuider(p.uuid) },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Vert),
                    modifier = Modifier.weight(1f).height(64.dp),
                ) {
                    Text(
                        if (d != null) stringResource(R.string.carte_guider_distance, p.code.orEmpty(), Libelles.distance(d)) else stringResource(R.string.carte_guider, p.code.orEmpty()),
                    )
                }
            } else {
                Button(onClick = { liste = true }, modifier = Modifier.weight(1f).height(64.dp)) { Text(stringResource(R.string.carte_placettes_liste)) }
            }
        }

        // Source et date, en petit (ADR 0009 : attribution datée).
        Text(ortho?.let { attribution(it) } ?: "", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
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
        Text(libelle)
    }
}

@Composable
private fun ListePlacettes(placettes: List<PlacetteTournee>, distances: Map<String, Double>, onChoisir: (PlacetteTournee) -> Unit, onFermer: () -> Unit) {
    val triees = remember(placettes, distances) { placettes.sortedWith(compareBy({ distances[it.uuid] ?: Double.MAX_VALUE }, { it.code })) }
    Column(Modifier.fillMaxSize()) {
        OutlinedButton(onClick = onFermer, modifier = Modifier.fillMaxWidth().padding(8.dp).height(56.dp)) { Text(stringResource(R.string.carte_fermer_liste)) }
        LazyColumn {
            items(triees, key = { it.uuid }) { p ->
                Row(
                    Modifier.fillMaxWidth().clickable { onChoisir(p) }.height(64.dp).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.code.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    Text(distances[p.uuid]?.let { Libelles.distance(it) } ?: "—", style = MaterialTheme.typography.titleLarge)
                }
                HorizontalDivider()
            }
        }
    }
}
