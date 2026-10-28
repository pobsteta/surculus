/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.session.EffetEcoute.Appliquer
import io.github.pobsteta.surculus.core.session.EffetEcoute.DireUnInstant
import io.github.pobsteta.surculus.core.session.EffetEcoute.Fermer
import io.github.pobsteta.surculus.core.session.EffetEcoute.Ouvrir
import io.github.pobsteta.surculus.core.session.EffetEcoute.VibrerFermeture
import io.github.pobsteta.surculus.core.session.EffetEcoute.VibrerOuverture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-D2 : la machine d'écoute « tenir pour parler » (plan § 3 et § 8, décisions de Pascal du 2026-10-28), en fonction pure. */
class MachineEcouteTest {
    /** Ouvre l'écoute à l'instant 0 et rend la machine ouverte à [pretA]. */
    private fun ouverte(pretA: Long = 100): MachineEcoute {
        val m = MachineEcoute()
        val e = m.tenir(0)
        assertEquals(listOf<EffetEcoute>(Ouvrir(m.generation)), e)
        assertEquals(listOf<EffetEcoute>(VibrerOuverture), m.moteurPret(m.generation, pretA))
        assertEquals(EtatEcoute.OUVERTE, m.etat)
        return m
    }

    // --- ouverture et fermeture

    @Test fun `tenir ouvre, le moteur pret fait vibrer l'ouverture`() {
        val m = MachineEcoute()
        assertEquals(EtatEcoute.FERMEE, m.etat)
        m.tenir(0)
        assertEquals(EtatEcoute.OUVERTURE, m.etat)
        assertEquals(listOf<EffetEcoute>(VibrerOuverture), m.moteurPret(m.generation, 300))
        assertEquals(EtatEcoute.OUVERTE, m.etat)
    }

    @Test fun `relacher ferme le micro et attend le resultat final`() {
        val m = ouverte()
        assertEquals(listOf<EffetEcoute>(Fermer, VibrerFermeture), m.relacher(2_000))
        assertEquals(EtatEcoute.FERMEE, m.etat)
        // Vosk rend son résultat final à la fermeture : la phrase est prise.
        assertEquals(listOf<EffetEcoute>(Appliquer("atteint")), m.phrase("atteint", 2_050, voixDisponible = true))
    }

    @Test fun `une phrase qui n'etait pas attendue est ignoree - ecoute fermee`() {
        val m = MachineEcoute()
        assertEquals(emptyList(), m.phrase("atteint", 0, true))
        val o = ouverte()
        o.relacher(1_000)
        o.phrase("atteint", 1_010, false)
        assertEquals(emptyList(), o.phrase("insectes", 1_020, false), "un seul résultat final par fermeture")
    }

    @Test fun `le resultat final qui ne vient pas n'attend pas toujours`() {
        val m = ouverte()
        m.relacher(1_000)
        assertEquals(1_000 + DelaisEcoute.RESULTAT_FINAL_MS, m.echeance())
        m.avancer(1_000 + DelaisEcoute.RESULTAT_FINAL_MS, voixParle = false)
        assertNull(m.echeance())
        assertEquals(emptyList(), m.phrase("atteint", 7_000, true))
    }

    @Test fun `relache pendant l'ouverture - le micro se ferme des qu'il est pret, sans vibrer`() {
        val m = MachineEcoute()
        m.tenir(0)
        assertEquals(emptyList(), m.relacher(100))
        assertEquals(listOf<EffetEcoute>(Fermer), m.moteurPret(m.generation, 900))
        assertEquals(EtatEcoute.FERMEE, m.etat)
    }

    @Test fun `un appui repete ne rouvre rien`() {
        val m = ouverte()
        assertEquals(emptyList(), m.tenir(500))
        assertEquals(EtatEcoute.OUVERTE, m.etat)
    }

    @Test fun `un moteur pret d'une ouverture perimee est ignore`() {
        val m = MachineEcoute()
        m.tenir(0)
        val vieille = m.generation
        m.arretForce()
        m.tenir(1_000)
        assertTrue(m.generation > vieille)
        assertEquals(emptyList(), m.moteurPret(vieille, 1_100))
        assertEquals(EtatEcoute.OUVERTURE, m.etat)
    }

    // --- un instant

    @Test fun `un instant - une seule fois, apres 2 s de chargement`() {
        val m = MachineEcoute()
        m.tenir(0)
        assertEquals(2_000L, m.echeance())
        assertEquals(emptyList(), m.avancer(1_999, false))
        assertEquals(listOf<EffetEcoute>(DireUnInstant), m.avancer(2_000, false))
        assertEquals(emptyList(), m.avancer(3_000, false))
    }

    // --- sortie du mode poche, onStop, erreur : fermée, jamais rouverte seule

    @Test fun `arret force - ferme tout, sans reouverture`() {
        val m = ouverte()
        assertEquals(listOf<EffetEcoute>(Fermer), m.arretForce())
        assertEquals(EtatEcoute.FERMEE, m.etat)
        assertFalse(m.tenue)
        assertEquals(emptyList(), m.avancer(60_000, false))
        assertEquals(emptyList(), m.annonceTerminee(61_000))
        assertEquals(EtatEcoute.FERMEE, m.etat)
        assertEquals(emptyList(), m.phrase("atteint", 61_100, true), "le résultat d'une écoute fermée de force n'est pas pris")
    }

    @Test fun `arret force sur une machine fermee - rien a faire`() {
        assertEquals(emptyList(), MachineEcoute().arretForce())
    }

    @Test fun `erreur du moteur - fermee, jamais rouverte seule, meme touche tenue`() {
        val m = ouverte()
        assertEquals(listOf<EffetEcoute>(Fermer), m.erreur(500))
        assertEquals(EtatEcoute.FERMEE, m.etat)
        assertEquals(emptyList(), m.avancer(60_000, false))
        assertEquals(emptyList(), m.tenir(700), "la touche est encore tenue : pas un nouvel appui")
        assertEquals(emptyList(), m.relacher(900))
        // Un nouvel appui, lui, rouvre.
        val e = m.tenir(2_000)
        assertEquals(listOf<EffetEcoute>(Ouvrir(m.generation)), e)
    }

    @Test fun `erreur pendant l'ouverture`() {
        val m = MachineEcoute()
        m.tenir(0)
        assertEquals(listOf<EffetEcoute>(Fermer), m.erreur(50))
        assertEquals(EtatEcoute.FERMEE, m.etat)
    }

    // --- anti-larsen : micro coupé pendant une annonce

    @Test fun `une phrase avec voix - le micro est coupe tout de suite, rouvert a la fin de l'annonce si la touche est tenue`() {
        val m = ouverte()
        assertEquals(listOf<EffetEcoute>(Fermer, Appliquer("atteint")), m.phrase("atteint", 1_500, voixDisponible = true))
        assertEquals(EtatEcoute.SUSPENDUE, m.etat)
        assertTrue(m.tenue)
        val g = m.generation
        assertEquals(listOf<EffetEcoute>(Ouvrir(g + 1)), m.annonceTerminee(3_000))
        assertEquals(EtatEcoute.OUVERTURE, m.etat)
        assertEquals(listOf<EffetEcoute>(VibrerOuverture), m.moteurPret(g + 1, 3_100))
        assertEquals(EtatEcoute.OUVERTE, m.etat)
    }

    @Test fun `une phrase sans voix francaise - le micro reste ouvert`() {
        val m = ouverte()
        assertEquals(listOf<EffetEcoute>(Appliquer("atteint")), m.phrase("atteint", 1_500, voixDisponible = false))
        assertEquals(EtatEcoute.OUVERTE, m.etat)
    }

    @Test fun `touche relachee pendant l'annonce - elle finit, le micro ne se rouvre pas`() {
        val m = ouverte()
        m.phrase("atteint", 1_500, true)
        assertEquals(emptyList(), m.relacher(1_800))
        assertEquals(EtatEcoute.FERMEE, m.etat)
        assertEquals(emptyList(), m.annonceTerminee(3_000))
        assertEquals(EtatEcoute.FERMEE, m.etat)
    }

    @Test fun `garde-fou de 5 s - la fin d'annonce n'est jamais venue, le micro se rouvre si la touche est tenue`() {
        val m = ouverte()
        m.phrase("atteint", 1_500, true)
        assertEquals(1_500 + DelaisEcoute.GARDE_FOU_ANNONCE_MS, m.echeance())
        assertEquals(emptyList(), m.avancer(6_499, voixParle = false))
        val e = m.avancer(6_500, voixParle = false)
        assertEquals(listOf<EffetEcoute>(Ouvrir(m.generation)), e)
        assertEquals(EtatEcoute.OUVERTURE, m.etat)
    }

    @Test fun `garde-fou - si la voix parle encore, on attend (jamais de larsen sur une longue annonce)`() {
        val m = ouverte()
        m.phrase("atteint", 1_500, true)
        assertEquals(emptyList(), m.avancer(6_500, voixParle = true))
        assertEquals(EtatEcoute.SUSPENDUE, m.etat)
        assertEquals(emptyList(), m.avancer(6_600, voixParle = true))
        val e = m.avancer(11_600, voixParle = false)
        assertEquals(listOf<EffetEcoute>(Ouvrir(m.generation)), e)
    }

    @Test fun `garde-fou - touche relachee, il referme simplement`() {
        val m = ouverte()
        m.phrase("atteint", 1_500, true)
        m.relacher(2_000)
        assertEquals(emptyList(), m.avancer(6_500, false))
        assertEquals(EtatEcoute.FERMEE, m.etat)
        assertNull(m.echeance())
    }

    @Test fun `une annonce de geste pendant l'ecoute coupe le micro, puis le rouvre`() {
        val m = ouverte()
        assertEquals(listOf<EffetEcoute>(Fermer), m.annonceDebut(1_000))
        assertEquals(EtatEcoute.SUSPENDUE, m.etat)
        assertEquals(emptyList(), m.annonceDebut(1_200), "une deuxième annonce ne coupe pas deux fois")
        val e = m.annonceTerminee(2_000)
        assertEquals(listOf<EffetEcoute>(Ouvrir(m.generation)), e)
    }

    @Test fun `une annonce quand l'ecoute est fermee ne touche pas au micro`() {
        val m = MachineEcoute()
        assertEquals(emptyList(), m.annonceDebut(0))
        assertEquals(emptyList(), m.annonceTerminee(100))
        assertEquals(EtatEcoute.FERMEE, m.etat)
    }

    @Test fun `une annonce pendant l'ouverture coupe aussi le micro qui s'ouvre`() {
        val m = MachineEcoute()
        m.tenir(0)
        assertEquals(listOf<EffetEcoute>(Fermer), m.annonceDebut(500))
        assertEquals(emptyList(), m.moteurPret(m.generation, 900), "le moteur prêt arrive après la coupure : sans effet")
        assertEquals(EtatEcoute.SUSPENDUE, m.etat)
    }

    // --- bruit et [unk]

    @Test fun `unk dans la seconde qui suit l'ouverture - ignore, sans refus`() {
        val m = ouverte(pretA = 1_000)
        assertEquals(emptyList(), m.phrase("[unk]", 1_999, true))
        assertEquals(EtatEcoute.OUVERTE, m.etat, "ignoré : le micro reste ouvert, aucune annonce")
    }

    @Test fun `unk apres la seconde - transmis, l'applicateur dira Pas compris`() {
        val m = ouverte(pretA = 1_000)
        assertEquals(listOf<EffetEcoute>(Fermer, Appliquer("[unk]")), m.phrase("[unk]", 2_000, true))
    }

    @Test fun `un texte vide dans la seconde est ignore aussi`() {
        val m = ouverte(pretA = 0)
        assertEquals(emptyList(), m.phrase("  ", 500, true))
    }

    @Test fun `une vraie phrase dans la seconde qui suit l'ouverture est prise`() {
        val m = ouverte(pretA = 0)
        assertEquals(listOf<EffetEcoute>(Fermer, Appliquer("atteint")), m.phrase("atteint", 300, true))
    }

    @Test fun `la seconde de bruit repart a chaque ouverture, y compris la reouverture apres une annonce`() {
        val m = ouverte(pretA = 0)
        m.phrase("atteint", 500, true)
        val g = m.generation
        m.annonceTerminee(2_000)
        m.moteurPret(g + 1, 2_100)
        assertEquals(emptyList(), m.phrase("[unk]", 2_900, true))
        assertEquals(listOf<EffetEcoute>(Fermer, Appliquer("[unk]")), m.phrase("[unk]", 3_100, true))
    }

    // --- constantes nommées (décision Q11)

    @Test fun `les delais sont ceux de la decision de Pascal`() {
        assertEquals(5_000L, DelaisEcoute.GARDE_FOU_ANNONCE_MS)
        assertEquals(200L, DelaisEcoute.VIBRATION_OUVERTURE_MS)
        assertEquals(2_000L, DelaisEcoute.UN_INSTANT_MS)
        assertEquals(1_000L, DelaisEcoute.BRUIT_APRES_OUVERTURE_MS)
    }

    @Test fun `plus de delai de 10 s sans phrase - tenir pour parler`() {
        val m = ouverte()
        assertEquals(emptyList(), m.avancer(100_000, false))
        assertEquals(EtatEcoute.OUVERTE, m.etat)
        assertNull(m.echeance())
    }
}
