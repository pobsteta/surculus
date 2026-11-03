package io.github.pobsteta.surculus.tournee

import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mission 090 : « Rendre la tournée » côté application (décisions de Pascal du 2026-11-03). Nom du `.zip` sans écrasement,
 * état « rendue » de la liste, et câblage des écrans (lecture des sources, comme `EcranPlacettesSourcesTest`).
 */
class RenduAppTest {
    private val main = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
    private fun source(chemin: String) = File(main, "kotlin/io/github/pobsteta/surculus/$chemin").readText()
    private val chaines by lazy { File(main, "res/values/strings_tournees.xml").readText() }

    // 2026-10-20T08:30:00Z = 10 h 30 à Paris (heure d'été).
    private val maintenant = Instant.parse("2026-10-20T08:30:00Z")

    @Test
    fun `premier rendu - le zip porte le nom de la tournee`() {
        assertEquals("Couchey-20261020-101500.zip", RenduApp.nomDuZip("Couchey-20261020-101500", maintenant) { false })
    }

    @Test
    fun `nouveau rendu - un zip neuf horodate, jamais l'ancien remplace`() {
        val pris = mutableSetOf("Couchey.zip")
        assertEquals("Couchey-20261020-103000.zip", RenduApp.nomDuZip("Couchey", maintenant) { it in pris })
        pris += "Couchey-20261020-103000.zip"
        assertEquals("Couchey-20261020-103000-2.zip", RenduApp.nomDuZip("Couchey", maintenant) { it in pris })
    }

    @Test
    fun `une tournee rendue passe en colonne Realisee, une ouverte non`() {
        val rendue = LigneTournee(File("x"), null, null, rendue = true)
        assertTrue(rendue.rendue)
        assertEquals(EtatKanban.REALISEE, statutMinimal(0, rendue.rendue))
        assertFalse(LigneTournee(File("x"), null, null).rendue)
    }

    @Test
    fun `la liste lit l'etat rendue dans la marque du rendu`() {
        val depot = source("tournee/DepotTournees.kt")
        assertTrue("rendue = RenduTournee.estRendue(dossier.toPath())" in depot)
        assertFalse("val rendue: Boolean get() = false" in depot, "l'état n'est plus toujours faux")
    }

    @Test
    fun `le bouton est en bas des placettes, 56 dp, et le rendu tourne hors du fil principal`() {
        val ecran = source("tournee/EcranPlacettes.kt")
        assertTrue("BasRendu(id" in ecran)
        assertTrue("HAUTEUR_BOUTON = 56.dp" in ecran)
        assertTrue("RenduApp.rendre(ctx, id)" in ecran)
        val i = ecran.indexOf("RenduApp.rendre(ctx, id)")
        assertTrue(ecran.lastIndexOf("withContext(Dispatchers.IO)", i) in (i - 200) until i, "rendu sous Dispatchers.IO")
        assertTrue("onPlacetteCode(code)" in ecran, "chaque faute ouvre sa placette")
        assertTrue("RenduApp.partager(ctx, issue.prive)" in ecran)
        assertTrue("dismissOnBackPress = false" in ecran, "pas d'interruption du rendu par le retour")
    }

    @Test
    fun `une tournee rendue n'ouvre aucun controle`() {
        val controle = source("controle/EcranControle.kt")
        assertTrue("RenduTournee.estRendue(ouverte.dossier.toPath())" in controle)
        assertTrue("if (ouverte == null || rendue ||" in controle, "l'arrivée ne démarre pas sur une tournée rendue")
        assertTrue("rendue -> Texte(stringResource(R.string.controle_tournee_rendue))" in controle)
    }

    @Test
    fun `les textes sont ceux decides par Pascal`() {
        assertTrue(">Rendre la tournée<" in chaines)
        assertTrue(">Rouvrir la tournée<" in chaines)
        assertTrue(">Vérification…<" in chaines && ">Écriture…<" in chaines)
        assertTrue(">Tournée rendue : %1\$s, %2\$s<" in chaines)
        assertTrue(">Partager<" in chaines)
        assertTrue("Un contrôle déjà importé dans sommieR ne sera pas mis à jour" in chaines)
    }

    @Test
    fun `le partage ne sert que le dossier des zip rendus, par un FileProvider non exporte`() {
        val manifeste = File(main, "AndroidManifest.xml").readText()
        val p = manifeste.substringAfter("<provider").substringBefore("</provider>")
        assertTrue("androidx.core.content.FileProvider" in p)
        assertTrue("android:exported=\"false\"" in p)
        assertTrue("\${applicationId}.rendus" in p)
        val chemins = File(main, "res/xml/chemins_partages.xml").readText()
        assertTrue("<files-path name=\"rendus\" path=\"rendus/\" />" in chemins)
        assertEquals(1, Regex("-path ").findAll(chemins).count(), "un seul dossier partagé")
        assertTrue("\"\${ctx.packageName}.rendus\"" in source("tournee/RenduApp.kt"))
    }
}
