package io.github.pobsteta.surculus.core.session

import com.google.gson.JsonObject
import io.github.pobsteta.surculus.core.dictee.DicteeFixtures
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import io.github.pobsteta.surculus.core.session.IssueDictee.Raison
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertIs

/**
 * Mission 012-D1 : `ApplicateurDictee` (phrase vers `SessionControle`), sur le journal en mémoire.
 * Les cas de `fixtures/dictee/` sont rejoués **à travers la session** sans être modifiés.
 */
class ApplicateurDicteeTest {

    /** Une session avec [n] plants pointés ; le premier est le courant. */
    private fun bancAvec(n: Int = 1): Pair<BancSession, SessionControle> {
        val b = BancSession()
        val s = b.session()
        repeat(n) { k -> assertIs<SuiteSession.Fait>(with(b) { s.pointe(1.0 + k, 10.0 + 40.0 * k) }) }
        if (n > 0) s.choisir(s.etat().plants.first().uuid)
        return b to s
    }

    private fun SessionControle.premier() = etat().plants.first()

    private fun SessionControle.observation() = premier().duJour

    private fun JsonObject.texte(cle: String): String? = get(cle)?.takeUnless { it.isJsonNull }?.asString

    /** Met le plant courant dans l'état de la fixture (état et mesures dites juste avant). */
    private fun SessionControle.poser(etat: String?, mesures: JsonObject? = null) {
        when (etat) {
            null -> Unit
            "non_observe" -> assertIs<SuiteSession.Fait>(nonObserve())
            else -> assertIs<SuiteSession.Fait>(
                dire(etat, mesures?.texte("cause_groupe"), null, mesures?.get("hauteur_cm")?.takeUnless { it.isJsonNull }?.asInt),
            )
        }
    }

    private fun lignesEtat(b: BancSession) = b.journal.deType(FormatValeurs.TYPE_ETAT)

    // ------------------------------------------------------------------ rejeu des fixtures

    @TestFactory
    fun `contexte - chaque cas de fixtures-dictee rejoue par la session`() = DicteeFixtures.cas("contexte").map { c ->
        dynamicTest("${c.id} ${c.nom}") {
            val (b, s) = bancAvec()
            val e = c.entree
            s.poser(e.texte("etat_courant"), e.getAsJsonObject("mesures_courantes"))
            val avant = b.journal.lignes.size
            val avantObs = s.observation()
            val issue = ApplicateurDictee.appliquer(e.get("phrase").asString, s)
            val a = c.attendu
            val msg = "${c.id} (source : ${c.source})"
            assertEquals(a["accepte"].asBoolean, issue !is IssueDictee.Refusee, msg)
            if (issue is IssueDictee.Refusee) {
                assertEquals(avant, b.journal.lignes.size, "$msg : un refus n'écrit rien")
                assertEquals(avantObs, s.observation(), msg)
                return@dynamicTest
            }
            a.get("correction")?.let { assertEquals(it.asBoolean, (issue as? IssueDictee.Ecrite)?.correction ?: false, "$msg : correction") }
            a.get("annonce_vocale")?.let { assertEquals(it.asBoolean, issue.annonce.startsWith("Correction."), "$msg : annonce de la correction") }
            if (a.get("ligne_ajoutee")?.asBoolean == false) {
                assertIs<IssueDictee.Redite>(issue, msg)
                assertEquals(avant, b.journal.lignes.size, "$msg : une redite n'écrit aucune ligne")
                assertEquals(avantObs, s.observation(), "$msg : les mesures sont conservées")
                assertTrue(a["confirmation_vocale"].asBoolean)
            } else if (a.get("ligne_ajoutee")?.asBoolean == true) {
                assertEquals(avant + 1, b.journal.lignes.size, "$msg : une ligne ajoutée")
            }
            if (a.get("mesures_conservees")?.asBoolean == false) assertEquals(Observation.NonObserve, s.observation(), "$msg : plus de mesure")
        }
    }

    @TestFactory
    fun `hauteur - les 36 cas de fixtures-dictee sur un plant atteint`() = DicteeFixtures.cas("hauteur").map { c ->
        dynamicTest("${c.id} ${c.nom}") {
            val (b, s) = bancAvec()
            s.poser("atteint")
            val avant = b.journal.lignes.size
            val issue = ApplicateurDictee.appliquer(c.entree.get("phrase").asString, s)
            if (c.attendu["accepte"].asBoolean) {
                assertIs<IssueDictee.Ecrite>(issue, c.id)
                val cm = c.attendu.getAsJsonObject("commande")["hauteur_cm"].asInt
                assertEquals(cm, (s.observation() as Observation.Observe).hauteurCm, c.id)
                assertEquals(avant + 1, b.journal.lignes.size, c.id)
            } else {
                assertIs<IssueDictee.Refusee>(issue, c.id)
                assertEquals(avant, b.journal.lignes.size, c.id)
            }
        }
    }

    @TestFactory
    fun `refus - les phrases hors grammaire ne touchent pas au journal et se disent`() = DicteeFixtures.cas("refus").map { c ->
        dynamicTest("${c.id} ${c.nom}") {
            val (b, s) = bancAvec()
            s.poser("atteint")
            val avant = b.journal.lignes.size
            val issue = ApplicateurDictee.appliquer(c.entree.get("phrase").asString, s)
            assertIs<IssueDictee.Refusee>(issue, c.id)
            assertTrue(issue.annonce.isNotBlank(), "${c.id} : chaque refus se dit")
            assertEquals(avant, b.journal.lignes.size, c.id)
        }
    }

    // ------------------------------------------------------------------ textes exacts des refus

    private fun refus(etat: String?, phrase: String, raison: Raison, texte: String) {
        val (b, s) = bancAvec()
        s.poser(etat)
        val avant = b.journal.lignes.size
        val issue = ApplicateurDictee.appliquer(phrase, s)
        val r = assertIs<IssueDictee.Refusee>(issue, "« $phrase » sur $etat")
        assertEquals(raison, r.raison, "« $phrase » sur $etat")
        assertEquals(texte, r.annonce, "« $phrase » sur $etat")
        assertEquals(avant, b.journal.lignes.size)
        assertFalse(r.ecrit)
    }

    @Test
    fun `le texte exact de chaque refus`() {
        refus(null, "banane", Raison.PAS_COMPRIS, "Pas compris.")
        refus(null, "atteint insectes", Raison.PAS_COMPRIS, "Pas compris.")
        refus(null, "", Raison.PAS_COMPRIS, "Pas compris.")
        refus(null, "hauteur quarante-cinq", Raison.HAUTEUR_SANS_ETAT, "Hauteur refusée. Dites d'abord l'état.")
        refus("absent", "hauteur quarante-cinq", Raison.HAUTEUR_SUR_ABSENT, "Pas de hauteur sur un plant absent.")
        refus("non_observe", "hauteur quarante-cinq", Raison.HAUTEUR_SUR_NON_OBSERVE, "Pas de hauteur sur un plant non observé.")
        refus(null, "hauteur cinq cents", Raison.HAUTEUR_SANS_ETAT, "Hauteur refusée. Dites d'abord l'état.")
        refus(null, "hauteur zéro", Raison.HAUTEUR_SANS_ETAT, "Hauteur refusée. Dites d'abord l'état.")
        refus("atteint", "hauteur zéro", Raison.HAUTEUR_HORS_BORNES, "Hauteur de 1 à 400 centimètres.")
        refus("atteint", "hauteur quatre cent un", Raison.HAUTEUR_HORS_BORNES, "Hauteur de 1 à 400 centimètres.")
        refus("atteint", "hauteur", Raison.HAUTEUR_HORS_BORNES, "Hauteur de 1 à 400 centimètres.")
        refus("atteint", "vide", Raison.VIDE, "Vide : pas encore disponible.")
        refus("atteint", "fin du vide", Raison.VIDE, "Vide : pas encore disponible.")
        refus("sain", "insectes", Raison.CAUSE_SUR_SAIN, "Une cause demande un plant atteint ou mort.")
        refus("absent", "insectes", Raison.CAUSE_SUR_ABSENT, "Pas de cause sur un plant absent.")
        refus("non_observe", "climat", Raison.CAUSE_SUR_NON_OBSERVE, "Pas de cause sur un plant non observé.")
        refus(null, "champignon", Raison.ETAT_D_ABORD, "Dites d'abord l'état.")
        refus("absent", "mangé", Raison.DETAIL_REFUSE, "Mangé : plant sain, atteint ou mort seulement.")
        refus(null, "mangé", Raison.DETAIL_REFUSE, "Mangé : plant sain, atteint ou mort seulement.")
        refus("sain", "remplace", Raison.REMPLACE_REFUSE, "Remplace : plant mort ou absent seulement.")
    }

    @Test
    fun `rien a annuler et plus de plant a dire se disent`() {
        val (b, s) = bancAvec(0)
        val rien = assertIs<IssueDictee.Refusee>(ApplicateurDictee.appliquer("annule", s))
        assertEquals(Raison.RIEN_A_ANNULER, rien.raison)
        assertEquals("Rien à annuler.", rien.annonce)
        val aucun = assertIs<IssueDictee.Refusee>(ApplicateurDictee.appliquer("atteint", s))
        assertEquals(Raison.AUCUN_PLANT, aucun.raison)
        assertEquals("Plus de plant à dire.", aucun.annonce)
        assertEquals("Plus de plant à dire.", ApplicateurDictee.appliquer("hauteur zéro", s).annonce)
        assertEquals("Plus de plant à dire.", ApplicateurDictee.appliquer("hauteur", s).annonce)
        assertEquals(0, b.journal.lignes.size)
    }

    // ------------------------------------------------------------------ « hauteur » : sans état, puis sur un plant mort

    @Test
    fun `hauteur sur un plant sans etat est refusee, une fois mort elle est acceptee`() {
        val (b, s) = bancAvec()
        val refus = assertIs<IssueDictee.Refusee>(ApplicateurDictee.appliquer("hauteur quarante-cinq", s))
        assertEquals("Hauteur refusée. Dites d'abord l'état.", refus.annonce)
        assertEquals(0, lignesEtat(b).size)
        assertIs<IssueDictee.Ecrite>(ApplicateurDictee.appliquer("mort", s))
        val ok = assertIs<IssueDictee.Ecrite>(ApplicateurDictee.appliquer("hauteur quarante-cinq", s))
        assertEquals("Numéro 1, hauteur 45 centimètres.", ok.annonce)
        val o = assertIs<Observation.Observe>(s.observation())
        assertEquals(45, o.hauteurCm)
        assertEquals("mort", o.etat.code)
    }

    // ------------------------------------------------------------------ annonces, Q4

    @Test
    fun `une phrase d'etat reste sur le plant courant, seul suivant avance`() {
        val (_, s) = bancAvec(3)
        val courant = s.etat().courant
        assertEquals("Numéro 1, atteint.", ApplicateurDictee.appliquer("atteint", s).annonce)
        assertEquals("Numéro 1, insectes.", ApplicateurDictee.appliquer("insectes", s).annonce)
        assertEquals("Numéro 1, mangé.", ApplicateurDictee.appliquer("mangé", s).annonce)
        assertEquals("Numéro 1, hauteur 45 centimètres.", ApplicateurDictee.appliquer("hauteur quarante-cinq", s).annonce)
        assertEquals(courant, s.etat().courant)
        assertEquals("Numéro 2.", ApplicateurDictee.appliquer("suivant", s).annonce)
        assertEquals(s.etat().plants[1].uuid, s.etat().courant)
        ApplicateurDictee.appliquer("sain", s)
        val fin = ApplicateurDictee.appliquer("suivant", s) // 3e
        assertEquals("Numéro 3.", fin.annonce)
        ApplicateurDictee.appliquer("mort", s)
        val plus = assertIs<IssueDictee.Suivant>(ApplicateurDictee.appliquer("suivant", s))
        assertNull(plus.numero)
        assertEquals("Tous les plants sont dits.", plus.annonce)
    }

    @Test
    fun `climat et indetermine se disent autre ou inconnu, champignons et animaux gardent leur mot`() {
        val (_, s) = bancAvec()
        ApplicateurDictee.appliquer("atteint", s)
        assertEquals("Numéro 1, autre ou inconnu.", ApplicateurDictee.appliquer("climat", s).annonce)
        assertEquals("Numéro 1, autre ou inconnu.", ApplicateurDictee.appliquer("indéterminé", s).annonce)
        assertEquals("Numéro 1, champignons.", ApplicateurDictee.appliquer("champignon", s).annonce)
        assertEquals("Numéro 1, animaux.", ApplicateurDictee.appliquer("animaux", s).annonce)
    }

    @Test
    fun `non observe sans mesure, remplace en attente, annule`() {
        val (b, s) = bancAvec()
        assertEquals("Numéro 1, non observé.", ApplicateurDictee.appliquer("non observé", s).annonce)
        val a = assertIs<IssueDictee.Annulee>(ApplicateurDictee.appliquer("annule", s))
        assertEquals("Annulé.", a.annonce)
        assertTrue(a.ecrit)
        assertNull(s.observation())
        ApplicateurDictee.appliquer("mort", s)
        val avant = b.journal.lignes.size
        val r = assertIs<IssueDictee.RemplaceEnAttente>(ApplicateurDictee.appliquer("remplace", s))
        assertEquals("Numéro 1 sera remplacé. Pointez le regarni à l'écran.", r.annonce)
        assertEquals(avant, b.journal.lignes.size, "« remplace » n'écrit pas de ligne")
        assertEquals(s.premier().uuid, s.etat().regarniDe)
    }

    @Test
    fun `vide ne trace rien et ne change pas de plant`() {
        val (b, s) = bancAvec(2)
        val avant = b.journal.lignes.size
        val courant = s.etat().courant
        assertIs<IssueDictee.Refusee>(ApplicateurDictee.appliquer("vide", s))
        assertIs<IssueDictee.Refusee>(ApplicateurDictee.appliquer("fin du vide", s))
        assertEquals(avant, b.journal.lignes.size)
        assertEquals(courant, s.etat().courant)
    }

    // ------------------------------------------------------------------ redite

    @Test
    fun `la redite n'ecrit pas de ligne et garde les mesures`() {
        val (b, s) = bancAvec()
        ApplicateurDictee.appliquer("atteint", s)
        ApplicateurDictee.appliquer("insectes", s)
        ApplicateurDictee.appliquer("hauteur quarante-cinq", s)
        val avant = b.journal.lignes.size
        val obs = s.observation()
        val r = assertIs<IssueDictee.Redite>(ApplicateurDictee.appliquer("atteint", s))
        assertEquals("Numéro 1, déjà atteint.", r.annonce)
        assertEquals(avant, b.journal.lignes.size, "journal inchangé après une redite")
        assertEquals(obs, s.observation())
        assertFalse(r.ecrit)
        ApplicateurDictee.appliquer("annule", s)
        ApplicateurDictee.appliquer("annule", s)
        ApplicateurDictee.appliquer("annule", s)
        ApplicateurDictee.appliquer("non observé", s)
        assertEquals("Numéro 1, déjà non observé.", ApplicateurDictee.appliquer("non observé", s).annonce)
    }

    // ------------------------------------------------------------------ correction, Q5

    private fun corrigee(avant: List<String>, phrase: String): Triple<IssueDictee.Ecrite, Observation?, Pair<BancSession, SessionControle>> {
        val (b, s) = bancAvec()
        avant.forEach { ApplicateurDictee.appliquer(it, s) }
        val ancienne = lignesEtat(b).last()
        val issue = assertIs<IssueDictee.Ecrite>(ApplicateurDictee.appliquer(phrase, s))
        val nouvelle = lignesEtat(b).last()
        assertEquals(ancienne.uuid, nouvelle.remplaceUuid, "la nouvelle ligne remplace l'ancienne (remplace_uuid)")
        assertTrue(issue.correction)
        return Triple(issue, s.observation(), b to s)
    }

    private val tout = listOf("atteint", "insectes", "mangé", "hauteur quarante-cinq")

    @Test
    fun `correction vers mort - cause, detail et hauteur sont gardes`() {
        val (issue, obs, _) = corrigee(tout, "mort")
        assertEquals("Correction. Numéro 1, mort.", issue.annonce)
        assertTrue(issue.retire.isEmpty())
        val o = assertIs<Observation.Observe>(obs)
        assertEquals("mort", o.etat.code)
        assertEquals("entomologique", o.causeGroupe?.code)
        assertEquals("abroutissement", o.causeDetail)
        assertEquals(45, o.hauteurCm)
    }

    @Test
    fun `correction vers sain - la cause est retiree et dite, le detail abroutissement et la hauteur sont gardes`() {
        val (issue, obs, _) = corrigee(tout, "sain")
        assertEquals("Correction. Numéro 1, sain. Retiré : cause.", issue.annonce)
        assertEquals(listOf("cause"), issue.retire)
        val o = assertIs<Observation.Observe>(obs)
        assertEquals("sain", o.etat.code)
        assertNull(o.causeGroupe)
        assertEquals("abroutissement", o.causeDetail)
        assertEquals(45, o.hauteurCm)
    }

    @Test
    fun `correction vers absent - tout est retire et dit`() {
        val (issue, obs, _) = corrigee(tout, "absent")
        assertEquals("Correction. Numéro 1, absent. Retiré : cause, mangé, hauteur.", issue.annonce)
        val o = assertIs<Observation.Observe>(obs)
        assertEquals("absent", o.etat.code)
        assertNull(o.causeGroupe)
        assertNull(o.causeDetail)
        assertNull(o.hauteurCm)
    }

    @Test
    fun `correction vers non observe - sans mesure`() {
        val (issue, obs, _) = corrigee(tout, "non observé")
        assertEquals("Correction. Numéro 1, non observé, sans mesure.", issue.annonce)
        assertEquals(Observation.NonObserve, obs)
    }

    @Test
    fun `correction sans mesure a retirer - rien n'est dit en plus`() {
        val (issue, _, _) = corrigee(listOf("sain"), "atteint")
        assertEquals("Correction. Numéro 1, atteint.", issue.annonce)
    }

    @Test
    fun `une correction ecrit une seule ligne`() {
        val (b, s) = bancAvec()
        tout.forEach { ApplicateurDictee.appliquer(it, s) }
        val avant = b.journal.lignes.size
        ApplicateurDictee.appliquer("mort", s)
        assertEquals(avant + 1, b.journal.lignes.size)
    }

    // ------------------------------------------------------------------ placette terminée, Q9

    @Test
    fun `placette terminee - la phrase s'ecrit, avec l'alerte avant l'annonce`() {
        val (b, s) = bancAvec()
        ApplicateurDictee.appliquer("atteint", s)
        assertIs<SuiteSession.Fait>(s.terminer())
        val avant = b.journal.lignes.size
        val issue = assertIs<IssueDictee.Ecrite>(ApplicateurDictee.appliquer("mort", s))
        assertTrue(issue.placetteTerminee)
        assertEquals("Placette déjà terminée. Correction. Numéro 1, mort.", issue.annonce)
        assertEquals(avant + 1, b.journal.lignes.size, "écrite, non refusée")
        assertEquals("mort", (s.observation() as Observation.Observe).etat.code)
        ApplicateurDictee.appliquer("sain", s)
        val refus = assertIs<IssueDictee.Refusee>(ApplicateurDictee.appliquer("remplace", s))
        assertEquals("Placette déjà terminée. Remplace : plant mort ou absent seulement.", refus.annonce)
        // Rien de compris : pas d'alerte.
        assertEquals("Pas compris.", ApplicateurDictee.appliquer("banane", s).annonce)
    }

    @Test
    fun `une placette non terminee n'a pas d'alerte`() {
        val (_, s) = bancAvec()
        val issue = ApplicateurDictee.appliquer("atteint", s)
        assertFalse(issue.placetteTerminee)
        assertEquals("Numéro 1, atteint.", issue.annonce)
    }

    // ------------------------------------------------------------------ coordonnées : la placette en Somalie

    @Test
    fun `la placette en Somalie ne pointe rien, aucune dictee n'a de session ou ecrire`() = with(BancSession()) {
        // (latitude 5,15 N ; longitude 46,2 E) : la position française (46,2 N ; 5,15 E) aux axes inversés.
        val somalie = PointGeographique.de(latitudeDeg = 5.15, longitudeDeg = 46.2).valeurOuEchec()
        val r = assertIs<OuvertureControle.Refusee>(SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Gnss(somalie)))
        assertEquals("hors_france", r.refus.code)
        assertEquals(0, journal.lignes.size, "un fix hors de France ne pointe rien, même par la dictée")
    }

    @Test
    fun `la dictee ne deplace jamais un plant - sa position en Lambert-93 est celle du pointage`() {
        val (_, s) = bancAvec()
        val avant = s.premier().position
        ApplicateurDictee.appliquer("atteint", s)
        ApplicateurDictee.appliquer("hauteur quarante-cinq", s)
        assertEquals(avant, s.premier().position)
    }
}
