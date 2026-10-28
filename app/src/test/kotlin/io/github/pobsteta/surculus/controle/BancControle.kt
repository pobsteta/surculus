package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.Projections
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.session.ContexteControle
import io.github.pobsteta.surculus.core.session.EcouteSauvegarde
import io.github.pobsteta.surculus.core.session.FixSession
import io.github.pobsteta.surculus.core.session.HorlogeSession
import io.github.pobsteta.surculus.core.session.JournalControle
import io.github.pobsteta.surculus.core.session.LigneCourante
import io.github.pobsteta.surculus.core.session.PlacetteControlable
import io.github.pobsteta.surculus.core.session.PlantAnterieur
import java.time.Instant

/** Journal en mémoire (mêmes règles de projection que `JournalMemoire` de `:core`, dont les tests ne sont pas visibles d'ici). */
class JournalEnMemoire : JournalControle {
    private class Ligne(val uuid: String, val objet: String, val type: String, val valeur: String?, val remplace: String?)

    private val lignes = ArrayList<Ligne>()
    private var n = 0

    override fun nouvelUuid() = "uuid-${++n}"

    override fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?): String {
        val u = nouvelUuid()
        lignes += Ligne(u, objetUuid, type, valeur, remplaceUuid)
        return u
    }

    override fun annuler(uuidCible: String, operateur: String) {
        val cible = lignes.first { it.uuid == uuidCible }
        lignes += Ligne(nouvelUuid(), cible.objet, "annulation", null, uuidCible)
    }

    override fun lignesCourantes(): List<LigneCourante> {
        val actives = lignes.filter { it.type != "annulation" && lignes.none { a -> a.type == "annulation" && a.remplace == it.uuid } }
        return actives.filter { l -> actives.none { it.remplace == l.uuid } }.map { LigneCourante(it.uuid, it.objet, it.type, it.valeur, it.remplace) }
    }

    val nombreDeLignes: Int get() = lignes.size
}

class EcouteComptee : EcouteSauvegarde {
    var saisies = 0
    var fins = 0
    override fun surSaisie() { saisies++ }
    override fun surFinPlacette() { fins++ }
}

class HorlogeDeTest(var maintenantMs: Long = Instant.parse("2026-10-27T09:00:00Z").toEpochMilli()) : HorlogeSession {
    override fun maintenantUtcMs() = maintenantMs
    override fun monotoneMs() = 1_000_000L
    override fun fix(): FixSession? = null
}

/** Une tournée de deux placettes à Couchey (EPSG:2154) ; la première porte des plants d'un passage précédent. */
class BancControle(
    nbPlantsAnterieurs: Int = 12,
    precedent: Observation? = Observation.Observe(EtatDsf.ATTEINT),
    val emprise: EmpriseLambert93? = null,
) {
    companion object {
        val POSITION_P1 = PointLambert93(xMetres = 846_164.76, yMetres = 6_686_123.26)
        val POSITION_P2 = PointLambert93(xMetres = 846_264.76, yMetres = 6_686_223.26)
        val P1 = UuidPlacette("placette-1")
        val P2 = UuidPlacette("placette-2")

        /** Position GNSS (latitude, longitude, en degrés) d'un point Lambert-93, pour le tester comme un fix. */
        fun fixSur(p: PointLambert93): Position {
            val (lon, lat) = Projections.versWgs84(p.xMetres, p.yMetres)
            return Position(latitude = lat, longitude = lon)
        }
    }

    val journal = JournalEnMemoire()
    val horloge = HorlogeDeTest()
    val essences = Essences(emptySet())
    val contexte = ContexteControle(
        operateur = "Agent d'essai",
        placettes = listOf(
            PlacetteControlable(Piquet(P1, "P35-01", POSITION_P1), 3.99),
            PlacetteControlable(Piquet(P2, "P35-02", POSITION_P2), 3.99),
        ),
        epsg = CodeEpsg.RGF93_V1_LAMBERT93,
        empriseForet = emprise,
        plantsAnterieurs = mapOf(
            P1 to (1..nbPlantsAnterieurs).map { PlantAnterieur(UuidPlant("ancien-$it"), it, null, null, precedent) },
        ),
    )
    val sauvegarde = EcouteComptee()

    fun positionsAuPiquet() = List(5) { fixSur(POSITION_P1) }

    fun ouvrir(positions: List<Position> = positionsAuPiquet(), codeLu: String? = null): EtatArrivee =
        ArriveeControle.ouvrir(contexte, journal, horloge, essences, positions, codeLu, sauvegarde)

    fun controleur(): ControleurPlacette = (ouvrir() as EtatArrivee.Ouverte).controleur
}
