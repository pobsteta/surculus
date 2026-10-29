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
import fr.nemeton.terrain.style.Schema
import fr.nemeton.terrain.style.StyleTerrain

// Le thème vient de terrain-style v0.7.1 (mission 069, ADR 0008) : plus de copie littérale (mission 038).
// Les valeurs sont des ARGB 0xFFRRGGBB en Long ; les rôles absents de Marculus gardent les défauts de Material 3.

private fun Schema.versColorScheme(sombre: Boolean): ColorScheme {
    fun c(v: Long) = Color(v.toInt())
    return (if (sombre) darkColorScheme() else lightColorScheme()).copy(
        primary = c(primary), onPrimary = c(onPrimary),
        primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
        inversePrimary = c(inversePrimary),
        secondary = c(secondary), onSecondary = c(onSecondary),
        secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
        tertiary = c(tertiary), onTertiary = c(onTertiary),
        tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
        background = c(background), onBackground = c(onBackground),
        surface = c(surface), onSurface = c(onSurface),
        surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
        surfaceTint = c(surfaceTint),
        inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface),
        error = c(error), onError = c(onError), errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
        outline = c(outline), outlineVariant = c(outlineVariant), scrim = c(scrim),
        surfaceBright = c(surfaceBright), surfaceDim = c(surfaceDim),
        surfaceContainer = c(surfaceContainer), surfaceContainerHigh = c(surfaceContainerHigh),
        surfaceContainerHighest = c(surfaceContainerHighest), surfaceContainerLow = c(surfaceContainerLow),
        surfaceContainerLowest = c(surfaceContainerLowest),
        primaryFixed = c(primaryFixed), primaryFixedDim = c(primaryFixedDim),
        onPrimaryFixed = c(onPrimaryFixed), onPrimaryFixedVariant = c(onPrimaryFixedVariant),
        secondaryFixed = c(secondaryFixed), secondaryFixedDim = c(secondaryFixedDim),
        onSecondaryFixed = c(onSecondaryFixed), onSecondaryFixedVariant = c(onSecondaryFixedVariant),
        tertiaryFixed = c(tertiaryFixed), tertiaryFixedDim = c(tertiaryFixedDim),
        onTertiaryFixed = c(onTertiaryFixed), onTertiaryFixedVariant = c(onTertiaryFixedVariant),
    )
}

/** Schéma clair, pris de [StyleTerrain.clair] (pas de couleur dynamique, ADR 0008). */
val SchemaClair: ColorScheme = StyleTerrain.clair.versColorScheme(sombre = false)

/** Schéma sombre, pris de [StyleTerrain.sombre] ; appliqué depuis la mission 051 (choix Sombre, ou Système avec le téléphone en sombre). */
val SchemaSombre: ColorScheme = StyleTerrain.sombre.versColorScheme(sombre = true)

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
