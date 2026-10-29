/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.carte

import android.content.Context
import android.graphics.Color as CouleurAndroid
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.core.carte.EmpriseCarte
import io.github.pobsteta.surculus.core.carte.Fond
import io.github.pobsteta.surculus.core.carte.FondsCarte
import io.github.pobsteta.surculus.core.carte.MarqueurPosition
import io.github.pobsteta.surculus.core.carte.OrthoChantier
import io.github.pobsteta.surculus.core.carte.Recentrage
import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.guidage.Guidage
import io.github.pobsteta.surculus.core.guidage.Libelles
import io.github.pobsteta.surculus.gpkg.EtatSuivi
import io.github.pobsteta.surculus.ui.BandeauCompact
import io.github.pobsteta.surculus.ui.theme.LocalSombre
import io.github.pobsteta.surculus.gpkg.FormePastille
import io.github.pobsteta.surculus.gpkg.Geometrie
import io.github.pobsteta.surculus.gpkg.ObjetSurfacique
import io.github.pobsteta.surculus.gpkg.Pastille
import io.github.pobsteta.surculus.gpkg.PlacetteTournee
import io.github.pobsteta.surculus.gpkg.decomptes
import io.github.pobsteta.surculus.gpkg.ProblemePlacette
import io.github.pobsteta.surculus.gpkg.centrageInitial
import io.github.pobsteta.surculus.gpkg.empriseDeCadrage
import io.github.pobsteta.surculus.gpkg.probleme
import io.github.pobsteta.surculus.gpkg.cibleNavigation
import io.github.pobsteta.surculus.gpkg.titreForet
import io.github.pobsteta.surculus.tournee.DepotTournees
import io.github.pobsteta.surculus.tournee.TourneeOuverte
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay

/**
 * Carte de la tournée (spec § 4.2) : forêt, UG, parcelles, placettes, fonds de carte (menu « Fond de carte », comme Marculus), position et badge du fix.
 * Une question : où suis-je par rapport aux placettes ? Un appui sur une placette (ou sur la liste) ouvre le guidage.
 * Les placettes sont colorées selon `etat_suivi` (rouge `jamais`, orange `a_programmer` (« travaux à prévoir », pilotage/journal.md, 2026-10-22, décision de Pascal, mission 033, libellé), anneau vert `a_jour`, noir « ? »
 * sinon) : pilotage/journal.md, 2026-10-22, décisions de Pascal (mission 033). La couleur vient de sommieR, lue à
 * l'ouverture de la tournée : elle ne change pas pendant la tournée. Ni travaux ni vides à ce stade.
 */
@Composable
fun EcranCarte(id: String, centreId: String?, onRetour: () -> Unit, onGuider: (String) -> Unit, onControler: (String) -> Unit = {}) {
    when (val c = chargerTournee(id)) {
        Chargement.EnCours -> Column {
            BandeauCarte(stringResource(R.string.carte_titre), onRetour)
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).padding(24.dp)) { Text(stringResource(R.string.carte_ouverture)) }
        }
        is Chargement.Refus -> Column {
            BandeauCarte(stringResource(R.string.carte_titre), onRetour)
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).padding(24.dp)) { Text(stringResource(R.string.carte_refus, c.message), color = couleurErreur()) }
        }
        is Chargement.Pret -> {
            // L'ortho chantier n'est proposée que si le dossier de la tournée porte un fichier tuilé exploitable. On attend
            // cette vérification avant de créer la carte : elle s'ouvre directement sur le bon fond, sans tuile en ligne
            // demandée pour rien.
            val examenOrtho by produceState<ExamenOrtho?>(null, c.tournee) {
                value = withContext(Dispatchers.IO) { FichierOrtho.examiner(c.tournee.dossier) }
            }
            val ortho = examenOrtho
            if (ortho == null) {
                Column {
                    BandeauCarte(stringResource(R.string.carte_titre), onRetour)
                    Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).padding(24.dp)) { Text(stringResource(R.string.carte_ouverture)) }
                }
            } else {
                CartePrete(c.tournee, ortho, centreId, onRetour, onGuider, onControler)
            }
        }
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

private class Couches(
    val foret: List<Overlay>,
    val ug: List<Overlay>,
    val parcelles: List<Overlay>,
    val placettes: List<Marker>,
    val position: Marker,
    val cadastre: TilesOverlay,
) {
    var carte: MapView? = null

    /** Fond dont le fournisseur de tuiles est posé sur la carte : on n'en change que lorsque le choix change. */
    var fondApplique: Fond? = null
}

/** Préférences de la carte : le fond retrouvé d'une ouverture à l'autre (mêmes nom et clé que Marculus). */
private const val PREFS_CARTE = "carte"
private const val CLE_CADASTRE = "cadastre"

/** Zoom d'un recentrage sur un point (Marculus : `recadrerSur`, `setZoom(17.0)`). */
private const val ZOOM_RECENTRER = 17.0

/**
 * Bandeau compact de la carte (mission 053) avec la flèche de retour (même action que le retour système), comme Marculus
 * (`CarteScreen.kt` l. 634-640). [actions] : le badge du fix et l'icône « Couches ».
 */
@Composable
private fun BandeauCarte(titre: String, onRetour: () -> Unit, actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}) {
    BandeauCompact(
        titre = titre,
        navigationIcon = {
            IconButton(onClick = onRetour) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.carte_retour))
            }
        },
        actions = actions,
    )
}

/** Titre d'une section du menu de la carte (« Fond de carte », « Couches affichées »), comme Marculus (l. 141-143). */
@Composable
private fun TitreSection(texte: String) {
    Text(texte, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

/** Une couche dans le menu : sa case, son libellé ; grisée quand elle est vide (Marculus, l. 160-172). */
@Composable
private fun LigneCouche(libelle: String, coche: Boolean, disponible: Boolean, onChange: (Boolean) -> Unit) {
    DropdownMenuItem(
        enabled = disponible,
        text = { Text(libelle) },
        leadingIcon = { Checkbox(checked = coche && disponible, enabled = disponible, onCheckedChange = null) },
        onClick = { if (disponible) onChange(!coche) },
    )
}

/**
 * Recadre la carte sur l'emprise de la tournée. Équivalent du `recadrerSur` de Marculus (`CarteScreen.kt` l. 870-878) :
 * plusieurs points, `zoomToBoundingBox` ; un seul, zoom 17 puis `animateTo`.
 */
private fun cadrerSur(carte: MapView, emprise: EmpriseLambert93, marge: Int, animer: Boolean) {
    val nordEst = GeoPoint(io.github.pobsteta.surculus.core.geo.PointLambert93(emprise.xMaxMetres, emprise.yMaxMetres))
    val sudOuest = GeoPoint(io.github.pobsteta.surculus.core.geo.PointLambert93(emprise.xMinMetres, emprise.yMinMetres))
    carte.zoomToBoundingBox(BoundingBox(nordEst.latitude, nordEst.longitude, sudOuest.latitude, sudOuest.longitude), animer, marge)
}

/** Un fond dans le menu : bouton radio, ligne d'au moins 48 dp (gants). */
@Composable
private fun LigneFond(libelle: String, choisi: Boolean, onChoisir: () -> Unit) {
    DropdownMenuItem(
        text = { Text(libelle) },
        leadingIcon = { RadioButton(selected = choisi, onClick = null) },
        onClick = onChoisir,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartePrete(ouverte: TourneeOuverte, examenOrtho: ExamenOrtho, centreId: String?, onRetour: () -> Unit, onGuider: (String) -> Unit, onControler: (String) -> Unit) {
    val orthoChantier = examenOrtho.exploitable
    val ctx = LocalContext.current
    val (fix, refusee) = rememberFix()
    val carto = ouverte.cartographie
    val placettes = remember(ouverte) { ouverte.tournee.placettes.filter { it.position != null && it.code != null } }

    var voirForet by remember { mutableStateOf(true) }
    var voirUg by remember { mutableStateOf(true) }
    var voirParcelles by remember { mutableStateOf(false) }
    var voirPlacettes by remember { mutableStateOf(true) }
    // Ouverte depuis une placette de l'écran « Placettes » : elle est déjà la placette choisie (mission 061).
    // Toutes les placettes de la tournée, même sans position : une placette sans position touchée dans la liste ouvre la carte
    // cadrée et le dit, elle ne se perd pas en silence. La choisie survit à une rotation (son uuid est sauvegardé).
    val toutes = ouverte.tournee.placettes
    val centre = remember(ouverte, centreId) { toutes.firstOrNull { it.uuid == centreId } }
    var choisieUuid by rememberSaveable(ouverte.dossier.name, centreId) { mutableStateOf(centreId) }
    val choisie = toutes.firstOrNull { it.uuid == choisieUuid }
    val controlees = remember(ouverte) { DepotTournees.placettesControlees(ctx, ouverte.dossier.name) }

    // Fond de carte : le choix mémorisé pour cette tournée, sinon l'Ortho chantier si la tournée en a une, sinon le Plan IGN.
    val prefsCarte = remember { ctx.getSharedPreferences(PREFS_CARTE, Context.MODE_PRIVATE) }
    // `fond` : le choix fait dans cette tournée (ortho chantier comprise), nul tant qu'il n'y en a pas.
    val cleFond = FondsCarte.cle(ouverte.dossier.name)
    var fond by remember(cleFond) { mutableStateOf(FondsCarte.depuisNom(prefsCarte.getString(cleFond, null))) }
    var menuFonds by remember { mutableStateOf(false) }
    var voirCadastre by remember { mutableStateOf(prefsCarte.getBoolean(CLE_CADASTRE, FondsCarte.CADASTRE_PAR_DEFAUT)) }
    val fondAffiche = FondsCarte.affiche(fond, orthoChantier)
    // Garde-fou : on ne cadre la carte que sur une emprise en France (sinon : la Somalie, sans alerte).
    val emprise: EmpriseLambert93? = carto.empriseUg ?: ouverte.tournee.empriseForet
    val empriseHorsFrance = remember(emprise) { emprise != null && EmpriseCarte.controler(emprise) is EmpriseCarte.Resultat.HorsFrance }
    // Ce que « maison » et l'ouverture cadrent : l'emprise de la tournée si elle est en France, sinon les placettes en France
    // (mission 061) ; `null` : rien à cadrer, « maison » est grisé.
    val cadre: EmpriseLambert93? = remember(emprise, empriseHorsFrance, toutes) { empriseDeCadrage(if (empriseHorsFrance) null else emprise, toutes) }
    val distances: Map<String, Double> = remember(fix, placettes) {
        val pos = fix?.position?.let { Guidage.versLambert93(it) }
        if (pos == null) emptyMap() else placettes.associate { p -> p.uuid to kotlin.math.hypot(p.position!!.xMetres - pos.xMetres, p.position!!.yMetres - pos.yMetres) }
    }

    Column(Modifier.fillMaxSize()) {
        // Bandeau compact : flèche de retour à gauche ; à droite le badge du fix et l'icône « Couches » (comme Marculus).
        BandeauCarte(
            titre = titreForet(ouverte.tournee.projet?.foret, ouverte.tournee.nomForet, ouverte.dossier.name),
            onRetour = onRetour,
            actions = {
                BadgeFix(fix)
                Spacer(Modifier.width(4.dp))
                Box {
                    IconButton(onClick = { menuFonds = true }) {
                        Icon(Icons.Filled.Layers, contentDescription = stringResource(R.string.carte_couches))
                    }
                    // Un seul menu : le fond (un choix) puis les couches (des cases). Il reste ouvert après un choix :
                    // on règle fond et couches d'un même geste (Marculus, l. 642-645).
                    DropdownMenu(expanded = menuFonds, onDismissRequest = { menuFonds = false }) {
                        TitreSection(stringResource(R.string.carte_fond_de_carte))
                        FondsCarte.proposes(orthoChantier).forEach { f ->
                            LigneFond(libelle = f.libelle, choisi = fondAffiche == f) {
                                fond = f
                                prefsCarte.edit().putString(cleFond, f.name).apply()
                            }
                        }
                        HorizontalDivider()
                        TitreSection(stringResource(R.string.carte_couches))
                        LigneCouche(stringResource(R.string.carte_couche_cadastre), voirCadastre, disponible = true) {
                            voirCadastre = it
                            prefsCarte.edit().putBoolean(CLE_CADASTRE, it).apply()
                        }
                        LigneCouche(stringResource(R.string.carte_couche_foret, carto.foret.size), voirForet, carto.foret.isNotEmpty()) { voirForet = it }
                        LigneCouche(stringResource(R.string.carte_couche_ug, carto.ug.size), voirUg, carto.ug.isNotEmpty()) { voirUg = it }
                        LigneCouche(stringResource(R.string.carte_couche_parcelles, carto.parcelles.size), voirParcelles, carto.parcelles.isNotEmpty()) { voirParcelles = it }
                        LigneCouche(stringResource(R.string.carte_couche_placettes, placettes.size), voirPlacettes, placettes.isNotEmpty()) { voirPlacettes = it }
                    }
                }
            },
        )
        Column(Modifier.weight(1f).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
        // Fichier d'ortho présent mais illisible (mission 059) : dit en clair, la carte est déjà repliée sur le Plan IGN.
        if (examenOrtho.illisible) {
            Text(OrthoChantier.ILLISIBLE, color = couleurErreur(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        }
        if (empriseHorsFrance) {
            Text(stringResource(R.string.carte_emprise_hors_france), color = couleurErreur(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        }
        if (fix != null && Guidage.versLambert93(fix.position) == null) {
            Text(stringResource(R.string.carte_hors_france), color = couleurErreur(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        }
        if (refusee) Text(stringResource(R.string.carte_permission), color = couleurErreur(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 8.dp))

        // Rogne la carte à ses bords (mission 060) : osmdroid et Compose ne le font pas, la carte recouvrait le bandeau.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            run {
                val couchesRef = remember(ouverte) { arrayOfNulls<Couches>(1) }
                AndroidView(
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                    factory = { contexte ->
                        configurerOsmdroid(contexte)
                        // Fond posé par `update` ; le réseau est utilisé par les fonds en ligne (cache d'osmdroid gardé).
                        val carte = MapView(contexte, fournisseurDeFond(contexte, fondAffiche, if (fondAffiche == Fond.ORTHO) FichierOrtho.ouvrir(ouverte.dossier) else null))
                        carte.setMultiTouchControls(true)
                        // Pas de + et − d'osmdroid qui apparaissent au zoom : les boutons de la carte suffisent (Marculus, CarteScreen.kt l. 308) ; le pincement reste.
                        carte.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        carte.maxZoomLevel = zoomMaxDeFond(fondAffiche)
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
                            cadastre = coucheCadastre(contexte),
                        )
                        l.carte = carte
                        (l.foret + l.ug + l.parcelles + l.placettes + l.position).forEach { carte.overlays.add(it) }
                        l.placettes.forEach { m -> m.setOnMarkerClickListener { marqueur, _ -> choisieUuid = (marqueur.relatedObject as? PlacetteTournee)?.uuid; true } }
                        l.fondApplique = fondAffiche
                        couchesRef[0] = l
                        // Ouverte depuis une placette : centrée dessus (si elle tombe en France), sinon cadrée sur la tournée.
                        val centrage = centrageInitial(centre)
                        if (centrage != null) {
                            carte.post { carte.controller.setZoom(ZOOM_RECENTRER); carte.controller.setCenter(GeoPoint(centrage)) }
                        } else if (cadre != null) {
                            carte.post { cadrerSur(carte, cadre, (48 * densite).toInt(), animer = false) }
                        }
                        carte
                    },
                    update = { carte ->
                        val l = couchesRef[0] ?: return@AndroidView
                        if (l.fondApplique != fondAffiche) {
                            // Un fournisseur NEUF à chaque changement : osmdroid détache l'ancien (comme Marculus).
                            carte.tileProvider = fournisseurDeFond(carte.context, fondAffiche, if (fondAffiche == Fond.ORTHO) FichierOrtho.ouvrir(ouverte.dossier) else null)
                            carte.maxZoomLevel = zoomMaxDeFond(fondAffiche)
                            // Retour vers un fond moins profond (ortho IGN vers OSM) : on redescend, sinon fond blanc.
                            if (carte.zoomLevelDouble > carte.maxZoomLevel) carte.controller.setZoom(carte.maxZoomLevel)
                            l.fondApplique = fondAffiche
                        }
                        // Cadastre IGN tout en dessous : forêt, UG et placettes restent lisibles (Marculus, l. 441-442).
                        val cadastrePose = l.cadastre in carte.overlays
                        if (voirCadastre && !cadastrePose) carte.overlays.add(0, l.cadastre)
                        if (!voirCadastre && cadastrePose) carte.overlays.remove(l.cadastre)
                        l.foret.forEach { it.isEnabled = voirForet }
                        l.ug.forEach { it.isEnabled = voirUg }
                        l.parcelles.forEach { it.isEnabled = voirParcelles }
                        l.placettes.forEach { it.isEnabled = voirPlacettes }
                        // Pas de marqueur pour un fix hors de France (la Somalie) ; le message rouge reste affiché.
                        val aDessiner = MarqueurPosition.aDessiner(fix?.position)
                        l.position.isEnabled = aDessiner != null
                        aDessiner?.let { l.position.position = GeoPoint(it.latitude, it.longitude) }
                        carte.invalidate()
                    },
                    // Le fournisseur de tuiles du cadastre se libère aussi quand la couche est décochée (elle n'est alors
                    // plus dans les surcouches de la carte, donc `onDetach` de la carte ne l'atteint pas).
                    onRelease = { carte ->
                        couchesRef[0]?.cadastre?.onDetach(carte)
                        carte.onDetach()
                    },
                )
                // Les boutons de Groma (`EcranCarte.kt` l. 323-389), au moins 48 dp : naviguer vers (la placette choisie, sinon la
                // prochaine à faire ; inactif s'il n'y en a pas), zoom avant, zoom arrière, maison (cadrer la tournée), cible
                // (la position GNSS si elle tombe en France, zoom 17 au moins, sinon l'emprise de la tournée : `Recentrage.cible`).
                val cibleNaviguer = cibleNavigation(choisie, toutes, controlees, fix?.position?.let { Guidage.versLambert93(it) })
                // Mission 064 : en paysage la hauteur visible ne suffit plus à une colonne ; les boutons passent en deux colonnes
                // (`DispositionBoutons`). En portrait, une seule colonne, comme avant.
                val nombreBoutons = if (choisie != null) 6 else 5
                val parColonne = DispositionBoutons.boutonsParColonne(maxHeight.value, nombreBoutons)
                FlowColumn(
                    Modifier.align(Alignment.BottomEnd).padding(DispositionBoutons.MARGE_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(DispositionBoutons.ECART_DP.dp),
                    horizontalArrangement = Arrangement.spacedBy(DispositionBoutons.ECART_DP.dp),
                    maxItemsInEachColumn = parColonne,
                ) {
                    // Porte n° 1 du contrôle (mission 012-B1) : « Contrôler » seulement quand une placette est choisie.
                    if (choisie != null) {
                        SmallFloatingActionButton(
                            onClick = { onControler(choisie.uuid) },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.controle_controler))
                        }
                    }
                    SmallFloatingActionButton(
                        onClick = { cibleNaviguer?.let { onGuider(it.uuid) } },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { if (cibleNaviguer == null) disabled() },
                        containerColor = if (cibleNaviguer != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (cibleNaviguer != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Icon(Icons.Filled.Navigation, contentDescription = stringResource(R.string.carte_naviguer))
                    }
                    SmallFloatingActionButton(onClick = { couchesRef[0]?.carte?.controller?.zoomIn() }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.carte_zoom_avant))
                    }
                    SmallFloatingActionButton(onClick = { couchesRef[0]?.carte?.controller?.zoomOut() }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.carte_zoom_arriere))
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            val carte = couchesRef[0]?.carte ?: return@SmallFloatingActionButton
                            if (cadre != null) cadrerSur(carte, cadre, (48 * carte.context.resources.displayMetrics.density).toInt(), animer = true)
                        },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { if (cadre == null) disabled() },
                        containerColor = if (cadre != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (cadre != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.carte_cadrer_tournee))
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            val carte = couchesRef[0]?.carte ?: return@SmallFloatingActionButton
                            when (val c = Recentrage.cible(fix?.position, cadre)) {
                                is Recentrage.Cible.Fix -> {
                                    if (carte.zoomLevelDouble < ZOOM_RECENTRER) carte.controller.setZoom(ZOOM_RECENTRER)
                                    carte.controller.animateTo(GeoPoint(c.position.latitude, c.position.longitude))
                                }
                                Recentrage.Cible.Emprise ->
                                    cadrerSur(carte, cadre!!, (48 * carte.context.resources.displayMetrics.density).toInt(), animer = true)
                                Recentrage.Cible.Rien -> {}
                            }
                        },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.carte_recentrer))
                    }
                }
            }
        }

        // Légende : un décompte par état, dans la couleur du disque ; « sans état » seulement s'il y en a.
        val nombres = remember(placettes) { decomptes(placettes) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            LigneLegende(EtatSuivi.JAMAIS, stringResource(R.string.carte_legende_jamais, nombres.getValue(EtatSuivi.JAMAIS)), Palette.Rouge)
            LigneLegende(EtatSuivi.A_PROGRAMMER, stringResource(R.string.carte_legende_a_programmer, nombres.getValue(EtatSuivi.A_PROGRAMMER)), Palette.Orange)
            LigneLegende(EtatSuivi.A_JOUR, stringResource(R.string.carte_legende_a_jour, nombres.getValue(EtatSuivi.A_JOUR)), Palette.Vert)
            if (nombres.getValue(EtatSuivi.INCONNU) > 0) {
                LigneLegende(EtatSuivi.INCONNU, stringResource(R.string.carte_legende_inconnu, nombres.getValue(EtatSuivi.INCONNU)), Palette.Noir)
            }
        }
        if (carto.anomalies.isNotEmpty()) {
            TexteEtat(stringResource(R.string.carte_anomalies, carto.anomalies.size), Palette.Orange, Modifier.padding(horizontal = 8.dp), MaterialTheme.typography.bodyMedium)
        }

        // La placette choisie (un appui sur son marqueur, ou l'appui sur l'écran « Placettes ») : son code et sa distance.
        // L'action est le bouton « naviguer vers » (mission 061) ; plus de bouton « Placettes » sous la carte.
        choisie?.let { p ->
            val d = distances[p.uuid]
            val probleme = probleme(p)
            if (probleme != null) {
                Text(
                    stringResource(if (probleme == ProblemePlacette.SANS_POSITION) R.string.carte_placette_sans_position else R.string.carte_placette_hors_france, p.code.orEmpty()),
                    color = couleurErreur(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                )
            } else Text(
                if (d != null) stringResource(R.string.carte_choisie_distance, p.code.orEmpty(), Libelles.distance(d)) else stringResource(R.string.carte_choisie, p.code.orEmpty()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        // Attributions du fond (IGN ou OSM) et du cadastre, en petit ; l'ortho chantier dit la source et la date de son
        // fichier (mission 059, Licence Ouverte 2.0).
        FondsCarte.mentions(fondAffiche, voirCadastre, examenOrtho.mention).joinToString(" · ").takeIf { it.isNotEmpty() }
            ?.let { Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
        }
    }
}

/**
 * Une ligne de la légende (mission 063). En clair : le texte dans la teinte de la pastille, comme avant. En sombre : la
 * pastille cernée de blanc (même forme que sur la carte) puis le texte en `onSurface`, sans le symbole du libellé.
 */
@Composable
private fun LigneLegende(etat: EtatSuivi, libelle: String, teinte: Color) {
    val style = MaterialTheme.typography.bodyMedium
    if (LocalSombre.current) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PastilleEtat(etat, 16.dp)
            Spacer(Modifier.width(4.dp))
            Text(libelle.substringAfter(' '), style = style, fontWeight = FontWeight.Bold, color = couleurTexteEtat(teinte))
        }
    } else {
        Text(libelle, style = style, fontWeight = FontWeight.Bold, color = teinte)
    }
}
