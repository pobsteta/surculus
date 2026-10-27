package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import java.time.Instant

/** Une ligne du journal en mémoire (tests). */
class LigneMemoire(
    val uuid: String,
    val objetUuid: String,
    val type: String,
    val valeur: String?,
    val remplaceUuid: String?,
    val operateur: String,
    val fix: FixSession?,
)

/**
 * Journal en mémoire qui suit les règles de projection de la spec § 5 : une annulation défait (la valeur remplacée
 * redevient courante), une ligne est tête si elle est active et qu'aucune ligne active ne la remplace. Les tests de
 * `:data` rejouent les mêmes scénarios sur le vrai journal (Room).
 */
class JournalMemoire : JournalControle {
    val lignes = ArrayList<LigneMemoire>()
    private var n = 0

    override fun nouvelUuid() = "uuid-${++n}"

    override fun saisir(objetUuid: String, type: String, valeur: String, operateur: String, remplaceUuid: String?, fix: FixSession?): String {
        val u = nouvelUuid()
        lignes += LigneMemoire(u, objetUuid, type, valeur, remplaceUuid, operateur, fix)
        return u
    }

    override fun annuler(uuidCible: String, operateur: String) {
        val cible = lignes.first { it.uuid == uuidCible }
        lignes += LigneMemoire(nouvelUuid(), cible.objetUuid, FormatValeurs.TYPE_ANNULATION, null, uuidCible, operateur, null)
    }

    private fun annulee(l: LigneMemoire) = lignes.any { it.type == FormatValeurs.TYPE_ANNULATION && it.remplaceUuid == l.uuid }

    override fun lignesCourantes(): List<LigneCourante> {
        val actives = lignes.filter { it.type != FormatValeurs.TYPE_ANNULATION && !annulee(it) }
        return actives.filter { l -> actives.none { it.remplaceUuid == l.uuid } }
            .map { LigneCourante(it.uuid, it.objetUuid, it.type, it.valeur, it.remplaceUuid) }
    }

    fun deType(type: String) = lignes.filter { it.type == type }
}

class HorlogeFixe(var maintenantMs: Long, var monotoneMs: Long = 1_000_000L, var fixCourant: FixSession? = null) : HorlogeSession {
    override fun maintenantUtcMs() = maintenantMs
    override fun monotoneMs() = monotoneMs
    override fun fix() = fixCourant

    fun a(iso: String) { maintenantMs = Instant.parse(iso).toEpochMilli() }
}

class EcouteComptee : EcouteSauvegarde {
    var saisies = 0
    var fins = 0
    override fun surSaisie() { saisies++ }
    override fun surFinPlacette() { fins++ }
}

/** Banc d'essai : une tournée de deux placettes à Couchey (EPSG:2154), un journal et une horloge en mémoire. */
class BancSession(
    heure: String = "2026-10-27T09:00:00Z",
    val plantsAnterieurs: Map<UuidPlacette, List<PlantAnterieur>> = emptyMap(),
    val essences: Essences = Essences(setOf("CHS", "HET")),
) {
    companion object {
        /** Piquet P35-01 à Couchey (x = E, y = N, mètres, EPSG:2154). */
        val POSITION_P1 = PointLambert93(xMetres = 846_164.76, yMetres = 6_686_123.26)
        val POSITION_P2 = PointLambert93(xMetres = 846_264.76, yMetres = 6_686_223.26)
        val P1 = UuidPlacette("placette-1")
        val P2 = UuidPlacette("placette-2")

        fun degres(v: Double) = AzimutSaisi.valider(v, UniteAzimut.DEGRES).valeurOuEchec()
    }

    val journal = JournalMemoire()
    val horloge = HorlogeFixe(Instant.parse(heure).toEpochMilli())
    val ecoute = EcouteComptee()
    val contexte = ContexteControle(
        operateur = "Agent d'essai",
        placettes = listOf(
            PlacetteControlable(Piquet(P1, "P35-01", POSITION_P1), 3.99),
            PlacetteControlable(Piquet(P2, "P35-02", POSITION_P2), 3.99),
        ),
        epsg = CodeEpsg.RGF93_V1_LAMBERT93,
        plantsAnterieurs = plantsAnterieurs,
    )

    /** Ouvre la session de la placette 1 (position mesurée sur son piquet, sauf indication). */
    fun ouvrir(position: PointLambert93 = POSITION_P1, codeLu: String? = null): OuvertureControle =
        SessionControle.ouvrir(contexte, journal, horloge, essences, Arrivee.Lambert93(position), codeLu, ecoute)

    fun session(position: PointLambert93 = POSITION_P1): SessionControle = (ouvrir(position) as OuvertureControle.Ouverte).session

    fun SessionControle.pointe(distanceM: Double, azimutDeg: Double, essence: String? = "CHS", confirme: Boolean = false) =
        pointer(distanceM, degres(azimutDeg), SourceAzimut.BOUSSOLE_TELEPHONE, essence, confirme)
}
