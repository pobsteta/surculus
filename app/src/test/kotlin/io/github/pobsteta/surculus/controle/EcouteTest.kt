package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.session.EtatEcoute
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mission 012-D2 : le pilote de l'écoute (micro, voix, vibreur, bande), la permission et les textes, sans téléphone. */
class EcouteTest {
    private class MicroFaux : PortMicro {
        val journal = ArrayList<String>()
        override fun ouvrir(generation: Int) { journal += "ouvrir#$generation" }
        override fun fermer() { journal += "fermer" }
    }

    private class VoixFausse(var francais: Boolean = true, var parleEncore: Boolean = false) : MoteurVoix {
        val dits = ArrayList<String>()
        override val francaisDisponible get() = francais
        override var surFin: (() -> Unit)? = null
        override val parle get() = parleEncore
        var coupees = 0
        override fun dire(texte: String): Boolean { dits += texte; return true }
        override fun arreter() { coupees++ }
        fun finDeLAnnonce() = surFin?.invoke()
    }

    private class VibreurFaux : Vibreur {
        val motifs = ArrayList<List<Long>>()
        override fun vibrer(motif: LongArray) { motifs += motif.toList() }
    }

    private class Banc(
        francais: Boolean = true,
        var modele: Boolean = true,
        var permission: EtatPermissionMicro = EtatPermissionMicro.ACCORDEE,
    ) {
        val micro = MicroFaux()
        val voix = VoixFausse(francais)
        val vibreur = VibreurFaux()
        val sorties = SortiesPoche(voix, vibreur)
        var t = 0L
        val phrases = ArrayList<String>()
        val pilote = PilotageEcoute(micro, sorties, { t }, { modele }, { permission }).also { it.surPhrase = { p -> phrases += p } }

        /** Le micro s'ouvre vraiment (le moteur rend la main), à l'instant [t]. */
        fun pret(g: Int = 1) = pilote.moteurPret(g)
    }

    // --- tenir pour parler

    @Test fun `tenir ouvre le micro, pret fait vibrer un coup et affiche J'ECOUTE, relacher ferme et vibre deux coups`() = with(Banc()) {
        assertTrue(pilote.tenir())
        assertEquals(listOf("ouvrir#1"), micro.journal)
        assertEquals(AffichageEcoute.OUVERTURE, pilote.affichage)
        assertEquals("J'ÉCOUTE…", TextesEcoute.grand(pilote.affichage))
        t = 300; pret()
        assertEquals(AffichageEcoute.ECOUTE, pilote.affichage)
        assertEquals("J'ÉCOUTE", TextesEcoute.grand(pilote.affichage))
        assertEquals(listOf(MotifsVibration.OUVERTE.toList()), vibreur.motifs)
        t = 2_000; pilote.relacher()
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
        assertEquals(AffichageEcoute.FERMEE, pilote.affichage)
        assertEquals("ÉCOUTE FERMÉE", TextesEcoute.grand(pilote.affichage))
        assertEquals(listOf(MotifsVibration.OUVERTE.toList(), MotifsVibration.FERMEE.toList()), vibreur.motifs)
    }

    @Test fun `l'ouverture coupe la voix qui parlait, sans rejouer l'annonce`() = with(Banc()) {
        pilote.tenir()
        assertEquals(1, voix.coupees)
        assertTrue(voix.dits.isEmpty())
    }

    @Test fun `sans ecran branche, rien ne s'ouvre`() = with(Banc()) {
        pilote.surPhrase = null
        assertFalse(pilote.tenir())
        assertTrue(micro.journal.isEmpty())
    }

    @Test fun `aucune ecoute avant le premier appui - la bande ne dit rien du micro`() = with(Banc()) {
        assertEquals(AffichageEcoute.RIEN, pilote.affichage)
        assertNull(TextesEcoute.grand(pilote.affichage))
    }

    // --- une phrase : micro coupé, appliquée, annoncée, micro rouvert si la touche est tenue

    @Test fun `une phrase pendant la tenue - micro coupe, phrase appliquee, annonce dite, micro rouvert a la fin de l'annonce`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        t = 2_000; pilote.phrase("atteint")
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal, "le micro est coupé avant l'annonce")
        assertEquals(listOf("atteint"), phrases)
        assertEquals("entendu : « atteint »", pilote.entendu)
        pilote.resultat(AnnoncePoche("Numéro 7, atteint.", MotifsVibration.ATTEINT))
        assertEquals(listOf("Numéro 7, atteint."), voix.dits)
        assertEquals("compris : Numéro 7, atteint.", TextesEcoute.compris(pilote.compris!!, pilote.comprisErreur))
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal, "pas de réouverture avant la fin de l'annonce")
        t = 3_500; voix.finDeLAnnonce()
        assertEquals(listOf("ouvrir#1", "fermer", "ouvrir#2"), micro.journal)
        t = 3_700; pret(2)
        assertEquals(AffichageEcoute.ECOUTE, pilote.affichage)
    }

    @Test fun `phrase puis relachement pendant l'annonce - le micro ne se rouvre pas`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        t = 1_000; pilote.phrase("insectes")
        pilote.resultat(AnnoncePoche("Numéro 7, insectes.", MotifsVibration.ATTEINT))
        t = 1_200; pilote.relacher()
        t = 2_500; voix.finDeLAnnonce()
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
    }

    @Test fun `le resultat final apres le relachement est pris et annonce, sans rouvrir`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        t = 2_000; pilote.relacher()
        t = 2_050; pilote.phrase("atteint")
        assertEquals(listOf("atteint"), phrases)
        pilote.resultat(AnnoncePoche("Numéro 7, atteint.", MotifsVibration.ATTEINT))
        t = 3_000; voix.finDeLAnnonce()
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
    }

    @Test fun `sans voix francaise - le micro reste ouvert pendant l'annonce ecrite, la vibration passe`() = with(Banc(francais = false)) {
        pilote.tenir(); t = 100; pret()
        t = 1_000; pilote.phrase("atteint")
        pilote.resultat(AnnoncePoche("Numéro 7, atteint.", MotifsVibration.ATTEINT))
        assertEquals(listOf("ouvrir#1"), micro.journal, "pas de larsen possible sans voix : le micro reste ouvert")
        assertEquals(AffichageEcoute.ECOUTE, pilote.affichage)
        assertEquals(MotifsVibration.ATTEINT.toList(), vibreur.motifs.last())
        assertEquals("Numéro 7, atteint.", pilote.compris)
    }

    @Test fun `un refus de la machine s'affiche en erreur et se dit`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        t = 1_500; pilote.phrase("hauteur")
        pilote.resultat(AnnoncePoche("Hauteur de 1 à 400 centimètres.", MotifsVibration.REFUS, erreur = true))
        assertTrue(pilote.comprisErreur)
        assertEquals("refusé : Hauteur de 1 à 400 centimètres.", TextesEcoute.compris(pilote.compris!!, pilote.comprisErreur))
        assertEquals(listOf("Hauteur de 1 à 400 centimètres."), voix.dits)
    }

    // --- bruit

    @Test fun `unk dans la seconde qui suit l'ouverture - ignore, pas de refus, pas de voix`() = with(Banc()) {
        pilote.tenir(); t = 1_000; pret()
        t = 1_800; pilote.phrase("[unk]")
        assertTrue(phrases.isEmpty())
        assertTrue(voix.dits.isEmpty())
        assertEquals(AffichageEcoute.ECOUTE, pilote.affichage)
    }

    @Test fun `unk apres la seconde - transmis, l'ecran dit Pas compris`() = with(Banc()) {
        pilote.tenir(); t = 1_000; pret()
        t = 2_500; pilote.phrase("[unk]")
        assertEquals(listOf("[unk]"), phrases)
        assertEquals("entendu : « (bruit) »", pilote.entendu)
    }

    // --- fermetures

    @Test fun `la sortie du mode poche ferme le micro et ne rouvre rien, meme touche tenue`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        pilote.arreterEtOublier()
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
        assertEquals(AffichageEcoute.RIEN, pilote.affichage)
        assertNull(pilote.entendu); assertNull(pilote.compris)
        t = 60_000; pilote.avancer()
        t = 61_000; pilote.phrase("atteint")
        assertTrue(phrases.isEmpty(), "le résultat d'une écoute fermée de force n'est pas appliqué")
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
    }

    @Test fun `onStop - arreter ferme, la bande garde ce qui a ete compris`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        pilote.arreter()
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
        assertEquals(AffichageEcoute.FERMEE, pilote.affichage)
        assertEquals(EtatEcoute.FERMEE, pilote.etat)
    }

    @Test fun `erreur du moteur - fermee, dite, jamais rouverte seule`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        t = 500; pilote.erreur()
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
        assertEquals(EtatEcoute.FERMEE, pilote.etat)
        assertTrue(pilote.comprisErreur)
        assertTrue(voix.dits.single().startsWith("Dictée impossible"))
        t = 60_000; pilote.avancer()
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
    }

    // --- anti-larsen : une annonce de geste pendant l'écoute

    @Test fun `une annonce de geste pendant l'ecoute coupe le micro puis le rouvre`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        t = 1_000; sorties.annoncer(AnnoncePoche("numéro 1, sain. Suivant, 2.", MotifsVibration.SAIN))
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
        t = 2_000; voix.finDeLAnnonce()
        assertEquals(listOf("ouvrir#1", "fermer", "ouvrir#2"), micro.journal)
    }

    @Test fun `la voix en echec ne laisse pas le micro ferme pour toujours`() = with(Banc()) {
        val muette = object : MoteurVoix {
            override val francaisDisponible = true
            override fun dire(texte: String) = false
        }
        val s = SortiesPoche(muette, vibreur)
        val p = PilotageEcoute(micro, s, { t }, { true }, { EtatPermissionMicro.ACCORDEE }).also { it.surPhrase = {} }
        p.tenir(); t = 100; p.moteurPret(1)
        t = 1_000; p.phrase("atteint")
        p.resultat(AnnoncePoche("Numéro 7, atteint.", MotifsVibration.ATTEINT))
        assertEquals("ouvrir#2", micro.journal.last(), "la voix a échoué : pas de fin à attendre, le micro rouvre")
    }

    // --- garde-fou de 5 s, « un instant » (constantes nommées)

    @Test fun `garde-fou 5 s - la fin d'annonce ne vient jamais, le micro se rouvre touche tenue`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        t = 1_000; pilote.phrase("atteint")
        pilote.resultat(AnnoncePoche("Numéro 7, atteint.", MotifsVibration.ATTEINT))
        assertEquals(6_000L, pilote.echeance())
        t = 6_000; pilote.avancer()
        assertEquals("ouvrir#2", micro.journal.last())
    }

    @Test fun `garde-fou - la voix parle encore, le micro attend`() = with(Banc()) {
        pilote.tenir(); t = 100; pret()
        t = 1_000; pilote.phrase("atteint")
        pilote.resultat(AnnoncePoche("Numéro 7, atteint. Retiré : cause, mangé, hauteur.", MotifsVibration.ATTEINT))
        voix.parleEncore = true
        t = 6_000; pilote.avancer()
        assertEquals(listOf("ouvrir#1", "fermer"), micro.journal)
    }

    @Test fun `un instant - dit une fois si le modele met plus de 2 s, sans couper le micro`() = with(Banc()) {
        pilote.tenir()
        assertEquals(2_000L, pilote.echeance())
        t = 2_000; pilote.avancer()
        assertEquals(listOf("un instant."), voix.dits)
        assertEquals(listOf("ouvrir#1"), micro.journal, "« un instant » ne ferme pas le micro qui s'ouvre")
        t = 3_000; pilote.avancer()
        assertEquals(1, voix.dits.size)
    }

    // --- modèle absent, micro refusé : la saisie au volume continue

    @Test fun `modele absent - dit a la voix et a l'ecran, le micro ne s'ouvre pas`() = with(Banc(modele = false)) {
        assertFalse(pilote.tenir())
        assertTrue(micro.journal.isEmpty())
        assertEquals(listOf("Dictée non installée. Paramètres, dictée vocale."), voix.dits)
        assertEquals(MotifsVibration.REFUS.toList(), vibreur.motifs.single())
        assertEquals("refusé : Dictée non installée : Paramètres, Dictée vocale.", TextesEcoute.compris(pilote.compris!!, pilote.comprisErreur))
    }

    @Test fun `micro refuse - dit, l'ecoute ne s'ouvre pas`() = with(Banc(permission = EtatPermissionMicro.REFUSEE)) {
        assertFalse(pilote.tenir())
        assertTrue(micro.journal.isEmpty())
        assertEquals(listOf("Dictée indisponible : micro refusé."), voix.dits)
    }

    @Test fun `refus definitif - la voix renvoie aux reglages de l'application`() = with(Banc(permission = EtatPermissionMicro.REFUSEE_DEFINITIVEMENT)) {
        assertFalse(pilote.tenir())
        val dit = voix.dits.single()
        assertTrue(dit.startsWith("Dictée indisponible : micro refusé."))
        assertTrue("Réglages du téléphone, applications, Surculus, autorisations." in dit)
        assertTrue("Réglages" in pilote.compris!!)
    }

    @Test fun `modele absent ou micro refuse - les gestes de volume continuent d'ecrire`() = with(BancControle()) {
        val c = controleur()
        val banc = Banc(modele = false, permission = EtatPermissionMicro.REFUSEE_DEFINITIVEMENT)
        assertFalse(banc.pilote.tenir())
        c.geste(io.github.pobsteta.surculus.core.session.Geste.VOLUME_HAUT_COURT)
        assertEquals("numéro 1, sain. Suivant, 2.", c.derniereAnnonce?.texte)
    }

    // --- permission (états purs)

    @Test fun `permission - apres une demande`() {
        assertEquals(EtatPermissionMicro.ACCORDEE, PermissionMicro.apresDemande(accordee = true, rationale = false))
        assertEquals(EtatPermissionMicro.REFUSEE, PermissionMicro.apresDemande(accordee = false, rationale = true))
        assertEquals(EtatPermissionMicro.REFUSEE_DEFINITIVEMENT, PermissionMicro.apresDemande(accordee = false, rationale = false))
    }

    @Test fun `permission - on ne demande pas apres un refus definitif ni une fois accordee`() {
        assertTrue(PermissionMicro.aDemander(EtatPermissionMicro.A_DEMANDER))
        assertTrue(PermissionMicro.aDemander(EtatPermissionMicro.REFUSEE))
        assertFalse(PermissionMicro.aDemander(EtatPermissionMicro.REFUSEE_DEFINITIVEMENT))
        assertFalse(PermissionMicro.aDemander(EtatPermissionMicro.ACCORDEE))
    }

    @Test fun `permission - les lignes de la bande`() {
        assertNull(TextesEcoute.ligneEtat(EtatPermissionMicro.ACCORDEE, modeleInstalle = true))
        assertNull(TextesEcoute.ligneEtat(EtatPermissionMicro.A_DEMANDER, modeleInstalle = true))
        assertEquals("Micro refusé : la dictée est indisponible, le volume marche.", TextesEcoute.ligneEtat(EtatPermissionMicro.REFUSEE, true))
        assertTrue("Réglages du téléphone" in TextesEcoute.ligneEtat(EtatPermissionMicro.REFUSEE_DEFINITIVEMENT, true)!!)
        assertEquals("Dictée non installée : Paramètres, Dictée vocale.", TextesEcoute.ligneEtat(EtatPermissionMicro.ACCORDEE, modeleInstalle = false))
    }

    // --- la bande : six lignes, six mots

    @Test fun `la bande - entendu et compris, quelques mots chacun`() {
        assertEquals("entendu : « insectes »", TextesEcoute.entendu("insectes"))
        assertEquals("compris : Numéro 7, insectes.", TextesEcoute.compris("Numéro 7, insectes.", erreur = false))
        assertEquals("refusé : Pas compris.", TextesEcoute.compris("Pas compris.", erreur = true))
    }

    // --- vibrations : onze motifs distincts

    @Test fun `onze motifs distincts, chacun de moins d'une seconde, les deux nouveaux ne ressemblent a aucun des neuf`() {
        val tous = MotifsVibration.tous
        assertEquals(11, tous.size)
        assertEquals(tous.size, tous.map { it.toList() }.toSet().size)
        tous.forEach { assertTrue(it.sum() < 1_000, "motif trop long : ${it.toList()}") }
        assertContentEquals(longArrayOf(0, 200), MotifsVibration.OUVERTE)
        assertContentEquals(longArrayOf(0, 200, 150, 200), MotifsVibration.FERMEE)
    }

    // --- Somalie : un fix hors de France ne pointe rien, même par la dictée

    @Test fun `un fix en Somalie ne pointe rien, la dictee n'a donc rien a ecrire`() = with(BancControle()) {
        val somalie = Position(latitude = 2.0469, longitude = 45.3182)
        assertIs<EtatArrivee.Refus>(ouvrir(positions = listOf(somalie)))
        assertEquals(0, journal.nombreDeLignes)
    }

    @Test fun `remplace dicte - en attente, le regarni se pointe a l'ecran, rien n'est ecrit`() = with(BancControle()) {
        val c = controleur()
        c.dicter("mort")
        val avant = journal.nombreDeLignes
        val e = c.dicter("remplace")
        assertEquals(avant, journal.nombreDeLignes, "le regarni s'écrit au pointage, pas à la dictée")
        assertEquals(1, e.pointage.regarniDe, "la vue Plant propose « Remplace le plant n° 1 » à la sortie du mode poche")
        assertEquals("Numéro 1 sera remplacé. Pointez le regarni à l'écran.", c.derniereAnnonce?.texte)
        assertEquals(VueControle.PLANT, e.vue, "en poche on ne change pas de vue")
    }

    // --- la dictée par le contrôleur

    @Test fun `dicter atteint puis insectes - ecrit, confirme, la tournee porte la cause`() = with(BancControle()) {
        val c = controleur()
        c.dicter("atteint")
        assertEquals("Numéro 1, atteint.", c.derniereAnnonce?.texte)
        assertContentEquals(MotifsVibration.ATTEINT, c.derniereAnnonce?.motif)
        val e = c.dicter("insectes")
        assertEquals("Numéro 1, insectes.", c.derniereAnnonce?.texte)
        assertEquals("Insectes", e.plant.groupeCause?.let { TextesControle.libelle(it) })
        assertEquals(io.github.pobsteta.surculus.core.placette.GroupeCause.ENTOMOLOGIQUE, e.plant.groupeCause)
        assertFalse(c.derniereAnnonce!!.erreur)
        assertEquals("Numéro 1, insectes.", e.message?.texte)
    }

    @Test fun `dicter une cause sur un plant sain - refus dit et ecrit en erreur, rien d'ecrit`() = with(BancControle()) {
        val c = controleur()
        c.dicter("sain")
        val avant = journal.nombreDeLignes
        val e = c.dicter("insectes")
        assertEquals(avant, journal.nombreDeLignes)
        assertEquals("Une cause demande un plant atteint ou mort.", c.derniereAnnonce?.texte)
        assertContentEquals(MotifsVibration.REFUS, c.derniereAnnonce?.motif)
        assertTrue(c.derniereAnnonce!!.erreur)
        assertTrue(e.message!!.erreur)
    }

    @Test fun `dicter du bruit - Pas compris, rien d'ecrit`() = with(BancControle()) {
        val c = controleur()
        val avant = journal.nombreDeLignes
        c.dicter("[unk]")
        assertEquals("Pas compris.", c.derniereAnnonce?.texte)
        assertEquals(avant, journal.nombreDeLignes)
    }

    @Test fun `dicter mort - la vibration est celle de l'etat ecrit`() = with(BancControle()) {
        val c = controleur()
        c.dicter("mort")
        assertContentEquals(MotifsVibration.MORT, c.derniereAnnonce?.motif)
    }

    // --- un seul libellé pour la voix et l'écran

    @Test fun `un seul libelle des causes pour l'ecran et la voix`() {
        for (g in io.github.pobsteta.surculus.core.placette.GroupeCause.entries) {
            assertEquals(io.github.pobsteta.surculus.core.session.TextesDictee.cause(g).lowercase(), TextesControle.libelle(g).lowercase(), g.name)
        }
        assertEquals("Champignons", TextesControle.libelle(io.github.pobsteta.surculus.core.placette.GroupeCause.PATHOLOGIQUE))
        assertEquals("champignons", io.github.pobsteta.surculus.core.session.TextesDictee.cause(io.github.pobsteta.surculus.core.placette.GroupeCause.PATHOLOGIQUE))
    }

    // --- câblage (sources)

    private fun source(chemin: String) = java.io.File("src/main/kotlin/io/github/pobsteta/surculus/$chemin").readText()

    @Test fun `le cablage - bas long ouvre, relachement ferme, sortie de poche et onStop arretent`() {
        val e = source("controle/EcranControle.kt")
        assertTrue("sortie.geste == Geste.VOLUME_BAS_LONG) EcouteApp.tenir()" in e)
        assertTrue("SortieClasseur.BasRelacheApresLong -> EcouteApp.relacher()" in e)
        assertTrue("EcouteApp.arreterEtOublier()" in e)
        val a = source("MainActivity.kt")
        assertTrue("EcouteApp.installer(this)" in a)
        assertTrue(a.substringAfter("override fun onPause").substringBefore("override fun onResume").contains("EcouteApp.arreter()"))
        assertTrue(a.substringAfter("override fun onStop").substringBefore("override fun onDestroy").contains("EcouteApp.arreter()"))
    }

    @Test fun `le cablage - le bouton micro se tient, il ne bascule pas`() {
        val e = source("controle/EcranControle.kt")
        assertTrue("PressInteraction.Press -> appui()" in e)
        assertTrue("PressInteraction.Release, is PressInteraction.Cancel -> relache()" in e)
    }

    @Test fun `le cablage - la permission est demandee a Mode poche et au micro, jamais en poche`() {
        val e = source("controle/EcranControle.kt")
        assertEquals(2, Regex("demanderMicro\\.launch\\(Manifest\\.permission\\.RECORD_AUDIO\\)").findAll(e).count())
        assertTrue("apresPermission = entrer" in e, "le mode poche entre quand même, après la réponse")
        assertTrue("\"android.permission.RECORD_AUDIO\"" in java.io.File("src/main/AndroidManifest.xml").readText())
    }

    @Test fun `la voix annonce sa fin - UtteranceProgressListener, seule la derniere annonce compte`() {
        val a = source("controle/VoixAndroid.kt")
        assertTrue("UtteranceProgressListener" in a)
        assertTrue("onDone" in a)
        assertFalse("vosk" in a.lowercase(), "la voix n'a aucun lien avec Vosk")
    }
}
