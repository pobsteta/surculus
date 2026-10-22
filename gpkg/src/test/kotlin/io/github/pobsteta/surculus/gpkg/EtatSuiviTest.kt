package io.github.pobsteta.surculus.gpkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** Mission 033. Choix de forme et de teinte : pilotage/journal.md, 2026-10-22, décisions de Pascal (mission 033). */
class EtatSuiviTest {
    @TempDir
    lateinit var dossier: Path

    /** Couchey, P35-01 `jamais`, P35-02 `a_jour`, P35-03 `a_programmer`, plus une placette nulle et une valeur future. */
    private fun tourneeAvecEtats(): Path {
        val f = TourneeDeTest.couchey(dossier)
        Sqlite.ouvrir(f).use { cnx ->
            cnx.createStatement().use { st ->
                st.executeUpdate("UPDATE placettes SET etat_suivi = 'jamais' WHERE code_placette = 'P35-01'")
                st.executeUpdate("UPDATE placettes SET etat_suivi = 'a_jour' WHERE code_placette = 'P35-02'")
                st.executeUpdate("UPDATE placettes SET etat_suivi = 'a_programmer' WHERE code_placette = 'P35-03'")
                st.executeUpdate(
                    "INSERT INTO placettes (geom, uuid, code_placette, travaux_id, etat_suivi) " +
                        "SELECT geom, '${TourneeDeTest.uuid(900)}', 'P35-04', travaux_id, NULL FROM placettes WHERE code_placette = 'P35-01'",
                )
                st.executeUpdate(
                    "INSERT INTO placettes (geom, uuid, code_placette, travaux_id, etat_suivi) " +
                        "SELECT geom, '${TourneeDeTest.uuid(901)}', 'P35-05', travaux_id, 'a_surveiller_v2' FROM placettes WHERE code_placette = 'P35-01'",
                )
            }
        }
        return f
    }

    @Test
    fun `les trois valeurs, une valeur nulle et une valeur inconnue se lisent`() {
        val t = LecteurTournee.lire(tourneeAvecEtats())
        assertEquals(
            listOf(EtatSuivi.JAMAIS, EtatSuivi.A_JOUR, EtatSuivi.A_PROGRAMMER, EtatSuivi.INCONNU, EtatSuivi.INCONNU),
            t.placettes.map { it.etatSuivi },
        )
    }

    @Test
    fun `une ancienne tournee sans colonne etat_suivi se lit toujours, tout est inconnu`() {
        val f = TourneeDeTest.couchey(dossier, nom = "ancienne.gpkg")
        Sqlite.ouvrir(f).use { cnx -> cnx.createStatement().use { it.executeUpdate("ALTER TABLE placettes DROP COLUMN etat_suivi") } }
        val t = LecteurTournee.lire(f)
        assertEquals(3, t.placettes.size)
        assertEquals(setOf(EtatSuivi.INCONNU), t.placettes.map { it.etatSuivi }.toSet())
    }

    @Test
    fun `valeur vers teinte et forme - option A de Pascal`() {
        assertEquals(Pastille(Teinte.ROUGE, FormePastille.DISQUE_PLEIN), Pastille.de(EtatSuivi.JAMAIS))
        assertEquals(Pastille(Teinte.ORANGE, FormePastille.DISQUE_PLEIN), Pastille.de(EtatSuivi.A_PROGRAMMER))
        assertEquals(Pastille(Teinte.VERT, FormePastille.ANNEAU), Pastille.de(EtatSuivi.A_JOUR))
        assertEquals(Pastille(Teinte.NOIR, FormePastille.DISQUE_AVEC_POINT_INTERROGATION), Pastille.de(EtatSuivi.INCONNU))
    }

    @Test
    fun `quatre etats, quatre pastilles toutes distinctes, quatre teintes au plus`() {
        val pastilles = EtatSuivi.entries.map(Pastille::de)
        assertEquals(pastilles.size, pastilles.toSet().size)
        assertEquals(4, pastilles.map { it.teinte }.toSet().size)
        assertNotEquals(Pastille.de(EtatSuivi.JAMAIS).forme, Pastille.de(EtatSuivi.A_JOUR).forme)
    }

    @Test
    fun `les valeurs de sommieR se reconnaissent exactement, rien d autre`() {
        assertEquals(EtatSuivi.JAMAIS, EtatSuivi.depuis("jamais"))
        assertEquals(EtatSuivi.A_JOUR, EtatSuivi.depuis("a_jour"))
        assertEquals(EtatSuivi.A_PROGRAMMER, EtatSuivi.depuis("a_programmer"))
        assertEquals(EtatSuivi.INCONNU, EtatSuivi.depuis("A_JOUR"))
        assertEquals(EtatSuivi.INCONNU, EtatSuivi.depuis(""))
        assertEquals(EtatSuivi.INCONNU, EtatSuivi.depuis(null))
    }

    @Test
    fun `les decomptes de la legende comptent aussi les placettes inconnues`() {
        val d = decomptes(LecteurTournee.lire(tourneeAvecEtats()).placettes)
        assertEquals(mapOf(EtatSuivi.JAMAIS to 1, EtatSuivi.A_PROGRAMMER to 1, EtatSuivi.A_JOUR to 1, EtatSuivi.INCONNU to 2), d)
        assertEquals(0, decomptes(emptyList()).values.sum())
    }
}
