package io.github.pobsteta.surculus.data.journal

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UuidV7Test {

    @Test
    fun `vecteur de la RFC 9562 annexe A 6`() {
        // 017F22E2-79B0-7CC3-98C4-DC0C0C07398F : 2022-02-22 19:22:22 UTC, rand_a 0xCC3, rand_b 0x18C4DC0C0C07398F
        val u = GenerateurUuidV7.assembler(0x017F22E279B0L, 0xCC3, 0x18C4DC0C0C07398FL)
        assertEquals("017f22e2-79b0-7cc3-98c4-dc0c0c07398f", u)
        assertEquals(1_645_557_742_000L, GenerateurUuidV7.horodatageMs(u))
    }

    @Test
    fun `version 7 variante 10 et horodatage de l'horloge`() {
        val g = GenerateurUuidV7({ 1_760_000_000_123L }, Random(1))
        repeat(200) {
            val u = g.suivant()
            assertTrue(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(u), u)
            assertEquals(1_760_000_000_123L, GenerateurUuidV7.horodatageMs(u))
        }
    }

    @Test
    fun `strictement croissant meme a la meme milliseconde, meme au-dela de 4096 par milliseconde`() {
        val g = GenerateurUuidV7({ 1_760_000_000_000L }, Random(2))
        val tous = List(10_000) { g.suivant() }
        assertEquals(tous.size, tous.toSet().size)
        assertEquals(tous.sorted(), tous, "l'ordre lexicographique doit suivre l'ordre de génération")
    }

    @Test
    fun `horloge reculee d'une heure les uuid restent croissants`() {
        var maintenant = 1_760_000_000_000L
        val g = GenerateurUuidV7({ maintenant }, Random(3))
        val avant = g.suivant()
        maintenant -= 3_600_000L
        val apres = g.suivant()
        assertTrue(apres > avant, "$apres doit suivre $avant")
    }

    @Test
    fun `horloge qui avance l'horodatage suit`() {
        var maintenant = 1_760_000_000_000L
        val g = GenerateurUuidV7({ maintenant }, Random(4))
        g.suivant()
        maintenant += 5_000L
        assertEquals(maintenant, GenerateurUuidV7.horodatageMs(g.suivant()))
    }

    @Test
    fun `le generateur par defaut produit des uuid distincts`() {
        val g = GenerateurUuidV7()
        assertEquals(1000, List(1000) { g.suivant() }.toSet().size)
    }
}
