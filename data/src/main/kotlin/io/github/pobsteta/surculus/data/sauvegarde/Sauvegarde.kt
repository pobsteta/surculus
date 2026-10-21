package io.github.pobsteta.surculus.data.sauvegarde

import io.github.pobsteta.surculus.data.journal.LigneJournal
import io.github.pobsteta.surculus.data.journal.db.JournalRepository
import io.github.pobsteta.surculus.data.journal.db.Reception

/**
 * Stockage partagé des sauvegardes (`Documents/Surculus/`). Sur l'appareil : MediaStore avec `IS_PENDING`
 * (fichier invisible tant qu'il n'est pas complet, jamais vu à 0 octet). Les tests emploient un faux en mémoire.
 */
interface StockageSauvegardes {
    fun noms(): Set<String>

    /** Écrit `octets` sous `nom` (qui n'existe pas) ; le fichier n'est visible qu'une fois complet. */
    fun ecrire(nom: String, octets: ByteArray)

    fun supprimer(nom: String)
}

/** Stockage partagé des photos (`Pictures/Surculus/<code-tournée>/`). */
interface StockagePhotos {
    /** Écrit la photo dès la prise ; rend le chemin relatif écrit. */
    fun ecrire(dossier: String, nom: String, octets: ByteArray): String
}

data class SauvegardeEcrite(val nom: String, val lignes: Int, val supprimees: List<String>)

class ServiceSauvegarde(
    private val depot: JournalRepository,
    private val stockage: StockageSauvegardes,
    private val appareilOpaque: String,
    private val versionSommier: String?,
) {
    /**
     * Sauvegarde les lignes de **cette tournée seule** sous un nom libre, puis applique la rotation de cette
     * tournée. Les lignes d'une autre tournée et les lignes non rattachées n'y entrent jamais.
     */
    fun sauvegarder(codeTournee: String, maintenantUtcMs: Long): SauvegardeEcrite {
        require(codeTournee.isNotBlank()) { "code de tournée vide" }
        val lignes = depot.lignes(codeTournee)
        val octets = EcritureSursync.ecrire(
            EnTeteSursync(FormatSursync.VERSION_COURANTE, codeTournee, versionSommier, appareilOpaque, maintenantUtcMs, lignes.size),
            lignes,
        )
        val nom = NomSauvegarde.nomLibre(NomSauvegarde.nom(codeTournee, appareilOpaque, maintenantUtcMs), stockage.noms())
        stockage.ecrire(nom, octets)
        val aSupprimer = Rotation.asupprimer(stockage.noms(), codeTournee).filter { it != nom }
        aSupprimer.forEach(stockage::supprimer)
        return SauvegardeEcrite(nom, lignes.size, aSupprimer)
    }
}

/** Compte rendu d'une restauration. */
data class CompteRendu(
    val refus: MotifRefus?,
    val signalements: Set<Signalement>,
    val ajoutees: Int,
    val dejaPresentes: Int,
    val refusees: Int,
) {
    val accepte: Boolean get() = refus == null
}

/**
 * Restauration : union sur l'`uuid` dans le journal local, sans jamais écraser (une tournée existante est
 * complétée, pas remplacée). Les lignes gardent leur `appareil` et leur `sequence` d'origine. Idempotente.
 */
object Restauration {
    fun restaurer(
        octets: ByteArray,
        depot: JournalRepository,
        versionCourante: Int = FormatSursync.VERSION_COURANTE,
    ): CompteRendu {
        val lecture = LectureSursync.lire(octets, versionCourante)
        if (!lecture.accepte) return CompteRendu(lecture.motif, emptySet(), 0, 0, 0)
        var ajoutees = 0
        var deja = lecture.lignesIgnorees
        var refusees = lecture.lignesRefusees
        val signalements = lecture.signalements.toMutableSet()
        for (l: LigneJournal in lecture.lignes) {
            when (depot.recevoir(l)) {
                Reception.Ajoutee -> ajoutees++
                Reception.Doublon -> deja++
                Reception.Refusee -> { refusees++; signalements += Signalement.UUID_CONTENU_DIFFERENT }
                Reception.TypeInvalide -> { refusees++; signalements += Signalement.LIGNE_ILLISIBLE }
            }
        }
        return CompteRendu(null, signalements, ajoutees, deja, refusees)
    }
}

/** Photo écrite dès la prise, avec son empreinte SHA-256 (ADR 0005, règle « Photos »). */
object EnregistrementPhoto {
    data class Photo(val chemin: String, val sha256: String)

    fun enregistrer(stockage: StockagePhotos, codeTournee: String, nom: String, octets: ByteArray): Photo {
        val dossier = "Pictures/Surculus/${NomSauvegarde.assainirCode(codeTournee)}/"
        return Photo(stockage.ecrire(dossier, nom, octets), FormatSursync.sha256Hex(octets))
    }
}
