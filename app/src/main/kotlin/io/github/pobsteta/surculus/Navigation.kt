package io.github.pobsteta.surculus

/** Écrans de premier niveau : la liste des tournées est la racine (mission 040). */
enum class Destination { TOURNEES, ESSAI, RESTAURATION }

/**
 * État de navigation en Kotlin pur (testable sans Compose).
 * Racine : la liste des tournées. Essai GNSS et Restauration s'ouvrent depuis le menu à trois points.
 * Une tournée ouverte donne la carte ; une placette choisie donne le guidage.
 */
data class EtatNavigation(
    val destination: Destination = Destination.TOURNEES,
    val tourneeId: String? = null,
    val placetteId: String? = null,
) {
    val estRacine: Boolean get() = destination == Destination.TOURNEES && tourneeId == null

    fun ouvrirEssai() = EtatNavigation(Destination.ESSAI)
    fun ouvrirRestauration() = EtatNavigation(Destination.RESTAURATION)
    fun ouvrirTournee(id: String) = EtatNavigation(Destination.TOURNEES, tourneeId = id)
    fun guider(placette: String) = if (tourneeId == null) this else copy(placetteId = placette)

    /** Un pas en arrière ; `null` = on est à la racine, le retour Android quitte l'application. */
    fun retour(): EtatNavigation? = when {
        placetteId != null -> copy(placetteId = null)
        tourneeId != null -> copy(tourneeId = null)
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
