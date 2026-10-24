package io.github.pobsteta.surculus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Thème de Marculus v0.65.0, recopié tel quel (mission 038, ADR 0008 amendement du 2026-10-24 soir) :
// marculus, tag v0.65.0, app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt l. 10-28.
// Les autres rôles gardent les valeurs par défaut de Material 3 (BOM Compose 2026.02.01, la même des deux côtés).
// À la publication de terrain-style v0.6.1, une mission remplacera cette copie par la dépendance.
private val VertMarculus = Color(0xFF374742)
private val VertVif = Color(0xFF3E7A40)
private val Creme = Color(0xFFF5EBD8)

/** Schéma clair : celui que Surculus applique toujours (ADR 0008, valeurs sacrifiées : pas de couleur dynamique). */
val SchemaClair: ColorScheme = lightColorScheme(
    primary = VertVif,
    onPrimary = Color.White,
    primaryContainer = VertMarculus,
    onPrimaryContainer = Creme,
    secondary = VertMarculus,
)

/** Schéma sombre, identique à celui de Marculus pour la future bascule sur terrain-style ; non appliqué. */
val SchemaSombre: ColorScheme = darkColorScheme(
    primary = VertVif,
    onPrimary = Color.White,
    primaryContainer = VertMarculus,
    onPrimaryContainer = Creme,
    secondary = Creme,
)

/** Thème clair forcé, quel que soit le mode du téléphone (lisibilité au soleil). */
@Composable
fun SurculusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SchemaClair, content = content)
}
