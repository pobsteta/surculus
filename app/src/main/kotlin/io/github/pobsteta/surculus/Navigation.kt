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
 * Une tournée ouverte donne la carte ; une placette choisie donne le guidage.
 */
data class EtatNavigation(
    val destination: Destination = Destination.TOURNEES,
    val tourneeId: String? = null,
    val placetteId: String? = null,
) {
    val estRacine: Boolean get() = destination == Destination.TOURNEES && tourneeId == null

    /** Vrai tant que la carte, le guidage ou l'essai GNSS est ouvert : le récepteur y tourne (mission 047), pas ailleurs. */
    val recepteurDemande: Boolean get() = tourneeId != null || destination == Destination.ESSAI

    fun ouvrirParametres() = EtatNavigation(Destination.PARAMETRES)
    fun ouvrirEssai() = EtatNavigation(Destination.ESSAI)
    fun ouvrirRestauration() = EtatNavigation(Destination.RESTAURATION)
    fun ouvrirTournee(id: String) = EtatNavigation(Destination.TOURNEES, tourneeId = id)
    fun guider(placette: String) = if (tourneeId == null) this else copy(placetteId = placette)

    /** Un pas en arrière ; `null` = on est à la racine, le retour Android quitte l'application. */
    fun retour(): EtatNavigation? = when {
        placetteId != null -> copy(placetteId = null)
        tourneeId != null -> copy(tourneeId = null)
        destination == Destination.ESSAI || destination == Destination.RESTAURATION -> EtatNavigation(Destination.PARAMETRES)
        destination != Destination.TOURNEES -> EtatNavigation()
        else -> null
    }

    /** Pour `rememberSaveable` (rotation) : `[destination, tourneeId, placetteId]`, chaînes vides pour null. */
    fun enListe(): List<String> = listOf(destination.name, tourneeId.orEmpty(), placetteId.orEmpty())

    companion object {
        fun depuisListe(l: List<String>) = EtatNavigation(
            Destination.valueOf(l[0]),
            l[1].ifEmpty { null },
            l[2].ifEmpty { null },
        )
    }
}
