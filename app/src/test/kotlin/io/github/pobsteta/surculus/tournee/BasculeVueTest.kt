/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.tournee

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 050 : le bouton liste/colonnes est toujours visible, sans case « Vue Kanban » (décision de Pascal, 2026-10-27).
 * Comme `TypographieTest`, ces tests lisent les sources (le répertoire de travail des tests unitaires est `app/`).
 */
class BasculeVueTest {
    private fun source(chemin: String) = File("src/main/kotlin/io/github/pobsteta/surculus/$chemin").readText()

    private class Memoire : MagasinKanban {
        val textes = mutableMapOf<String, String>()
        val booleens = mutableMapOf<String, Boolean>()
        override fun lireTexte(cle: String) = textes[cle]
        override fun ecrireTexte(cle: String, valeur: String) { textes[cle] = valeur }
        override fun lireBooleen(cle: String, parDefaut: Boolean) = booleens[cle] ?: parDefaut
        override fun ecrireBooleen(cle: String, valeur: Boolean) { booleens[cle] = valeur }
    }

    @Test fun `les sources sont trouvees`() {
        assertTrue(source("tournee/EcranTournees.kt").contains("BandeauAccueil"))
    }

    @Test fun `le bouton de bascule ne depend plus d'aucun reglage`() {
        val s = source("tournee/EcranTournees.kt")
        assertFalse(s.contains("kanbanActif"), "le bouton ne doit plus dépendre de kanbanActif")
        assertFalse(s.contains("vueActive"), "la liste ne lit plus le réglage d'activation")
        assertTrue(s.contains("onBasculerVue"), "le bouton de bascule reste dans la barre")
    }

    @Test fun `la liste reste la vue d'ouverture`() {
        assertTrue(Regex("""modeColonnes by rememberSaveable \{ mutableStateOf\(false\) \}""").containsMatchIn(source("tournee/EcranTournees.kt")))
    }

    @Test fun `Parametres n'a plus de case Vue Kanban`() {
        assertFalse(source("parametres/EcranParametres.kt").contains("Kanban"))
        val chaines = File("src/main/res/values/strings_tournees.xml").readText()
        assertFalse(chaines.contains("param_kanban"), "chaînes de la case retirées")
    }

    @Test fun `les statuts gardes restent lus, meme avec l'ancienne cle d'activation`() {
        val magasin = Memoire()
        magasin.ecrireTexte("statut_t1", EtatKanban.VALIDEE.name)
        magasin.ecrireBooleen("vue_kanban", false) // reste d'avant la mission 050 : ignoré
        val r = ReglagesKanban(magasin)
        assertEquals(EtatKanban.VALIDEE, r.statut("t1"))
        assertEquals(EtatKanban.PROPOSEE, r.statut("t2"))
        r.definirStatut("t2", EtatKanban.REALISEE)
        assertEquals(EtatKanban.REALISEE, ReglagesKanban(magasin).statut("t2"))
    }
}
