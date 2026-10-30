/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import io.github.pobsteta.surculus.core.placette.EtatDsf
import io.github.pobsteta.surculus.core.placette.GroupeCause
import io.github.pobsteta.surculus.core.placette.Observation
import io.github.pobsteta.surculus.core.placette.Totaux
import io.github.pobsteta.surculus.core.placette.Verdict
import io.github.pobsteta.surculus.core.session.ActionGeste
import io.github.pobsteta.surculus.core.session.ApplicateurDictee
import io.github.pobsteta.surculus.core.session.EtatSession
import io.github.pobsteta.surculus.core.session.Geste
import io.github.pobsteta.surculus.core.session.GestesPlacette
import io.github.pobsteta.surculus.core.session.IssueDictee
import io.github.pobsteta.surculus.core.session.PlantSession
import io.github.pobsteta.surculus.core.placette.UuidPlant
import io.github.pobsteta.surculus.core.reperes.AzimutSaisi
import io.github.pobsteta.surculus.core.geo.UniteAzimut
import io.github.pobsteta.surculus.core.guidage.Libelles
import io.github.pobsteta.surculus.core.session.SessionControle
import io.github.pobsteta.surculus.core.session.SourceAzimut
import io.github.pobsteta.surculus.core.session.SuiteSession
import io.github.pobsteta.surculus.core.session.TextesDictee
import java.util.Locale
import kotlin.math.roundToInt

/** Rayon supposé quand l'appelant n'en donne pas (tests) : celui des placettes de Couchey. */
const val RAYON_PAR_DEFAUT_M = 3.99

/** Les vues de l'écran du contrôle (règle du 1664 : une idée par vue). */
enum class VueControle {
    /** Le plant courant et son état. */
    PLANT,

    /** Confirmer la fin avec les totaux. */
    TERMINER,

    /** La placette vient d'être terminée : « Contrôlée ». */
    TERMINEE,

    /** Placer un nouveau plant : cap de la boussole (ou clavier) et distance (mission 012-B2). */
    POINTER,
}

/** Message à l'écran : une information, ou un refus (en couleur d'erreur, avec son code quand il en a un). */
data class MessageControle(val texte: String, val erreur: Boolean)

/** Ce que l'écran montre du plant courant, en Kotlin pur (testé sans Compose). */
data class VuePlant(
    /** « Plant n° 7 », « Tous les plants sont dits » ou « Aucun plant ». */
    val titre: String,
    val numero: Int?,
    /** « 7 sur 12 ». */
    val compteur: String,
    /** État dit aujourd'hui ; `null` : « pas encore dit ». */
    val etatDuJour: EtatDsf?,
    /** « non observé » (pas un état du DSF) : l'état n'est pas dit, mais le plant n'attend plus. */
    val nonObserve: Boolean,
    val groupeCause: GroupeCause?,
    /** « pas encore dit », ou le libellé de l'état du jour. */
    val libelleEtat: String,
    /** « avant : atteint », ou `null` sans état précédent. */
    val precedent: String?,
    /** Les groupes de cause s'offrent sur un plant atteint ou mort. */
    val causesVisibles: Boolean,
    val alerteTerminee: Boolean,
    val aucunPlant: Boolean,
    /** Le plant courant est mort ou absent aujourd'hui : « Remplacer » (regarni) se propose. La session refuse le reste. */
    val regarniPossible: Boolean,
    val aPrecedent: Boolean,
    val aSuivant: Boolean,
)

/** Totaux à confirmer à la fin : « 12 plants · 9 vivants · 2 abroutis · hauteur moyenne 78 cm ». */
data class VueTotaux(val texte: String, val refus: String?)

/** La vue Pointer : le regarni en attente, ou le pointage hors rayon à confirmer (jamais un refus : rien n'est écrit). */
data class VuePointage(
    /** Numéro du plant que le prochain pointage remplace ; `null` : un plant ordinaire. */
    val regarniDe: Int?,
    /** Regarni : l'essence qui sera écrite (celle du remplacé par défaut), `null` : aucune. */
    val essenceChoisie: String? = null,
    /** Regarni : les essences de la tournée offertes au choix ; vide pour un plant ordinaire. */
    val essencesTournee: List<String> = emptyList(),
    /** « Plant à 22 m, au-delà de… » : le texte de l'alerte de la session, avec « Confirmer » et « Refaire ». */
    val aConfirmer: String?,
)

/** Tout ce que l'écran lit : fonction pure de l'état de la session, de la vue et du message. */
data class EcranControle(
    val codePlacette: String,
    val jour: String,
    val vue: VueControle,
    val plant: VuePlant,
    val totaux: VueTotaux,
    val message: MessageControle?,
    val pointage: VuePointage,
    val plan: PlanPlants,
)

/**
 * Pilote de l'écran du contrôle (mission 012-B1) : traduit un appui en commande de la [SessionControle] et rend ce que
 * l'écran montre. Aucune règle ici : états, causes, refus, « terminer » viennent de la session (012-A) ; ce pilote ne
 * garde que la vue ouverte et le dernier message. Méthodes bloquantes (journal) : hors du fil principal.
 */
class ControleurPlacette(
    private val session: SessionControle,
    private val rayonM: Double = RAYON_PAR_DEFAUT_M,
    /** Les essences de la tournée (codes déjà portés par ses plants) : seules offertes au regarni. */
    private val essencesTournee: List<String> = emptyList(),
) {
    private var vue = VueControle.PLANT
    private var message: MessageControle? = null

    /** Ce que la voix et la vibration disent du dernier geste de volume (012-C2) ; `null` avant tout geste. */
    var derniereAnnonce: AnnoncePoche? = null
        private set
    private var enAttente: PointageEnAttente? = null
    /** Essence du regarni en attente : celle du remplacé au départ, changée par le forestier ; `null` : aucune. */
    private var essenceRegarni: String? = null

    /** Pointage hors rayon en attente de « Confirmer » ou « Refaire » : rien n'est écrit tant que l'agent n'a pas répondu. */
    private data class PointageEnAttente(val distanceM: Double, val azimut: AzimutSaisi, val source: SourceAzimut, val alerte: String)

    fun instantane(): EcranControle {
        val etat = session.etat()
        val regarni = etat.regarniDe != null
        return TextesControle.ecran(
            etat, vue, message, enAttente?.alerte, PlanPlants.depuis(etat, session.rattachement.piquet.position, rayonM),
            essenceRegarni.takeIf { regarni }, if (regarni) essencesTournee else emptyList(),
        )
    }

    // ------------------------------------------------------------------ pointer (012-B2)

    /** Ouvre la vue Pointer. */
    fun ouvrirPointer(): EcranControle { vue = VueControle.POINTER; message = null; enAttente = null; return instantane() }

    /** Pointe au cap lu par la boussole du téléphone (magnétique pur) ; sans cap, on le dit, rien n'est écrit. */
    fun pointerAvecBoussole(distanceTexte: String, cap: CapMagnetique?): EcranControle {
        if (cap == null) return refuser(MessageControle(TextesControle.SANS_CAP, erreur = true))
        return pointer(distanceTexte, SaisiePointage.azimutBoussole(cap), SourceAzimut.BOUSSOLE_TELEPHONE)
    }

    /** Pointe au cap tapé (secours), dans [unite] : hors plage, refusé avec son code. */
    fun pointerAuClavier(distanceTexte: String, azimutTexte: String, unite: UniteAzimut): EcranControle =
        when (val a = SaisiePointage.azimut(azimutTexte, unite)) {
            is LectureSaisie.Valeur -> pointer(distanceTexte, a.valeur, SourceAzimut.SAISIE)
            is LectureSaisie.Illisible -> refuser(MessageControle(a.message, erreur = true))
            is LectureSaisie.Refus -> refuser(TextesControle.message(SuiteSession.Refus.de(a.refus))!!)
        }

    private fun pointer(distanceTexte: String, azimut: AzimutSaisi, source: SourceAzimut): EcranControle =
        when (val d = SaisiePointage.distance(distanceTexte)) {
            is LectureSaisie.Valeur -> tenter(d.valeur, azimut, source, confirme = false)
            is LectureSaisie.Illisible -> refuser(MessageControle(d.message, erreur = true))
            is LectureSaisie.Refus -> refuser(TextesControle.message(SuiteSession.Refus.de(d.refus))!!)
        }

    /** « Confirmer » : le plant hors rayon est écrit, marqué confirmé (`hors_rayon_confirme`). */
    fun confirmerHorsRayon(): EcranControle {
        val a = enAttente ?: return instantane()
        return tenter(a.distanceM, a.azimut, a.source, confirme = true)
    }

    /** « Refaire » : rien n'avait été écrit ; on revient aux champs. */
    fun refaire(): EcranControle { enAttente = null; message = null; return instantane() }

    private fun tenter(distanceM: Double, azimut: AzimutSaisi, source: SourceAzimut, confirme: Boolean): EcranControle {
        // Un regarni porte l'essence proposée (celle du remplacé, ou celle choisie) ; la session la valide. Un plant ordinaire n'en porte pas.
        val essence = if (session.etat().regarniDe != null) essenceRegarni else null
        when (val suite = session.pointer(distanceM, azimut, source, essence, confirme)) {
            is SuiteSession.Fait -> {
                enAttente = null
                essenceRegarni = null
                vue = VueControle.PLANT
                val apres = session.etat()
                val numero = apres.plants.firstOrNull { it.uuid == apres.courant }?.numero
                message = MessageControle(TextesControle.pointe(numero, azimut.degres, distanceM), erreur = false)
            }
            is SuiteSession.AConfirmer -> {
                enAttente = PointageEnAttente(distanceM, azimut, source, suite.alerte.message)
                vue = VueControle.POINTER
                message = null
            }
            else -> refuser(TextesControle.message(suite)!!)
        }
        return instantane()
    }

    private fun refuser(m: MessageControle): EcranControle { enAttente = null; vue = VueControle.POINTER; message = m; return instantane() }

    /** « Remplacer » le plant courant (mort ou absent) : le prochain pointage est son regarni ; la session refuse un plant sain. */
    fun regarni(): EcranControle {
        val suite = session.remplace()
        if (suite is SuiteSession.Fait) {
            vue = VueControle.POINTER; message = null; enAttente = null
            // Par défaut, l'essence du plant remplacé (décision de Pascal, 2026-10-28) ; modifiable par choisirEssence.
            val etat = session.etat()
            essenceRegarni = etat.regarniDe?.let { r -> etat.plants.firstOrNull { it.uuid == r }?.essence?.takeIf(String::isNotBlank) }
        } else message = TextesControle.message(suite)
        return instantane()
    }

    /** Regarni en attente : change l'essence d'un geste (`null` : aucune). Sans regarni en attente, sans effet. La session refuse un code étranger à la tournée. */
    fun choisirEssence(code: String?): EcranControle {
        if (session.etat().regarniDe != null) essenceRegarni = code
        return instantane()
    }

    /** L'agent touche un plant du plan : il devient le plant courant. */
    fun choisir(plant: UuidPlant): EcranControle { session.choisir(plant); message = null; return instantane() }

    /** Dit l'état du plant courant. Sain et absent n'ont pas de cause : on passe au plant suivant (comme les boutons de volume, spec § 4.3). */
    fun dire(etat: EtatDsf): EcranControle = agir {
        val suite = session.dire(etat.code)
        if (suite is SuiteSession.Fait && (etat == EtatDsf.SAIN || etat == EtatDsf.ABSENT)) session.suivant()
        suite
    }

    /** Cause du plant atteint ou mort, juste après son état, puis plant suivant. */
    fun cause(groupe: GroupeCause): EcranControle = agir {
        val suite = session.preciser(causeGroupe = groupe.code)
        if (suite is SuiteSession.Fait) session.suivant()
        suite
    }

    /** Plant atteint ou mort sans cause : plant suivant. */
    fun sansCause(): EcranControle = agir { session.suivant(); SuiteSession.Fait() }

    fun plantSuivant(): EcranControle = agir {
        val e = session.etat()
        val i = e.plants.indexOfFirst { it.uuid == e.courant }
        e.plants.getOrNull(i + 1)?.let { session.choisir(it.uuid) }
        SuiteSession.Fait()
    }

    fun plantPrecedent(): EcranControle = agir {
        val e = session.etat()
        val i = e.plants.indexOfFirst { it.uuid == e.courant }
        val avant = if (i < 0) e.plants.lastOrNull() else e.plants.getOrNull(i - 1)
        avant?.let { session.choisir(it.uuid) }
        SuiteSession.Fait()
    }

    fun annuler(): EcranControle = agir { session.annuler() }

    /**
     * Applique un geste des boutons de volume (mission 012-C1, spec § 4.3, table [GestesPlacette]) par la session, et dit à
     * l'écran ce qui a été fait : « Dit : n° 7, sain. » Le numéro est celui du plant **dit**, lu avant l'écriture (sain et mort
     * passent ensuite au suivant). Après « Terminer », la saisie est permise et l'alerte passe **avant** le numéro (décision de
     * Pascal du 2026-10-28).
     */
    fun geste(geste: Geste): EcranControle {
        val avant = session.etat()
        val plant = avant.plants.firstOrNull { it.uuid == avant.courant }
        val suite = session.appliquer(geste)
        val action = GestesPlacette.action(geste)
        message = if (suite is SuiteSession.Fait) when (action) {
            is ActionGeste.Dire -> {
                val dit = TextesControle.dit(plant?.numero, action.etat)
                val m = if (avant.termine) TextesControle.DEJA_TERMINEE_PREFIXE + dit else dit
                MessageControle(m, erreur = avant.termine)
            }
            ActionGeste.AnnulerDerniereSaisie -> MessageControle(TextesControle.ANNULE, erreur = false)
            // Chemin mort depuis la 012-D2 : le long bas ouvre le micro dans l'écran, il n'arrive plus ici. Gardé pour la table de la spec.
            ActionGeste.OuvrirDictee -> null
        } else TextesControle.message(suite)
        val apres = session.etat()
        derniereAnnonce = AnnoncesPoche.pourSuite(
            geste, suite, plant?.numero, apres.plants.firstOrNull { it.uuid == apres.courant }?.numero,
            memePlant = apres.courant == avant.courant, termine = avant.termine && apres.termine,
        )
        return instantane()
    }

    /**
     * Applique une phrase dictée (mission 012-D2) : [ApplicateurDictee] confronte la phrase à l'état du plant courant et la fait
     * écrire par la session (aucune règle ici). L'écran répète le texte que la voix dit ; le refus de la session garde son code
     * (spec § 6) à l'écran, jamais à la voix. Méthode bloquante (journal) : hors du fil principal.
     */
    fun dicter(phrase: String): EcranControle {
        val issue = ApplicateurDictee.appliquer(phrase, session)
        val apres = session.etat()
        val etat = (issue as? IssueDictee.Ecrite)?.let { e ->
            (apres.plants.firstOrNull { it.numero == e.numero }?.duJour as? Observation.Observe)?.etat
        }
        val codee = (issue as? IssueDictee.Refusee)?.refusSession?.let(TextesControle::message)
        message = if (codee != null) {
            codee.copy(texte = (if (issue.placetteTerminee) TextesControle.DEJA_TERMINEE_PREFIXE else "") + codee.texte)
        } else {
            MessageControle(issue.annonce, erreur = issue is IssueDictee.Refusee || issue.placetteTerminee)
        }
        derniereAnnonce = AnnoncesPoche.pourIssue(issue, etat)
        return instantane()
    }

    /** Les deux touches de volume ensemble : ignorées, on le dit (rien n'est écrit). */
    fun deuxTouches(): EcranControle {
        message = MessageControle(TextesControle.UNE_TOUCHE_A_LA_FOIS, erreur = true)
        derniereAnnonce = AnnoncesPoche.deuxTouches()
        return instantane()
    }

    /** Une annonce du mode poche (sortie, rappel) : information à l'écran, jamais une faute. */
    fun annoncer(texte: String): EcranControle { message = MessageControle(texte, erreur = false); return instantane() }

    /**
     * Ouvre la vue de confirmation. Aucune règle ici : « Terminer » sans état dit est refusé par la session seule
     * (`RienASaisir`), au moment de terminer, et l'écran répète sa réponse (décision de Pascal sur la #108, 2026-10-28).
     */
    fun demanderTerminer(): EcranControle { message = null; vue = VueControle.TERMINER; return instantane() }

    fun terminer(): EcranControle {
        when (val suite = session.terminer()) {
            is SuiteSession.Fait -> { vue = VueControle.TERMINEE; message = null }
            else -> { vue = VueControle.PLANT; message = TextesControle.message(suite) }
        }
        return instantane()
    }

    /** Revient à la vue Plant (« Reprendre », ou retour depuis Pointer). */
    fun reprendre(): EcranControle { vue = VueControle.PLANT; message = null; enAttente = null; return instantane() }

    private fun agir(commande: () -> SuiteSession): EcranControle {
        message = TextesControle.message(commande())
        return instantane()
    }
}

/** Les mots de l'écran et la lecture de l'état de la session : fonctions pures. */
object TextesControle {
    fun libelle(etat: EtatDsf): String = when (etat) {
        EtatDsf.SAIN -> "Sain"
        EtatDsf.ATTEINT -> "Atteint"
        EtatDsf.MORT -> "Mort"
        EtatDsf.ABSENT -> "Absent"
    }

    /**
     * Libellés courts des quatre groupes de cause du DSF (`etats-plants.yaml`, `causes_dsf_groupes_bilan`) : **le même mot que
     * la voix** (`TextesDictee.cause`, mission 012-D2), avec une majuscule.
     */
    fun libelle(groupe: GroupeCause): String = TextesDictee.cause(groupe).replaceFirstChar { it.titlecase(Locale.FRANCE) }

    const val ANNULE = "Annulé."
    const val UNE_TOUCHE_A_LA_FOIS = "Une touche à la fois."
    const val DEJA_TERMINEE_PREFIXE = "Placette déjà terminée. "

    /** « Dit : n° 7, sain. » : ce que le geste vient d'écrire (le numéro dit, pas le suivant). */
    fun dit(numero: Int?, etat: EtatDsf): String = "Dit : n° ${numero ?: "?"}, ${libelle(etat).lowercase(Locale.FRANCE)}."

    /** Pourquoi le mode poche s'est levé, pour qui revient à l'écran ; `null` : la sortie se voit déjà (Terminer, autre vue). */
    fun sortiePoche(raison: RaisonSortie): String? = when (raison) {
        RaisonSortie.TROIS_SECONDES -> "Mode poche terminé."
        RaisonSortie.INACTIVITE -> "Mode poche terminé : 15 minutes sans geste."
        RaisonSortie.ARRIERE_PLAN -> "Mode poche terminé : l'application a quitté l'écran."
        RaisonSortie.TERMINER, RaisonSortie.VUE_QUITTEE -> null
    }

    const val PAS_ENCORE_DIT = "pas encore dit"
    const val NON_OBSERVE = "non observé"

    fun compteur(dits: Int, total: Int) = "$dits sur $total"

    /** Le bouton sous « Code lu sur le piquet » suit le champ (mission 068, décision de Pascal) : « Rattacher » dès qu'un code est lu (non vide après `trim`), « Recommencer » sinon. Même action dans les deux cas. */
    fun boutonArrivee(code: String): String = if (code.isBlank()) "Recommencer" else "Rattacher"

    /** Le refus en clair, sans son motif (il reste dans les données, le journal et les tests : mission 068) ; les autres réponses aussi. */
    fun message(suite: SuiteSession): MessageControle? = when (suite) {
        is SuiteSession.Fait -> null
        is SuiteSession.Refus -> MessageControle(suite.message, erreur = true)
        is SuiteSession.AConfirmer -> MessageControle(suite.alerte.message, erreur = true)
        is SuiteSession.PasEncoreDisponible -> MessageControle("« ${suite.fonction} » : pas encore disponible.", erreur = false)
        SuiteSession.AucunPlantCourant -> MessageControle("Aucun plant à qui dire un état.", erreur = true)
        SuiteSession.EtatDAbord -> MessageControle("L'état d'abord : dites l'état du plant avant sa cause.", erreur = true)
        SuiteSession.RienAAnnuler -> MessageControle("Rien à annuler.", erreur = false)
        SuiteSession.RienASaisir -> MessageControle("Aucun état dit aujourd'hui : rien à terminer.", erreur = true)
        SuiteSession.DejaTerminee -> MessageControle("Placette déjà terminée aujourd'hui.", erreur = true)
        SuiteSession.DicteeDemandee -> MessageControle("La dictée n'est pas encore disponible.", erreur = false)
    }

    const val SANS_CAP = "Pas de cap : la boussole ne répond pas. Saisissez le cap au clavier."

    /** « Plant n° 7 pointé : 143°, 6,2 m » (virgule décimale française). */
    fun pointe(numero: Int?, azimutDeg: Double, distanceM: Double): String {
        return "Plant n° ${numero ?: "?"} pointé : ${azimutDeg.roundToInt()}°, ${Libelles.decimetres(distanceM)} m"
    }

    fun ecran(
        e: EtatSession, vue: VueControle, message: MessageControle?, aConfirmer: String? = null, plan: PlanPlants,
        essenceChoisie: String? = null, essencesTournee: List<String> = emptyList(),
    ): EcranControle =
        EcranControle(
            e.codePlacette, e.jour.toString(), vue, plant(e), totaux(e.totaux), message,
            VuePointage(regarniDe = e.regarniDe?.let { r -> e.plants.firstOrNull { it.uuid == r }?.numero }, aConfirmer = aConfirmer, essenceChoisie = essenceChoisie, essencesTournee = essencesTournee),
            plan,
        )

    fun plant(e: EtatSession): VuePlant {
        val courant: PlantSession? = e.plants.firstOrNull { it.uuid == e.courant }
        val i = e.plants.indexOfFirst { it.uuid == e.courant }
        val duJour = courant?.duJour
        val observe = duJour as? Observation.Observe
        val titre = when {
            e.plants.isEmpty() -> "Aucun plant"
            courant == null -> "Tous les plants sont dits"
            else -> "Plant n° ${courant.numero}"
        }
        return VuePlant(
            titre = titre,
            numero = courant?.numero,
            compteur = compteur(e.nbDits, e.nbPlants),
            etatDuJour = observe?.etat,
            nonObserve = duJour is Observation.NonObserve,
            groupeCause = observe?.causeGroupe,
            libelleEtat = when {
                observe != null -> libelle(observe.etat)
                duJour is Observation.NonObserve -> NON_OBSERVE
                else -> PAS_ENCORE_DIT
            },
            precedent = courant?.precedent?.let { "avant : " + libellePrecedent(it) },
            causesVisibles = observe != null && (observe.etat == EtatDsf.ATTEINT || observe.etat == EtatDsf.MORT),
            alerteTerminee = e.termine,
            aucunPlant = e.plants.isEmpty(),
            regarniPossible = observe != null && (observe.etat == EtatDsf.MORT || observe.etat == EtatDsf.ABSENT),
            aPrecedent = e.plants.isNotEmpty() && i != 0,
            aSuivant = e.plants.isNotEmpty() && i != e.plants.lastIndex,
        )
    }

    private fun libellePrecedent(o: Observation): String = when (o) {
        is Observation.Observe -> libelle(o.etat).lowercase()
        Observation.NonObserve -> NON_OBSERVE
    }

    fun totaux(v: Verdict<Totaux>): VueTotaux = when (v) {
        is Verdict.Accepte -> {
            val t = v.valeur
            val base = "${t.nbTotal} ${if (t.nbTotal > 1) "plants" else "plant"} · ${t.nbVivants} ${if (t.nbVivants > 1) "vivants" else "vivant"} · " +
                "${t.nbAbroutis} ${if (t.nbAbroutis > 1) "abroutis" else "abrouti"}"
            VueTotaux(t.hMoyCm?.let { "$base · hauteur moyenne $it cm" } ?: base, null)
        }
        is Verdict.Refuse -> VueTotaux("", v.refus.message)
        is Verdict.AConfirmer -> VueTotaux("", v.alerte.message)
    }
}
