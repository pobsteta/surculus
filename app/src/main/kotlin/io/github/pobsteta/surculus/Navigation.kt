package io.github.pobsteta.surculus

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
    fun guider(placette: String) = if (tourneeId == null) this else copy(carte = true, placetteId = placette, recepteurOuvert = true)

    /** Un pas en arrière ; `null` = on est à la racine, le retour Android quitte l'application. */
    fun retour(): EtatNavigation? = when {
        placetteId != null -> copy(placetteId = null)
        carte -> copy(carte = false, centreId = null)
        tourneeId != null -> copy(tourneeId = null, recepteurOuvert = false)
        destination == Destination.ESSAI || destination == Destination.RESTAURATION -> EtatNavigation(Destination.PARAMETRES)
        destination != Destination.TOURNEES -> EtatNavigation()
        else -> null
    }

    /** Pour `rememberSaveable` (rotation) : `[destination, tourneeId, placetteId, carte, centreId]`, chaînes vides pour null. */
    fun enListe(): List<String> = listOf(destination.name, tourneeId.orEmpty(), placetteId.orEmpty(), carte.toString(), centreId.orEmpty(), recepteurOuvert.toString())

    companion object {
        fun depuisListe(l: List<String>) = EtatNavigation(
            Destination.valueOf(l[0]),
            l[1].ifEmpty { null },
            l[2].ifEmpty { null },
            carte = l.getOrNull(3) == "true",
            centreId = l.getOrNull(4)?.ifEmpty { null },
            recepteurOuvert = l.getOrNull(5) == "true",
        )
    }
}
