/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.session

import io.github.pobsteta.surculus.core.geo.EmpriseLambert93
import io.github.pobsteta.surculus.core.geo.PointLambert93
import io.github.pobsteta.surculus.core.placette.AlertePlacette
import io.github.pobsteta.surculus.core.placette.CodesRefus
import io.github.pobsteta.surculus.core.placette.ControleEcrit
import io.github.pobsteta.surculus.core.placette.ControleRayon
import io.github.pobsteta.surculus.core.placette.EcritureControle
import io.github.pobsteta.surculus.core.placette.EmpriseEtFrance
import io.github.pobsteta.surculus.core.placette.Essences
import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.EtatPlant
import io.github.pobsteta.surculus.core.placette.Identite
import io.github.pobsteta.surculus.core.placette.IdentitePlant
import io.github.pobsteta.surculus.core.placette.JourCivil
import io.github.pobsteta.surculus.core.placette.Numerotation
import io.github.pobsteta.surculus.core.placette.NumerosPlacette
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.ObservationDatee
import io.github.pobsteta.surculus.core.placette.Observations
import io.github.pobsteta.surculus.core.placette.Piquet
import io.github.pobsteta.surculus.core.placette.Placette
import io.github.pobsteta.surculus.core.placette.PlantANumeroter
import io.github.pobsteta.surculus.core.placette.Pointage
import io.github.pobsteta.surculus.core.placette.Rattache
import io.github.pobsteta.surculus.core.placette.Rattachement
import io.github.pobsteta.surculus.core.placette.RefusPlacette
import io.github.pobsteta.surculus.core.placette.RegarniPose
import io.github.pobsteta.surculus.core.placette.Regarnis
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.UnControleParJour
import io.github.pobsteta.surculus.core.placette.UuidControle
import io.github.pobsteta.surculus.core.placette.UuidPlacette
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.reperes.CodeEpsg
import io.github.pobsteta.surculus.core.reperes.CodesUniteAzimut
import io.github.pobsteta.surculus.core.reperes.PlantDerive
import io.github.pobsteta.surculus.core.reperes.PointDerive
import io.github.pobsteta.surculus.core.reperes.PointGeographique
import io.github.pobsteta.surculus.core.reperes.RefusRepere
import io.github.pobsteta.surculus.core.reperes.Resultat
import java.time.Instant
import java.time.LocalDate

/** D'où vient l'azimut d'un plant (décision de Pascal du 2026-10-27) : la boussole du téléphone, ou une valeur tapée. */
enum class SourceAzimut(val code: String) {
    BOUSSOLE_TELEPHONE("boussole_telephone"),
    SAISIE("saisie"),
}

/** Placette de la tournée que l'on peut contrôler : son piquet (UUID, code, position Lambert-93) et son rayon. */
data class PlacetteControlable(val piquet: Piquet, val rayonM: Double)

/**
 * Plant d'un passage précédent (couches `plants` et `etats_plants` de sommieR, lues par `LecteurTournee`) : il garde son
 * numéro, son état du passage précédent est montré à côté de celui du jour (décision de Pascal du 2026-10-27).
 */
data class PlantAnterieur(
    val uuid: UuidPlant,
    val numero: Int,
    val essence: String?,
    val remplace: UuidPlant?,
    /** Dernier état observé au passage précédent, `null` s'il n'en a pas. */
    val observation: Observation?,
)

/** Ce que la session sait de la tournée ouverte. */
data class ContexteControle(
    /** `projet.operateur` de la tournée (décision de Pascal du 2026-10-27) ; écrit dans chaque ligne. */
    val operateur: String,
    val placettes: List<PlacetteControlable>,
    /** Repère du fichier de la tournée (lu dans le fichier, jamais supposé). */
    val epsg: CodeEpsg,
    val empriseForet: EmpriseLambert93? = null,
    val plantsAnterieurs: Map<UuidPlacette, List<PlantAnterieur>> = emptyMap(),
)

/** Position de l'agent à l'arrivée, déjà moyennée par l'écran ; ordre et unités explicites. */
sealed interface Arrivee {
    /** Fix GNSS en degrés décimaux, **(latitude, longitude)** ; testé en France avant toute projection. */
    data class Gnss(val position: PointGeographique) : Arrivee

    /** Position déjà en Lambert-93 (x = Est, y = Nord, mètres). */
    data class Lambert93(val position: PointLambert93) : Arrivee
}

/** Issue d'une commande de la session. Un refus porte son **code** de la spec § 6 et l'explication en français. */
sealed interface SuiteSession {
    /** Commande écrite ; [lignes] : `uuid` des lignes ajoutées au journal. */
    data class Fait(val lignes: List<String> = emptyList()) : SuiteSession

    data class Refus(val code: String, val motif: String, val message: String, val axesInverses: Boolean = false) : SuiteSession {
        companion object {
            fun de(refus: RefusPlacette) = Refus(CodesRefus.code(refus), refus.motif, refus.message)
            fun de(refus: RefusRepere) = Refus(CodesRefus.code(refus), refus.motif, refus.message)
        }
    }

    /** Alerte qui exige une confirmation explicite de l'agent (plant hors rayon) : rien n'est écrit. */
    data class AConfirmer(val alerte: AlertePlacette) : SuiteSession

    /** « vide » et « fin du vide » : rien n'est tracé en tranche 1 (décision de Pascal du 2026-10-27). Pas un code de la spec § 6. */
    data class PasEncoreDisponible(val fonction: String) : SuiteSession

    /** Aucun plant courant : pointer un plant d'abord. */
    data object AucunPlantCourant : SuiteSession

    /** L'état d'abord : une cause ou une hauteur ne se dit que sur un plant qui a déjà un état. */
    data object EtatDAbord : SuiteSession

    data object RienAAnnuler : SuiteSession

    /** Terminer sans qu'aucun état n'ait été dit aujourd'hui : refusé (question posée à Pascal). */
    data object RienASaisir : SuiteSession

    /** Le contrôle du jour est déjà terminé. */
    data object DejaTerminee : SuiteSession

    /** Le geste « volume bas, appui long » : l'écran ouvre la dictée (012-D). */
    data object DicteeDemandee : SuiteSession
}

sealed interface OuvertureControle {
    data class Ouverte(val session: SessionControle) : OuvertureControle
    data class Refusee(val refus: SuiteSession.Refus) : OuvertureControle
}

/** Un plant tel que la session le montre. */
data class PlantSession(
    val uuid: UuidPlant,
    val numero: Int,
    /** Numéro figé (placette terminée ou plant d'un passage précédent) ; provisoire sinon. */
    val numeroFige: Boolean,
    val essence: String?,
    val remplace: UuidPlant?,
    val distanceHorizontaleM: Double?,
    val azimutGeographiqueDeg: Double?,
    /** Position dérivée en Lambert-93 (x = Est, y = Nord) ; `null` pour un plant d'un passage précédent. */
    val position: PointLambert93?,
    /** État dit aujourd'hui ; `null` : pas encore dit. */
    val duJour: Observation?,
    /** État du passage précédent. */
    val precedent: Observation?,
    /** Remplacé par un regarni posé avant aujourd'hui : il sort des totaux. */
    val remplaceAvant: Boolean,
)

data class EtatSession(
    val placette: UuidPlacette,
    val codePlacette: String,
    /** Jour civil Europe/Paris du contrôle en cours. */
    val jour: LocalDate,
    /** Contrôle du jour ; `null` tant que rien n'est saisi. */
    val controle: UuidControle?,
    val termine: Boolean,
    /** Plants dans l'ordre de leurs numéros. */
    val plants: List<PlantSession>,
    val courant: UuidPlant?,
    /** Plant mort ou absent dont le prochain pointage sera le regarni. */
    val regarniDe: UuidPlant?,
    /** Plants dont l'état est dit aujourd'hui (« 7 » de « 7 sur 12 »). */
    val nbDits: Int,
    /** Plants de la placette (« 12 » de « 7 sur 12 »), hors remplacés. */
    val nbPlants: Int,
    val totaux: Verdict<Totaux>,
)

/**
 * La session de contrôle d'une placette, sur le journal de la tournée ouverte (mission 012-A, spec § 4.3).
 *
 * Aucun état n'est gardé en mémoire hormis le plant courant et le regarni en attente : tout le reste est **relu du
 * journal** à chaque appel (état courant des lignes), si bien que l'application peut être fermée et rouverte au
 * milieu d'une placette sans rien perdre. Chaque commande écrit des lignes (`controle`, `pointage`, `etat`) dont la
 * `valeur` suit [FormatValeurs] ; « terminer » réécrit la ligne `controle` avec `termine = true` (la liste fermée
 * des cinq types ne change pas) ; « annuler » annule la dernière ligne annulable.
 *
 * Les méthodes sont bloquantes (écriture du journal) : à appeler hors du fil principal.
 */
class SessionControle private constructor(
    private val contexte: ContexteControle,
    private val journal: JournalControle,
    private val horloge: HorlogeSession,
    private val essences: Essences,
    private val sauvegarde: EcouteSauvegarde,
    private val cible: PlacetteControlable,
    private val rattache: Rattache,
) {
    /** Le rattachement de l'arrivée : piquet, distance, « par identité », position du contrôle et position mesurée. */
    val rattachement: Rattache get() = rattache

    private val placette = Placette(cible.piquet.placette, cible.rayonM)
    private var courant: UuidPlant? = null
    private var regarniEnAttente: Pair<UuidPlant, UuidPlant>? = null // (plant remplacé, UUID du futur regarni)

    init {
        courant = premierSansEtat(lire())
    }

    // ------------------------------------------------------------------ lecture

    /** L'état de la session, relu du journal. */
    fun etat(): EtatSession = lire().let { vue -> instantane(vue) }

    val plantCourant: UuidPlant? get() = courant?.takeIf { c -> lire().plants.any { it.uuid == c } }

    // ------------------------------------------------------------------ commandes

    /**
     * Pointe un plant relativement au piquet : distance **horizontale** en mètres, azimut magnétique lu (avec son unité
     * et sa [source]). Le plant devient le plant courant. Au-delà du rayon + 0,10 m : [SuiteSession.AConfirmer] tant que
     * [confirmeHorsRayon] est faux, rien n'est écrit. Si un « remplace » est en attente, le plant est son regarni.
     */
    fun pointer(
        distanceM: Double,
        azimut: AzimutSaisi,
        source: SourceAzimut,
        essence: String? = null,
        confirmeHorsRayon: Boolean = false,
    ): SuiteSession {
        val vue = lire()
        val codeEssence = essence?.let {
            when (val v = essences.valider(it)) {
                is Verdict.Accepte -> v.valeur
                is Verdict.Refuse -> return SuiteSession.Refus.de(v.refus)
                is Verdict.AConfirmer -> error("pas d'alerte attendue pour une essence")
            }
        }
        val derive: PlantDerive = when (val r = PointDerive.deriver(cible.piquet.position, contexte.epsg, distanceM, azimut, vue.jour)) {
            is Resultat.Accepte -> r.valeur
            is Resultat.Refuse -> return SuiteSession.Refus.de(r.refus)
        }
        val nouveau = regarniEnAttente?.second ?: UuidPlant(journal.nouvelUuid())
        val remplace = regarniEnAttente?.first
        val maintenant = horloge.maintenantUtcMs()
        val plant = when (val v = Pointage.pointer(nouveau, placette, derive, Instant.ofEpochMilli(maintenant), codeEssence, remplace, confirmeHorsRayon)) {
            is Verdict.Accepte -> v.valeur
            is Verdict.AConfirmer -> return SuiteSession.AConfirmer(v.alerte)
            is Verdict.Refuse -> return SuiteSession.Refus.de(v.refus)
        }
        val identites = identites(vue.plants)
        val numeroNouveau = (identites.maxOfOrNull { it.numero } ?: 0) + 1
        val iNouveau = IdentitePlant(nouveau, placette.uuid, numeroNouveau, remplace)
        (Identite.verifier(identites + iNouveau) as? Verdict.Refuse)?.let { return SuiteSession.Refus.de(it.refus) }
        if (remplace != null) {
            (Regarnis.verifier(identites, dernierEtat(vue), iNouveau) as? Verdict.Refuse)?.let { return SuiteSession.Refus.de(it.refus) }
        }
        val controle = assurerControle(vue)
        val ligne = ecrire(
            nouveau.valeur, FormatValeurs.TYPE_POINTAGE,
            ValeurPointage(
                controleUuid = controle.uuid, placetteUuid = placette.uuid.valeur,
                distanceHorizontaleM = derive.distanceHorizontaleM, azimutMag = derive.azimut.valeur,
                uniteAzimut = CodesUniteAzimut.code(derive.azimut.unite), sourceAzimut = source.code,
                declinaisonDeg = derive.orientation.declinaisonDeg, convergenceDeg = derive.orientation.convergenceDeg,
                azimutGeoDeg = derive.orientation.azimutGeographiqueDeg, azimutGrilleDeg = derive.orientation.azimutGrilleDeg,
                jourDeclinaison = derive.date.toString(), x = derive.position.xMetres, y = derive.position.yMetres,
                repereEpsg = contexte.epsg.code, essence = plant.essence?.code, remplaceUuid = remplace?.valeur,
                horsRayonConfirme = confirmeHorsRayon && ControleRayon.verifier(derive.distanceHorizontaleM, placette.rayonM) != null, pointeLeUtcMs = maintenant,
            ).versJson(),
            remplace = null,
        )
        courant = nouveau
        regarniEnAttente = null
        return SuiteSession.Fait(controle.lignes + ligne)
    }

    /** Dit l'état du plant courant, avec, si l'agent le donne, la cause, le détail et la hauteur (en cm). */
    fun dire(etat: String, causeGroupe: String? = null, causeDetail: String? = null, hauteurCm: Int? = null): SuiteSession {
        val plant = plantCourantOuNull() ?: return SuiteSession.AucunPlantCourant
        val observation = when (val v = Observations.valider(etat, causeGroupe, causeDetail, hauteurCm)) {
            is Verdict.Accepte -> v.valeur
            is Verdict.Refuse -> return SuiteSession.Refus.de(v.refus)
            is Verdict.AConfirmer -> error("pas d'alerte attendue pour un état")
        }
        return ecrireObservation(plant, observation)
    }

    /** Plant pointé non retrouvé : ce n'est pas un état, il sort des totaux de ce contrôle (spec § 4.3). */
    fun nonObserve(): SuiteSession = dire(Observations.CODE_NON_OBSERVE)

    /**
     * Cause, détail ou hauteur donnés **juste après** l'état (décision de Pascal du 2026-10-27) : l'état du plant courant
     * est complété, jamais changé. Sans état d'abord : [SuiteSession.EtatDAbord].
     */
    fun preciser(causeGroupe: String? = null, causeDetail: String? = null, hauteurCm: Int? = null): SuiteSession {
        val plant = plantCourantOuNull() ?: return SuiteSession.AucunPlantCourant
        val actuel = lire().plants.first { it.uuid == plant }.duJour ?: return SuiteSession.EtatDAbord
        val etat = (actuel as? Observation.Observe)?.etat?.code ?: Observations.CODE_NON_OBSERVE
        val ancien = actuel as? Observation.Observe
        val observation = when (
            val v = Observations.valider(
                etat, causeGroupe ?: ancien?.causeGroupe?.code, causeDetail ?: ancien?.causeDetail, hauteurCm ?: ancien?.hauteurCm,
            )
        ) {
            is Verdict.Accepte -> v.valeur
            is Verdict.Refuse -> return SuiteSession.Refus.de(v.refus)
            is Verdict.AConfirmer -> error("pas d'alerte attendue pour un état")
        }
        return ecrireObservation(plant, observation)
    }

    /**
     * « remplace » : le plant courant, mort ou absent, est celui que le prochain plant pointé remplace (décision de
     * Pascal du 2026-10-27). Refusé si le plant n'a pas été vu mort ou absent, ou s'il est déjà remplacé.
     */
    fun remplace(): SuiteSession {
        val vue = lire()
        val plant = plantCourantOuNull() ?: return SuiteSession.AucunPlantCourant
        val futur = UuidPlant(journal.nouvelUuid())
        val identites = identites(vue.plants)
        val iNouveau = IdentitePlant(futur, placette.uuid, (identites.maxOfOrNull { it.numero } ?: 0) + 1, plant)
        (Regarnis.verifier(identites, dernierEtat(vue), iNouveau) as? Verdict.Refuse)?.let { return SuiteSession.Refus.de(it.refus) }
        regarniEnAttente = plant to futur
        return SuiteSession.Fait()
    }

    /** Passe au plant suivant sans état dit aujourd'hui, dans l'ordre des numéros ; aucun : plus de plant courant. */
    fun suivant() {
        val plants = lire().plants
        val apres = courant?.let { c -> plants.indexOfFirst { it.uuid == c } }?.takeIf { it >= 0 }
        val ordre = if (apres == null) plants else plants.drop(apres + 1)
        courant = ordre.firstOrNull { it.duJour == null && !it.remplaceAvant }?.uuid
    }

    /** Choisit le plant courant (l'agent touche un plant du plan). Faux si ce plant n'est pas dans la placette. */
    fun choisir(plant: UuidPlant): Boolean {
        if (lire().plants.none { it.uuid == plant }) return false
        courant = plant
        return true
    }

    /** Annule la dernière saisie : un pointage, un état, ou la fin de la placette (qui redevient « à faire »). */
    fun annuler(): SuiteSession {
        val vue = lire()
        val cible = vue.lignes.lastOrNull { l -> annulable(l, vue) } ?: return SuiteSession.RienAAnnuler
        journal.annuler(cible.uuid, contexte.operateur)
        sauvegarde.surSaisie()
        courant = courant?.takeIf { c -> lire().plants.any { it.uuid == c } } ?: premierSansEtat(lire())
        return SuiteSession.Fait(listOf(cible.uuid))
    }

    /**
     * Termine la placette (décision de Pascal du 2026-10-27) : le contrôle devient « Contrôlé », les numéros sont
     * **figés** et la sauvegarde reçoit une fin de placette. Refusé si aucun état n'est dit aujourd'hui, ou si le contrôle
     * du jour est déjà terminé.
     */
    fun terminer(): SuiteSession {
        val vue = lire()
        val ligneControle = vue.controleDuJour
        if (ligneControle?.second?.termine == true) return SuiteSession.DejaTerminee
        if (vue.plants.none { it.duJour != null }) return SuiteSession.RienASaisir
        val (ligne, valeur) = ligneControle ?: error("un état dit suppose un contrôle écrit")
        val figes = when (val n = vue.numeros) {
            is NumerosPlacette.Provisoires -> Numerotation.figer(n).valeurOuEchec()
            is NumerosPlacette.Figes -> n
        }
        val ecrite = ecrire(
            ligne.objetUuid, FormatValeurs.TYPE_CONTROLE,
            valeur.copy(termine = true, numeros = figes.numeros.mapKeys { it.key.valeur }).versJson(),
            remplace = ligne.uuid,
        )
        sauvegarde.surFinPlacette()
        return SuiteSession.Fait(listOf(ecrite))
    }

    /** « vide » : pas encore disponible, rien n'est tracé (décision de Pascal du 2026-10-27). */
    fun vide(): SuiteSession = SuiteSession.PasEncoreDisponible("vide")

    fun finDuVide(): SuiteSession = SuiteSession.PasEncoreDisponible("fin du vide")

    /** Applique un geste des boutons de volume (table [GestesPlacette]). */
    fun appliquer(geste: Geste): SuiteSession = when (val a = GestesPlacette.action(geste)) {
        is ActionGeste.Dire -> dire(a.etat.code).also { if (it is SuiteSession.Fait && a.puisSuivant) suivant() }
        ActionGeste.OuvrirDictee -> SuiteSession.DicteeDemandee
        ActionGeste.AnnulerDerniereSaisie -> annuler()
    }

    // ------------------------------------------------------------------ interne : écriture

    private fun plantCourantOuNull(): UuidPlant? = courant?.takeIf { c -> lire().plants.any { it.uuid == c } }

    private fun ecrire(objet: String, type: String, valeur: String, remplace: String?): String {
        val uuid = journal.saisir(objet, type, valeur, contexte.operateur, remplace, horloge.fix())
        sauvegarde.surSaisie()
        return uuid
    }

    private fun ecrireObservation(plant: UuidPlant, observation: Observation): SuiteSession {
        val vue = lire()
        val controle = assurerControle(vue)
        val controleUuid = controle.uuid
        val existant = vue.etats.firstOrNull { it.second.controleUuid == controleUuid && it.second.plantUuid == plant.valeur }
        val o = observation as? Observation.Observe
        val valeur = ValeurEtat(
            controleUuid, plant.valeur, o?.etat?.code ?: Observations.CODE_NON_OBSERVE,
            o?.causeGroupe?.code, o?.causeDetail, o?.hauteurCm,
        ).versJson()
        val ligne = ecrire(existant?.first?.objetUuid ?: journal.nouvelUuid(), FormatValeurs.TYPE_ETAT, valeur, existant?.first?.uuid)
        return SuiteSession.Fait(controle.lignes + ligne)
    }

    private class ControleAssure(val uuid: String, val lignes: List<String>)

    /** UUID du contrôle du jour (un par placette et par jour civil, [UnControleParJour]) ; l'écrit au premier besoin. */
    private fun assurerControle(vue: Vue): ControleAssure {
        vue.controleDuJour?.let { return ControleAssure(it.first.objetUuid, emptyList()) }
        val uuid = journal.nouvelUuid()
        val e = UnControleParJour.ecrire(vue.existants, UuidControle(uuid), placette.uuid, Instant.ofEpochMilli(vue.instantMs))
        check(e is EcritureControle.Nouveau) { "un contrôle existe déjà ce jour-là" }
        val valeur = ValeurControle(
            placetteUuid = placette.uuid.valeur, codePlacette = cible.piquet.code, jourCivil = vue.jour.toString(),
            operateur = contexte.operateur, termine = false, visiteLeUtcMs = vue.instantMs,
            positionX = rattache.positionControle.xMetres, positionY = rattache.positionControle.yMetres,
            positionMesureeX = rattache.positionMesuree.xMetres, positionMesureeY = rattache.positionMesuree.yMetres,
            rattacheParIdentite = rattache.parIdentite, repereEpsg = contexte.epsg.code,
        )
        val ligne = ecrire(uuid, FormatValeurs.TYPE_CONTROLE, valeur.versJson(), remplace = null)
        return ControleAssure(uuid, listOf(ligne))
    }

    /** Lignes annulables : pointage, état, ou fin de placette (la ligne `controle` réécrite), de **cette** placette. */
    private fun annulable(l: LigneCourante, vue: Vue): Boolean = when (l.type) {
        FormatValeurs.TYPE_POINTAGE -> ValeurPointage.lire(l.valeur)?.placetteUuid == placette.uuid.valeur
        FormatValeurs.TYPE_ETAT -> ValeurEtat.lire(l.valeur)?.controleUuid in vue.controlesParUuid
        FormatValeurs.TYPE_CONTROLE -> l.remplaceUuid != null && ValeurControle.lire(l.valeur)?.let { it.termine && it.placetteUuid == placette.uuid.valeur } == true
        else -> false
    }

    // ------------------------------------------------------------------ interne : lecture du journal

    private class Vue(
        val lignes: List<LigneCourante>,
        val jour: LocalDate,
        val instantMs: Long,
        val controles: List<Pair<LigneCourante, ValeurControle>>,
        val controleDuJour: Pair<LigneCourante, ValeurControle>?,
        val pointages: List<Pair<LigneCourante, ValeurPointage>>,
        val etats: List<Pair<LigneCourante, ValeurEtat>>,
        val plants: List<PlantSession>,
        val numeros: NumerosPlacette,
    ) {
        val existants: List<ControleEcrit> = controles.map { (l, v) ->
            ControleEcrit(UuidControle(l.objetUuid), UuidPlacette(v.placetteUuid), LocalDate.parse(v.jourCivil))
        }
        val controlesParUuid: Map<String, ValeurControle> = controles.associate { it.first.objetUuid to it.second }
    }

    private fun lire(): Vue {
        val maintenant = horloge.maintenantUtcMs()
        val fix = horloge.fix()
        val age = fix?.let { horloge.monotoneMs() - it.monotoneMs }
        val instant = JourCivil.instantRetenuMs(maintenant, fix?.heureUtcMs, age)
        val jour = JourCivil.deSaisie(maintenant, fix?.heureUtcMs, age)
        val lignes = journal.lignesCourantes()
        val miens = placette.uuid.valeur
        val controles = lignes.filter { it.type == FormatValeurs.TYPE_CONTROLE }
            .mapNotNull { l -> ValeurControle.lire(l.valeur)?.takeIf { it.placetteUuid == miens }?.let { l to it } }
        val existants = controles.map { (l, v) -> ControleEcrit(UuidControle(l.objetUuid), placette.uuid, LocalDate.parse(v.jourCivil)) }
        val duJour = (UnControleParJour.ecrire(existants, UuidControle("sans-objet"), placette.uuid, Instant.ofEpochMilli(instant)) as? EcritureControle.Corrige)
            ?.let { c -> controles.first { it.first.objetUuid == c.premier.uuid.valeur } }
        val pointages = lignes.filter { it.type == FormatValeurs.TYPE_POINTAGE }
            .mapNotNull { l -> ValeurPointage.lire(l.valeur)?.takeIf { it.placetteUuid == miens }?.let { l to it } }
        val parControle = controles.associate { it.first.objetUuid to it.second }
        val etats = lignes.filter { it.type == FormatValeurs.TYPE_ETAT }
            .mapNotNull { l -> ValeurEtat.lire(l.valeur)?.takeIf { it.controleUuid in parControle }?.let { l to it } }
        val (plants, numeros) = plants(jour, controles, duJour, pointages, etats, parControle)
        return Vue(lignes, jour, instant, controles, duJour, pointages, etats, plants, numeros)
    }

    private fun observation(v: ValeurEtat): Observation? =
        Observations.valider(v.etat, v.causeGroupe, v.causeDetail, v.hauteurCm).valeurOuNull()

    private fun plants(
        jour: LocalDate,
        controles: List<Pair<LigneCourante, ValeurControle>>,
        duJour: Pair<LigneCourante, ValeurControle>?,
        pointages: List<Pair<LigneCourante, ValeurPointage>>,
        etats: List<Pair<LigneCourante, ValeurEtat>>,
        parControle: Map<String, ValeurControle>,
    ): Pair<List<PlantSession>, NumerosPlacette> {
        val anterieurs = contexte.plantsAnterieurs[placette.uuid].orEmpty()
        // Numéros : ceux du passage précédent, puis ceux figés par un contrôle terminé ; les autres, provisoires
        // (premier contrôle, recalculés) ou « numéro suivant » (placette déjà numérotée), sans renuméroter.
        val base = LinkedHashMap<UuidPlant, Int>()
        anterieurs.forEach { base[it.uuid] = it.numero }
        controles.map { it.second }.filter { it.termine }.forEach { c -> c.numeros?.forEach { (u, n) -> base.putIfAbsent(UuidPlant(u), n) } }
        val aNumeroter = pointages.map { it.first.objetUuid }.map(::UuidPlant).filter { it !in base }
        val derives = pointages.associateBy { UuidPlant(it.first.objetUuid) }
        val numeros: NumerosPlacette = if (base.isEmpty()) {
            Numerotation.provisoires(aNumeroter.map { u -> derives.getValue(u).second.let { PlantANumeroter(u.valeur, it.azimutGeoDeg, it.distanceHorizontaleM) } })
        } else {
            aNumeroter.fold(NumerosPlacette.Figes(base) as NumerosPlacette.Figes) { f, u -> Numerotation.ajouter(f, u).valeurOuEchec() }
        }
        val figes = numeros is NumerosPlacette.Figes
        val duJourUuid = duJour?.first?.objetUuid
        val etatsParPlant = etats.groupBy { it.second.plantUuid }
        fun jourDe(e: ValeurEtat) = LocalDate.parse(parControle.getValue(e.controleUuid).jourCivil)
        val anterieursParUuid = anterieurs.associateBy { it.uuid }

        val poses = ArrayList<RegarniPose>()
        anterieurs.forEach { a -> a.remplace?.let { poses += RegarniPose(a.uuid, it, LocalDate.MIN) } }
        pointages.forEach { (l, v) ->
            v.remplaceUuid?.let { poses += RegarniPose(UuidPlant(l.objetUuid), UuidPlant(it), parControle[v.controleUuid]?.let { c -> LocalDate.parse(c.jourCivil) } ?: jour) }
        }
        val remplacesAvant = Regarnis.remplacesAvant(poses, jour)

        fun session(uuid: UuidPlant): PlantSession {
            val mes = etatsParPlant[uuid.valeur].orEmpty()
            val duJourObs = mes.firstOrNull { it.second.controleUuid == duJourUuid }?.let { observation(it.second) }
            val precedentJournal = mes.filter { jourDe(it.second).isBefore(jour) }
                .maxWithOrNull(compareBy({ jourDe(it.second) }, { etats.indexOf(it) }))?.let { observation(it.second) }
            val a = anterieursParUuid[uuid]
            val p = derives[uuid]?.second
            return PlantSession(
                uuid = uuid, numero = numeros.numeros.getValue(uuid), numeroFige = figes || a != null,
                essence = p?.essence ?: a?.essence, remplace = p?.remplaceUuid?.let(::UuidPlant) ?: a?.remplace,
                distanceHorizontaleM = p?.distanceHorizontaleM, azimutGeographiqueDeg = p?.azimutGeoDeg,
                position = p?.let { PointLambert93(it.x, it.y) }, duJour = duJourObs,
                precedent = precedentJournal ?: a?.observation, remplaceAvant = uuid in remplacesAvant,
            )
        }
        val tous = (anterieurs.map { it.uuid } + pointages.map { UuidPlant(it.first.objetUuid) }).distinct()
        return tous.map(::session).sortedBy { it.numero } to numeros
    }

    private fun identites(plants: List<PlantSession>) =
        plants.map { IdentitePlant(it.uuid, placette.uuid, it.numero, it.remplace) }

    /** Dernier état observé d'un plant, au plus tard aujourd'hui (non observés ignorés), pour [Regarnis.verifier]. */
    private fun dernierEtat(vue: Vue): (UuidPlant) -> EtatDsf? = { uuid ->
        val historique = vue.etats.filter { it.second.plantUuid == uuid.valeur }.mapNotNull { (_, v) ->
            observation(v)?.let { ObservationDatee(LocalDate.parse(vue.controlesParUuid.getValue(v.controleUuid).jourCivil), it) }
        } + listOfNotNull(
            contexte.plantsAnterieurs[placette.uuid]?.firstOrNull { it.uuid == uuid }?.observation?.let { ObservationDatee(LocalDate.MIN, it) },
        )
        Regarnis.dernierEtatObserve(historique, vue.jour)
    }

    private fun instantane(vue: Vue): EtatSession {
        val etatsDuJour = vue.plants.mapNotNull { p -> p.duJour?.let { EtatPlant(p.uuid, it) } }
        val remplaces = vue.plants.filter { it.remplaceAvant }.map { it.uuid }.toSet()
        val comptes = vue.plants.filter { !it.remplaceAvant }
        return EtatSession(
            placette = placette.uuid, codePlacette = cible.piquet.code, jour = vue.jour,
            controle = vue.controleDuJour?.first?.objetUuid?.let(::UuidControle), termine = vue.controleDuJour?.second?.termine == true,
            plants = vue.plants, courant = courant?.takeIf { c -> vue.plants.any { it.uuid == c } }, regarniDe = regarniEnAttente?.first,
            nbDits = comptes.count { it.duJour != null }, nbPlants = comptes.size,
            totaux = Totaux.deduire(etatsDuJour, remplaces),
        )
    }

    private fun premierSansEtat(vue: Vue): UuidPlant? = vue.plants.firstOrNull { it.duJour == null && !it.remplaceAvant }?.uuid

    companion object {
        /**
         * Ouvre la session d'une placette à l'arrivée de l'agent : position testée **en France** (en degrés, avant toute
         * projection), puis rattachée à un piquet à 15 m au plus, ou à celui dont l'agent a lu le [codeLu] (le code lu
         * l'emporte sur le GNSS). Un fix hors de France, un piquet inconnu ou une position hors de l'emprise de la forêt :
         * [OuvertureControle.Refusee] avec son code, aucune ligne n'est écrite.
         */
        fun ouvrir(
            contexte: ContexteControle,
            journal: JournalControle,
            horloge: HorlogeSession,
            essences: Essences,
            arrivee: Arrivee,
            codeLu: String? = null,
            sauvegarde: EcouteSauvegarde = EcouteSauvegarde.AUCUNE,
        ): OuvertureControle {
            val mesuree: PointLambert93 = when (arrivee) {
                is Arrivee.Gnss -> {
                    val c = EmpriseEtFrance.controler(arrivee.position)
                    if (!c.enFrance) {
                        return OuvertureControle.Refusee(
                            horsFrance("latitude ${RefusRepere.dit(arrivee.position.latitudeDeg)}, longitude ${RefusRepere.dit(arrivee.position.longitudeDeg)} (degrés)", c.inversionDetectee),
                        )
                    }
                    EmpriseEtFrance.versLambert93(arrivee.position)
                }
                is Arrivee.Lambert93 -> {
                    val c = EmpriseEtFrance.controler(arrivee.position)
                    if (!c.enFrance) {
                        return OuvertureControle.Refusee(
                            horsFrance("x = ${RefusRepere.ditMetres(arrivee.position.xMetres)}, y = ${RefusRepere.ditMetres(arrivee.position.yMetres)} (Lambert-93, mètres)", c.inversionDetectee),
                        )
                    }
                    arrivee.position
                }
            }
            val rattache = when (val v = Rattachement.rattacher(contexte.placettes.map { it.piquet }, mesuree, codeLu)) {
                is Verdict.Accepte -> v.valeur
                is Verdict.Refuse -> return OuvertureControle.Refusee(SuiteSession.Refus.de(v.refus))
                is Verdict.AConfirmer -> error("pas d'alerte attendue au rattachement")
            }
            val foret = contexte.empriseForet
            if (foret != null && !foret.contient(rattache.positionControle)) {
                return OuvertureControle.Refusee(
                    SuiteSession.Refus(
                        CodesRefus.POINT_HORS_FORET, CodesRefus.POINT_HORS_FORET,
                        "Position du contrôle (x = ${RefusRepere.ditMetres(rattache.positionControle.xMetres)}, y = ${RefusRepere.ditMetres(rattache.positionControle.yMetres)}, Lambert-93) hors de l'emprise de la forêt.",
                    ),
                )
            }
            val cible = contexte.placettes.first { it.piquet.placette == rattache.piquet.placette }
            return OuvertureControle.Ouverte(SessionControle(contexte, journal, horloge, essences, sauvegarde, cible, rattache))
        }

        private fun horsFrance(position: String, axesInverses: Boolean) = SuiteSession.Refus(
            CodesRefus.HORS_FRANCE, CodesRefus.HORS_FRANCE,
            "Position hors de France : $position." + if (axesInverses) " Une fois les axes permutés, elle tombe en France : axes inversés." else "",
            axesInverses,
        )
    }
}
