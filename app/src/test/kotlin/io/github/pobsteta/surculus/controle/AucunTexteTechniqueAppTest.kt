package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.RefusRepere
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.dictee.IssueInstallation
import io.github.pobsteta.surculus.dictee.PhaseInstallation
import io.github.pobsteta.surculus.dictee.SuiviInstallation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Mission 081 : pointage, refus au contrôle et échec d'installation de Vosk, sans texte technique à l'écran. */
class AucunTexteTechniqueAppTest {
    private val uuidRe = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val decimalRe = Regex("\\d\\.\\d")
    private val exceptionRe = Regex("(Exception|Error|java\\.|kotlin\\.)")

    private fun verifier(nom: String, texte: String) {
        assertTrue(texte.isNotBlank(), "$nom : texte vide")
        assertFalse('_' in texte, "$nom : « $texte » contient un « _ »")
        assertFalse(uuidRe.containsMatchIn(texte), "$nom : « $texte » contient un UUID")
        assertFalse(decimalRe.containsMatchIn(texte), "$nom : « $texte » contient un nombre à point décimal")
        assertFalse(exceptionRe.containsMatchIn(texte), "$nom : « $texte » contient un nom d'exception")
    }

    private fun textes(e: EcranControle): List<String> =
        listOfNotNull(e.message?.texte, e.pointage.aConfirmer, e.totaux.refus, e.totaux.texte)

    // ------------------------------------------------------------------ pointage (mineur de la 074)

    @Test fun `le texte du pointage n'a ni point decimal ni identifiant`() {
        listOf(
            Triple(5, 123.456, 7.5),
            Triple(12, 0.0, 3.99),
            Triple(null, 359.9, 0.1),
        ).forEach { (n, az, d) -> verifier("pointe($n, $az, $d)", TextesControle.pointe(n, az, d)) }
    }

    @Test fun `un plant pointe au clavier est annonce sans texte technique`() = with(BancControle()) {
        val e = controleur().pointerAuClavier("3,2", "123,4", UniteAzimut.DEGRES)
        assertNotNull(e.message, "le pointage doit annoncer quelque chose")
        textes(e).forEach { verifier("pointage clavier", it) }
    }

    @Test fun `un plant pointe a la boussole est annonce sans texte technique`() = with(BancControle()) {
        val e = controleur().pointerAvecBoussole("3,2", CapMagnetique.de(100.5)!!)
        textes(e).forEach { verifier("pointage boussole", it) }
    }

    @Test fun `l'alerte hors rayon montree au forestier est sans texte technique`() = with(BancControle()) {
        val e = controleur().pointerAuClavier("6", "90", UniteAzimut.DEGRES)
        assertNotNull(e.pointage.aConfirmer, "6 m dépasse le rayon de 3,99 m : confirmation attendue")
        textes(e).forEach { verifier("hors rayon", it) }
    }

    // ------------------------------------------------------------------ refus au contrôle (mission 068)

    @Test fun `une distance refusee (negative) est dite sans decimale a point`() = with(BancControle()) {
        val e = controleur().pointerAuClavier("-1", "90", UniteAzimut.DEGRES)
        textes(e).forEach { verifier("distance négative", it) }
    }

    @Test fun `un point a plus de quinze metres est refuse sans decimale a point`() = with(BancControle()) {
        val e = controleur().pointerAuClavier("17,3", "90", UniteAzimut.DEGRES)
        textes(e).forEach { verifier("hors quinze mètres", it) }
    }

    @Test fun `le message de refus du controleur ne montre ni motif ni UUID`() {
        val uuid = UuidPlant("0190f3a2-7b1c-7d4e-8a9b-1c2d3e4f5a6b")
        listOf(
            RefusPlacette.MesureSurNonObserve,
            RefusPlacette.PlantEnDoubleDansControle(uuid),
            RefusPlacette.PlantDejaRemplace(uuid, UuidPlant("0190f3a2-7b1c-7d4e-8a9b-aaaaaaaaaaaa")),
            RefusPlacette.HorsQuinzeMetres(17.3),
        ).forEach {
            val m = assertNotNull(TextesControle.message(SuiteSession.Refus.de(it)), it.toString())
            verifier(it::class.simpleName ?: "refus", m.texte)
        }
        val d = assertNotNull(TextesControle.message(SuiteSession.Refus.de(RefusRepere.DistanceInvalide(-1.5))))
        verifier("DistanceInvalide", d.texte)
    }

    @Test fun `le refus des totaux est sans texte technique`() {
        val uuid = UuidPlant("0190f3a2-7b1c-7d4e-8a9b-1c2d3e4f5a6b")
        val v = TextesControle.totaux(Verdict.Refuse(RefusPlacette.PlantEnDoubleDansControle(uuid)))
        verifier("totaux refus", assertNotNull(v.refus))
    }

    // ------------------------------------------------------------------ échec d'installation de Vosk (mission 052)

    private fun echec(t: Throwable): IssueInstallation.Echec {
        val s = SuiviInstallation(CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        s.demarrer(PhaseInstallation.RESEAU, { true }) { _, _, _ -> throw t }
        val issue = s.etat.value.issue
        assertTrue(issue is IssueInstallation.Echec, "issue attendue : Echec, obtenue : $issue")
        return issue as IssueInstallation.Echec
    }

    @Test fun `l'echec d'installation ne montre pas le message brut de l'exception`() {
        listOf(
            UnknownHostException("alphacephei.com"),
            java.io.IOException("Unable to resolve host \"alphacephei.com\": No address associated with hostname"),
            IllegalStateException("vosk_model_small_fr_0.22.zip abime"),
            UnsatisfiedLinkError("libvosk_jni.so"),
            OutOfMemoryError(),
            RuntimeException(),
            RuntimeException("java.lang.IllegalArgumentException: modele_fr"),
        ).forEach { verifier("échec ${it::class.simpleName}", echec(it).cause) }
    }

    @Test fun `l'echec garde le drapeau avait, seul le texte change`() {
        assertTrue(echec(IllegalStateException("zip_abime")).avait)
    }
}
