package io.github.pobsteta.surculus.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Égalité rôle par rôle avec le thème de Marculus v0.65.0 (mission 038). Les cinq rôles posés sont écrits en
 * littéral ici (pas importés du thème) ; chacun des autres vaut le défaut de Material 3 (BOM 2026.02.01).
 */
class ThemeTest {
    private val vertMarculus = Color(0xFF374742)
    private val vertVif = Color(0xFF3E7A40)
    private val creme = Color(0xFFF5EBD8)

    /** Tous les rôles du ColorScheme, lus par réflexion sur ses accesseurs de couleur : aucun n'échappe au test. */
    private fun roles(s: ColorScheme): Map<String, Color> =
        ColorScheme::class.java.methods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == java.lang.Long.TYPE }
            .associate { m ->
                val nom = m.name.removePrefix("get").substringBefore('-')
                nom to Color((m.invoke(s) as Long).toULong())
            }

    private fun attendu(base: ColorScheme, poses: Map<String, Color>): Map<String, Color> =
        roles(base).mapValues { (nom, defaut) -> poses[nom] ?: defaut }

    /** Compare rôle par rôle et nomme chaque rôle en écart (couleur en ARGB hexadécimal). */
    private fun comparer(attendu: Map<String, Color>, obtenu: Map<String, Color>) {
        assertEquals(attendu.keys, obtenu.keys, "rôles différents")
        val ecarts = attendu.filter { (nom, c) -> obtenu[nom] != c }.map { (nom, c) ->
            "$nom : attendu ${hex(c)}, obtenu ${hex(obtenu.getValue(nom))}"
        }
        assertTrue(ecarts.isEmpty(), "rôles en écart :\n" + ecarts.joinToString("\n"))
    }

    private fun hex(c: Color) = "#%08X".format(c.toArgb())

    @Test
    fun lesRolesSontTousLus() {
        // Garde-fou du test lui-même : la réflexion voit bien tous les rôles (Material 3 en compte plus de 40).
        val lus = roles(SchemaClair)
        assertTrue(lus.size >= 40, "rôles lus : ${lus.keys}")
        assertTrue("Primary" in lus && "OnPrimaryContainer" in lus && "Secondary" in lus)
    }

    @Test
    fun schemaClairEgalMarculusRoleParRole() {
        val poses = mapOf(
            "Primary" to vertVif, "OnPrimary" to Color.White,
            "SurfaceTint" to vertVif, // défaut de Material 3 : surfaceTint = primary, donc VertVif aussi chez Marculus
            "PrimaryContainer" to vertMarculus, "OnPrimaryContainer" to creme,
            "Secondary" to vertMarculus,
        )
        comparer(attendu(lightColorScheme(), poses), roles(SchemaClair))
    }

    @Test
    fun schemaSombreEgalMarculusRoleParRole() {
        val poses = mapOf(
            "Primary" to vertVif, "OnPrimary" to Color.White,
            "SurfaceTint" to vertVif, // défaut de Material 3 : surfaceTint = primary, donc VertVif aussi chez Marculus
            "PrimaryContainer" to vertMarculus, "OnPrimaryContainer" to creme,
            "Secondary" to creme,
        )
        comparer(attendu(darkColorScheme(), poses), roles(SchemaSombre))
    }
}
