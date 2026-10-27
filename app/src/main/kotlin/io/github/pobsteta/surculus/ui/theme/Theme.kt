package io.github.pobsteta.surculus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Thème de Marculus v0.65.0, recopié tel quel (mission 038, ADR 0008 amendement du 2026-10-24 soir) :
// marculus, tag v0.65.0, app/src/main/kotlin/io/github/pobsteta/marculus/ui/theme/Theme.kt l. 10-28.
// Les autres rôles gardent les valeurs par défaut de Material 3 (BOM Compose 2026.02.01, la même des deux côtés).
// À la publication de terrain-style v0.6.1, une mission remplacera cette copie par la dépendance.
private val VertMarculus = Color(0xFF374742)
private val VertVif = Color(0xFF3E7A40)
private val Creme = Color(0xFFF5EBD8)

/** Schéma clair (ADR 0008, valeurs sacrifiées : pas de couleur dynamique). */
val SchemaClair: ColorScheme = lightColorScheme(
    primary = VertVif,
    onPrimary = Color.White,
    primaryContainer = VertMarculus,
    onPrimaryContainer = Creme,
    secondary = VertMarculus,
)

/** Schéma sombre, identique à celui de Marculus ; appliqué depuis la mission 051 (choix Sombre, ou Système avec le téléphone en sombre). */
val SchemaSombre: ColorScheme = darkColorScheme(
    primary = VertVif,
    onPrimary = Color.White,
    primaryContainer = VertMarculus,
    onPrimaryContainer = Creme,
    secondary = Creme,
)

/** Vrai quand le thème sombre est appliqué : les couleurs d'état (palette) se posent alors sur une plaque claire. */
val LocalSombre = staticCompositionLocalOf { false }

/**
 * Thème choisi dans Paramètres (mission 051) : [ChoixTheme.SYSTEME] suit le mode du téléphone (défaut, comme Marculus),
 * Clair et Sombre le forcent. Remplace le « thème clair forcé » du 2026-10-22.
 */
@Composable
fun SurculusTheme(choix: ChoixTheme = ChoixTheme.SYSTEME, content: @Composable () -> Unit) {
    val sombre = choix.estSombre(isSystemInDarkTheme())
    CompositionLocalProvider(LocalSombre provides sombre) {
        MaterialTheme(colorScheme = if (sombre) SchemaSombre else SchemaClair, content = content)
    }
}
