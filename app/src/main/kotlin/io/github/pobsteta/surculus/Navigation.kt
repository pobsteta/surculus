package io.github.pobsteta.surculus

import io.github.pobsteta.surculus.core.guidage.Guidage

/** Écrans de premier niveau : la liste des tournées est la racine (mission 040). */
enum class Destination { TOURNEES, PARAMETRES, ESSAI, RESTAURATION }

/** Entrées du menu « ⋮ » de l'accueil (missions 046 et 054) : les deux ouvertures d'abord, comme l'« Importer » de Marculus. */
enum class EntreeMenu { OUVRIR_DOSSIER, OUVRIR_ZIP, PARAMETRES, A_PROPOS }

val ENTREES_MENU: List<EntreeMenu> = listOf(EntreeMenu.OUVRIR_DOSSIER, EntreeMenu.OUVRIR_ZIP, EntreeMenu.PARAMETRES, EntreeMenu.A_PROPOS)

/**
 * État de navigation en Kotlin pur (testable sans Compose).
 * Racine : la liste des tournées. Paramètres s'ouvre depuis le menu à trois points (mission 046) ; Essai GNSS et
 * Restauration s'ouvrent depuis Paramètres et y ramènent.
 * Mission 061, comme Groma : une tournée ouverte donne d'abord ses **placettes** ; la carte s'ouvre depuis une placette
 * ([centreId]) ou l'icône carte (cadrée sur la tournée) ; une placette choisie sur la carte donne le guidage.
 * Retours : Guidage, Carte, Placettes, Tournées.
 */
data class EtatNavigation(
    val destination: Destination = Destination.TOURNEES,
    val tourneeId: String? = null,
    val placetteId: String? = null,
    /** Vrai quand la carte de la tournée est ouverte (sinon, tournée ouverte = écran « Placettes »). */
    val carte: Boolean = false,
    /** Placette sur laquelle la carte est centrée à l'ouverture ; `null` : carte cadrée sur toute la tournée. */
    val centreId: String? = null,
    /**
     * Le récepteur GNSS est lancé dans cette tournée (décision de Pascal, 2026-10-27, comme Groma : `EcranCarte.kt` l. 207 le
     * démarre, `MainActivity.kt` l. 92-93 ne l'arrête qu'en sortant du projet). Posé à la première carte ou au premier
     * guidage, gardé en revenant aux placettes, levé en revenant à l'accueil.
     */
    val recepteurOuvert: Boolean = false,
    /**
     * Placette dont le contrôle est ouvert (mission 012-B1). Distinct de [placetteId], qui veut dire « guidage vers » :
     * ouvert depuis le guidage, le retour revient au guidage ([placetteId] reste posé) ; ouvert depuis la carte, à la carte.
     */
    val controleId: String? = null,
    /**
     * Le contrôle s'est déjà ouvert à l'arrivée (moins de 30 m) : plus d'ouverture automatique tant que l'agent n'est pas
     * ressorti à [Guidage.SEUIL_REARMEMENT_M] (40 m). Évite la réouverture en boucle au retour au guidage, dont
     * l'écran est recomposé à neuf. Sauvegardé avec l'état (survit à la rotation).
     */
    val arriveeVue: Boolean = false,
) {
    val estRacine: Boolean get() = destination == Destination.TOURNEES && tourneeId == null

    /** Vrai tant que la carte, le guidage ou l'essai GNSS est ouvert : le récepteur y tourne (mission 047), pas ailleurs, ni sur « Placettes » (décision de Pascal, 2026-10-27, comme Groma). */
    val recepteurDemande: Boolean get() = (tourneeId != null && recepteurOuvert) || destination == Destination.ESSAI

    fun ouvrirParametres() = EtatNavigation(Destination.PARAMETRES)
    fun ouvrirEssai() = EtatNavigation(Destination.ESSAI)
    fun ouvrirRestauration() = EtatNavigation(Destination.RESTAURATION)
    /** Ouvre la tournée sur ses placettes. */
    fun ouvrirTournee(id: String) = EtatNavigation(Destination.TOURNEES, tourneeId = id)

    /** Ouvre la carte de la tournée ouverte : centrée sur [centre], ou cadrée sur la tournée si `null`. */
    fun ouvrirCarte(centre: String? = null) = if (tourneeId == null) this else copy(carte = true, centreId = centre, placetteId = null, recepteurOuvert = true)

    /** Lance le guidage vers [placette] depuis la carte de la tournée. */
    fun guider(placette: String) = if (tourneeId == null) this else copy(carte = true, placetteId = placette, recepteurOuvert = true, arriveeVue = false)

    /** Porte n° 1 : « Contrôler » sur la carte, la placette choisie [id] (la carte reste dessous, recentrée sur elle). */
    fun controler(id: String) = if (tourneeId == null) this else copy(carte = true, placetteId = null, centreId = id, controleId = id, recepteurOuvert = true)

    /** Porte n° 2 : à l'arrivée du guidage (ou son bouton « Contrôler ») ; le guidage reste dessous, l'arrivée est notée vue. */
    fun controlerDepuisGuidage() = if (tourneeId == null || placetteId == null) this else copy(controleId = placetteId, arriveeVue = true)

    /** Vrai si le guidage doit ouvrir le contrôle tout seul à cette distance (mètres) : dans le cercle, et pas déjà ouvert à cette arrivée. */
    fun ouvreALArrivee(distanceM: Double): Boolean = placetteId != null && controleId == null && !arriveeVue && Guidage.estArrive(distanceM)

    /** Réarme l'ouverture automatique une fois l'agent à [Guidage.SEUIL_REARMEMENT_M] ou plus ; sinon l'état est inchangé. */
    fun apresDistance(distanceM: Double): EtatNavigation =
        if (arriveeVue && distanceM >= Guidage.SEUIL_REARMEMENT_M) copy(arriveeVue = false) else this

    /** Un pas en arrière ; `null` = on est à la racine, le retour Android quitte l'application. */
    fun retour(): EtatNavigation? = when {
        controleId != null -> copy(controleId = null)
        placetteId != null -> copy(placetteId = null, arriveeVue = false)
        carte -> copy(carte = false, centreId = null)
        tourneeId != null -> copy(tourneeId = null, recepteurOuvert = false)
        destination == Destination.ESSAI || destination == Destination.RESTAURATION -> EtatNavigation(Destination.PARAMETRES)
        destination != Destination.TOURNEES -> EtatNavigation()
        else -> null
    }

    /** Pour `rememberSaveable` (rotation) : `[destination, tourneeId, placetteId, carte, centreId]`, chaînes vides pour null. */
    fun enListe(): List<String> = listOf(destination.name, tourneeId.orEmpty(), placetteId.orEmpty(), carte.toString(), centreId.orEmpty(), recepteurOuvert.toString(), controleId.orEmpty(), arriveeVue.toString())

    companion object {
        fun depuisListe(l: List<String>) = EtatNavigation(
            Destination.valueOf(l[0]),
            l[1].ifEmpty { null },
            l[2].ifEmpty { null },
            carte = l.getOrNull(3) == "true",
            centreId = l.getOrNull(4)?.ifEmpty { null },
            recepteurOuvert = l.getOrNull(5) == "true",
            controleId = l.getOrNull(6)?.ifEmpty { null },
            arriveeVue = l.getOrNull(7) == "true",
        )
    }
}
