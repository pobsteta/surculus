/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.messages

import io.github.pobsteta.surculus.tournee.DepotTournees
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mission 082 : plus aucun message d'exception brut à l'écran. Écrit depuis la fiche et la spec, sans lire le code des
 * huit endroits (carte/Commun, DepotTournees, EcranTournees, MoteurDicteeVosk x2, ServiceGnssRtk x3).
 */
class AucunMessageBrutTest {
    /** Ce qu'un texte montré au forestier ne doit jamais contenir. Rend les défauts trouvés (vide = propre). */
    private fun defauts(texte: String): List<String> = buildList {
        if (Regex("\\b[A-Za-z0-9]+(Exception|Error|Throwable)\\b").containsMatchIn(texte)) add("nom d'exception")
        if (Regex("\\b(java|javax|kotlin|android|org|io|com)\\.[a-z]").containsMatchIn(texte)) add("nom de classe qualifié")
        if ('_' in texte) add("« _ »")
        if (Regex("(^|\\s)/\\w|[A-Za-z]:\\\\|\\w/\\w").containsMatchIn(texte)) add("chemin")
        if (Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}").containsMatchIn(texte)) add("UUID")
        val anglais = listOf(
            "no such file", "not a database", "file is not", "failed", "unable", "cannot", "can't", "permission denied", "timeout",
            "timed out", "connection", "refused", "reset by peer", "unexpected", "illegal", "invalid", "null", "exception", "error",
            "broken pipe", "socket", "bluetooth device", "not found", "is a directory", "unknown host", "read failed",
        )
        val bas = texte.lowercase()
        anglais.filter { it in bas }.forEach { add("anglais « $it »") }
        if (texte.isBlank()) add("texte vide")
    }

    private fun assertPropre(ou: String, texte: String?) {
        assertNotNull(texte, "$ou : pas de texte")
        val d = defauts(texte)
        assertTrue(d.isEmpty(), "$ou : « $texte » contient $d")
    }

    // --- Garde-fou : les huit endroits ne montrent plus l'exception (lecture mécanique des sources, comme la 061) ----

    private val main = File("src/main").takeIf { it.isDirectory } ?: File("app/src/main")
    private fun source(chemin: String) = File(main, "kotlin/io/github/pobsteta/surculus/$chemin")

    /** Lignes de code (hors commentaires et hors `Log.`) où une exception est lue : `.message`, `simpleName`, `toString()`. */
    private fun lignesBrutes(chemin: String): List<String> =
        source(chemin).readLines().withIndex().filter { (_, l) ->
            val t = l.trim()
            !t.startsWith("//") && !t.startsWith("*") && !t.startsWith("/*") && "Log." !in l &&
                Regex("(?<!\\.refus)\\??\\.(message|localizedMessage)\\b|simpleName|\\.toString\\(\\)").containsMatchIn(l)
        }.map { (i, l) -> "$chemin:${i + 1} ${l.trim()}" }

    private fun assertSansException(chemin: String) {
        val brutes = lignesBrutes(chemin)
        assertTrue(brutes.isEmpty(), "message d'exception encore à l'écran :\n" + brutes.joinToString("\n"))
    }

    @Test fun `carte - le refus de chargement ne montre pas l'exception`() = assertSansException("carte/Commun.kt")
    @Test fun `liste des tournees - la ligne illisible ne montre pas l'exception`() = assertSansException("tournee/DepotTournees.kt")
    @Test fun `import de tournee - le refus ne montre pas l'exception`() = assertSansException("tournee/EcranTournees.kt")
    @Test fun `dictee - les erreurs ne montrent pas l'exception`() = assertSansException("dictee/MoteurDicteeVosk.kt")
    @Test fun `recepteur GNSS et NTRIP - les erreurs ne montrent pas l'exception`() = assertSansException("gnss/ServiceGnssRtk.kt")

    // --- Par l'API publique : la ligne d'une tournée illisible, quelle que soit la cause ----------------------------

    private fun dossierAvec(contenu: ByteArray?): File {
        val d = Files.createTempDirectory("tournee-082").toFile().also { it.deleteOnExit() }
        if (contenu != null) File(d, "travaux.gpkg").also { it.writeBytes(contenu); it.deleteOnExit() }
        return d
    }

    private fun refusDeLigne(contenu: ByteArray?): String? = DepotTournees.ligne(dossierAvec(contenu)).refus

    @Test fun `tournee illisible - un fichier qui n'est pas un GeoPackage`() =
        assertPropre("ligne, texte quelconque", refusDeLigne("ceci n'est pas une base_de_donnees /data/x_y".toByteArray()))

    // Le fichier vide n'est pas testé ici : son refus (« ni identifiant d'application GPKG, ni table gpkg_contents ») est un
    // refus déjà en français, que la fiche interdit de changer ; Pascal dira si le nom de table doit rester.

    @Test fun `tournee illisible - un en-tete SQLite tronque`() =
        assertPropre("ligne, SQLite tronqué", refusDeLigne("SQLite format 3\u0000".toByteArray() + ByteArray(40)))

    @Test fun `tournee illisible - octets au hasard`() =
        assertPropre("ligne, octets au hasard", refusDeLigne(ByteArray(4096) { (it * 31 + 7).toByte() }))

    @Test fun `tournee illisible - le GeoPackage est un dossier`() {
        val d = dossierAvec(null)
        File(d, "travaux.gpkg").mkdir()
        assertPropre("ligne, gpkg dossier", DepotTournees.ligne(d).refus)
    }

    // --- Reprise 1/3 (2026-11-02) : refus traduits, avec l'action à faire ---------------------------------------------

    /** Un SQLite sans aucune table de GeoPackage ; [idGpkg] y pose l'identifiant d'application « GPKG ». */
    private fun sqliteSansTables(idGpkg: Boolean): ByteArray {
        val f = Files.createTempFile("sans-tables-082", ".db").toFile().also { it.delete(); it.deleteOnExit() }
        DriverManager.getConnection("jdbc:sqlite:${f.absolutePath}").use { c ->
            c.createStatement().use { st ->
                if (idGpkg) st.execute("PRAGMA application_id = 1196444487")
                st.execute("CREATE TABLE autre (a INTEGER)")
            }
        }
        return f.readBytes()
    }

    private val abime = "ceci n'est pas une base de données".toByteArray()

    /**
     * Critère « dit quoi faire » : après le premier « : », un verbe à l'infinitif (mot en -er, -ir ou -re, hors noms
     * courants en -ier et hors mots outils). Une simple constatation (« Fichier vide. ») ne passe pas.
     */
    private fun ditQuoiFaire(texte: String): Boolean {
        val apres = texte.substringAfter(':', "")
        val horsVerbes = setOf("autre", "entre", "contre", "notre", "votre", "outre", "ordre", "être", "mettre", "chaque")
        return Regex("\\p{L}{3,}").findAll(apres).map { it.value.lowercase() }.any { m ->
            (m.endsWith("er") || m.endsWith("ir") || m.endsWith("re")) && !m.endsWith("ier") && m !in horsVerbes
        }
    }

    @Test fun `le critere dit quoi faire distingue constatation et consigne`() {
        assertFalse(ditQuoiFaire("Fichier vide."))
        assertFalse(ditQuoiFaire("Fichier vide : aucune table."))
        assertTrue(ditQuoiFaire("Fichier vide : le réexporter depuis sommieR."))
        assertTrue(ditQuoiFaire("Fichier abîmé : demander un nouvel export."))
    }

    private fun assertTraduitEtActionnable(ou: String, texte: String?) {
        assertPropre(ou, texte)
        val bas = texte!!.lowercase()
        listOf("gpkg", "spatial_ref").forEach {
            assertFalse(it in bas, "$ou : « $texte » garde le nom technique « $it »")
        }
        assertTrue(ditQuoiFaire(texte), "$ou : « $texte » ne dit pas quoi faire (verbe à l'infinitif attendu après « : »)")
    }

    @Test fun `refus - fichier vide, en francais et disant quoi faire`() =
        assertTraduitEtActionnable("fichier vide", refusDeLigne(ByteArray(0)))

    @Test fun `refus - sqlite sans table gpkg_contents ni identifiant GPKG`() =
        assertTraduitEtActionnable("sans tables, sans id", refusDeLigne(sqliteSansTables(idGpkg = false)))

    @Test fun `refus - identifiant GPKG mais sans table gpkg_contents`() =
        assertTraduitEtActionnable("sans tables, avec id", refusDeLigne(sqliteSansTables(idGpkg = true)))

    @Test fun `refus - pas un GeoPackage (fichier abime) dit quoi faire`() =
        assertTraduitEtActionnable("pas un GeoPackage", refusDeLigne(abime))

    // --- La phrase composée de la liste : préfixe « Illisible : » + refus, telle qu'elle s'affiche ----------------------

    private val ressources = File(main, "res/values/strings_tournees.xml")

    /** Le gabarit `tournees_illisible`, lu dans la ressource publiée (« Illisible : %1$s »). */
    private fun gabaritIllisible(): String =
        Regex("<string name=\"tournees_illisible\">(.*?)</string>").find(ressources.readText())!!.groupValues[1]
            .replace("\\'", "'")

    private fun composee(refus: String?): String {
        assertNotNull(refus, "pas de refus pour une tournée illisible")
        return gabaritIllisible().replace("%1\$s", refus)
    }

    private fun assertSansBegaiement(ou: String, refus: String?) {
        val phrase = composee(refus)
        val n = Regex("illisible", RegexOption.IGNORE_CASE).findAll(phrase).count()
        assertTrue(n == 1, "$ou : « $phrase » répète « illisible » ($n fois)")
        assertFalse(Regex(":\\s*:").containsMatchIn(phrase), "$ou : « $phrase » a deux « : » à la suite")
    }

    @Test fun `liste - phrase composee d'un fichier vide sans begaiement`() =
        assertSansBegaiement("vide", refusDeLigne(ByteArray(0)))

    @Test fun `liste - phrase composee d'un fichier abime sans begaiement`() =
        assertSansBegaiement("abîmé", refusDeLigne(abime))

    @Test fun `liste - phrase composee d'un GeoPackage sans tables sans begaiement`() =
        assertSansBegaiement("sans tables", refusDeLigne(sqliteSansTables(idGpkg = true)))

    @Test fun `liste - phrase composee d'octets au hasard sans begaiement`() =
        assertSansBegaiement("hasard", refusDeLigne(ByteArray(4096) { (it * 31 + 7).toByte() }))

    // --- Récepteur : « injoignable » disparaît des textes d'erreur (par les ressources publiées) -----------------------

    @Test fun `recepteur - aucune phrase d'erreur du service ne dit injoignable`() {
        val textes = File(main, "res/values").listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }!!
            .flatMap { f -> Regex("<string name=\"(rtk_[a-z_]+|carte_recepteur_[a-z_]+)\">(.*?)</string>").findAll(f.readText()).toList() }
        assertTrue(textes.isNotEmpty(), "aucune ressource rtk_* trouvée")
        val fautifs = textes.filter { "injoignable" in it.groupValues[2].lowercase() }.map { it.groupValues[1] }
        assertTrue(fautifs.isEmpty(), "« injoignable » encore dans : $fautifs")
    }
}
