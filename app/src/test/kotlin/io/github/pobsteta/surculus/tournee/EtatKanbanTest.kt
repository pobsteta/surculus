/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.Projection
import io.github.pobsteta.surculus.data.journal.TypeLigne
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class EtatKanbanTest {
    private class Memoire : MagasinKanban {
        val textes = mutableMapOf<String, String>()
        val booleens = mutableMapOf<String, Boolean>()
        override fun lireTexte(cle: String) = textes[cle]
        override fun ecrireTexte(cle: String, valeur: String) { textes[cle] = valeur }
        override fun lireBooleen(cle: String, parDefaut: Boolean) = booleens[cle] ?: parDefaut
        override fun ecrireBooleen(cle: String, valeur: Boolean) { booleens[cle] = valeur }
    }

    @Test fun `les cinq colonnes de Marculus, dans l'ordre, avec leurs libelles`() {
        assertEquals(
            listOf("Proposée", "Validée", "Planifiée", "Réalisée", "Abandonnée"),
            EtatKanban.entries.map { it.libelle },
        )
    }

    @Test fun `au moins une saisie donne au moins Planifiee`() {
        assertEquals(EtatKanban.PROPOSEE, statutMinimal(0, rendue = false))
        assertEquals(EtatKanban.PLANIFIEE, statutMinimal(1, rendue = false))
        assertEquals(EtatKanban.PLANIFIEE, statutMinimal(40, rendue = false))
    }

    @Test fun `rendue donne au moins Realisee, avec ou sans saisie`() {
        assertEquals(EtatKanban.REALISEE, statutMinimal(0, rendue = true))
        assertEquals(EtatKanban.REALISEE, statutMinimal(5, rendue = true))
    }

    @Test fun `colonne affichee = le plus avance du statut garde et du minimum`() {
        assertEquals(EtatKanban.PLANIFIEE, colonneAffichee(EtatKanban.PROPOSEE, EtatKanban.PLANIFIEE))
        assertEquals(EtatKanban.PLANIFIEE, colonneAffichee(EtatKanban.VALIDEE, EtatKanban.PLANIFIEE))
        assertEquals(EtatKanban.REALISEE, colonneAffichee(EtatKanban.REALISEE, EtatKanban.PLANIFIEE))
        assertEquals(EtatKanban.VALIDEE, colonneAffichee(EtatKanban.VALIDEE, EtatKanban.PROPOSEE))
        assertEquals(EtatKanban.ABANDONNEE, colonneAffichee(EtatKanban.ABANDONNEE, EtatKanban.REALISEE))
    }

    @Test fun `une carte deposee sous le minimum reste au minimum`() {
        val minimal = statutMinimal(3, rendue = false)
        assertNull(deposer(EtatKanban.PLANIFIEE, EtatKanban.PROPOSEE, minimal))
        assertNull(deposer(EtatKanban.PLANIFIEE, EtatKanban.VALIDEE, minimal))
        assertEquals(EtatKanban.PLANIFIEE, colonneAffichee(EtatKanban.PLANIFIEE, minimal))
    }

    @Test fun `un depot au minimum ou au-dessus est accepte, un depot dans la meme colonne ne change rien`() {
        assertEquals(EtatKanban.REALISEE, deposer(EtatKanban.PLANIFIEE, EtatKanban.REALISEE, EtatKanban.PLANIFIEE))
        assertEquals(EtatKanban.PLANIFIEE, deposer(EtatKanban.PROPOSEE, EtatKanban.PLANIFIEE, EtatKanban.PLANIFIEE))
        assertEquals(EtatKanban.VALIDEE, deposer(EtatKanban.PROPOSEE, EtatKanban.VALIDEE, EtatKanban.PROPOSEE))
        assertNull(deposer(EtatKanban.VALIDEE, EtatKanban.VALIDEE, EtatKanban.PROPOSEE))
    }

    @Test fun `un statut garde inconnu ou absent vaut Proposee`() {
        assertEquals(EtatKanban.PROPOSEE, EtatKanban.depuisNom(null))
        assertEquals(EtatKanban.PROPOSEE, EtatKanban.depuisNom("N_IMPORTE_QUOI"))
        assertEquals(EtatKanban.ABANDONNEE, EtatKanban.depuisNom("ABANDONNEE"))
    }

    @Test fun `recherche vide, toutes les tournees sont reparties et les cinq colonnes existent`() {
        val lignes = listOf("a", "b", "c", "d")
        val colonnes = mapOf("a" to EtatKanban.PROPOSEE, "b" to EtatKanban.PLANIFIEE, "c" to EtatKanban.PLANIFIEE, "d" to EtatKanban.ABANDONNEE)
        val r = repartir(lignes) { colonnes.getValue(it) }
        assertEquals(EtatKanban.entries.toList(), r.keys.toList())
        assertEquals(lignes.toSet(), r.values.flatten().toSet())
        assertEquals(lignes.size, r.values.sumOf { it.size })
        assertEquals(listOf("b", "c"), r.getValue(EtatKanban.PLANIFIEE))
        assertEquals(emptyList(), r.getValue(EtatKanban.VALIDEE))
    }

    @Test fun `les reglages se gardent et se relisent, par tournee`() {
        val magasin = Memoire()
        ReglagesKanban(magasin).apply {
            definirStatut("t1", EtatKanban.VALIDEE)
        }
        val relu = ReglagesKanban(magasin) // autre instance : relecture depuis le stockage
        assertEquals(EtatKanban.VALIDEE, relu.statut("t1"))
        assertEquals(EtatKanban.PROPOSEE, relu.statut("t2"))
    }

    private var n = 0L
    private fun ligne(uuid: String, objet: String, type: String, remplace: String? = null) = LigneJournal(
        uuid = uuid, objetUuid = objet, type = type, valeur = "{}", remplaceUuid = remplace, operateur = "op", appareil = "a1",
        sequence = ++n, saisiLeUtcMs = 1_000L * n, heureGnssMs = null, monotoneMs = n, demarrage = 1, ageFixMs = null, codeTournee = "T1",
    )
    private fun saisies(vararg lignes: LigneJournal) = compterSaisies(Projection.projeter(lignes.toList()).cles)

    @Test fun `aucune ligne, aucune saisie, et un saut d'horloge n'est pas une saisie`() {
        assertEquals(0, saisies())
        assertEquals(0, saisies(ligne("h1", "p1", TypeLigne.SAUT_HORLOGE)))
    }

    @Test fun `une saisie compte une fois, une par objet et par type`() {
        assertEquals(1, saisies(ligne("c1", "p1", TypeLigne.CONTROLE)))
        assertEquals(3, saisies(ligne("c1", "p1", TypeLigne.CONTROLE), ligne("c2", "p2", TypeLigne.POINTAGE), ligne("c3", "p3", TypeLigne.ETAT)))
    }

    @Test fun `une saisie puis son annulation ne comptent pour rien, donc pas de minimum Planifiee`() {
        val n0 = saisies(ligne("c1", "p1", TypeLigne.CONTROLE), ligne("a1", "p1", TypeLigne.ANNULATION, remplace = "c1"))
        assertEquals(0, n0)
        assertEquals(EtatKanban.PROPOSEE, statutMinimal(n0, rendue = false))
    }

    @Test fun `annuler l'annulation refait la saisie`() {
        assertEquals(
            1,
            saisies(
                ligne("c1", "p1", TypeLigne.CONTROLE),
                ligne("a1", "p1", TypeLigne.ANNULATION, remplace = "c1"),
                ligne("a2", "p1", TypeLigne.ANNULATION, remplace = "a1"),
            ),
        )
    }

    @Test fun `une saisie corrigee compte une fois`() {
        val n1 = saisies(ligne("c1", "p1", TypeLigne.CONTROLE), ligne("c2", "p1", TypeLigne.CONTROLE, remplace = "c1"))
        assertEquals(1, n1)
        assertEquals(EtatKanban.PLANIFIEE, statutMinimal(n1, rendue = false))
    }
}
