package io.github.pobsteta.surculus.data.journal.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Accès au journal. **Ni `@Update` ni `@Delete`** sur `journal` et `refus_journal` : le journal est en ajout
 * seul (les déclencheurs SQLite le garantissent aussi). Seule `projection`, reconstructible, est réécrite.
 */
@Dao
interface JournalDao {
    /** Échoue (`SQLiteConstraintException`) si l'`uuid` existe déjà : jamais d'écrasement. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun inserer(ligne: LigneJournalEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insererRefus(refus: RefusJournalEntity)

    @Query("SELECT * FROM journal WHERE uuid = :uuid")
    fun trouver(uuid: String): LigneJournalEntity?

    @Query("SELECT MAX(sequence) FROM journal WHERE appareil = :appareil")
    fun derniereSequence(appareil: String): Long?

    /** Dernière ligne de l'appareil hors `saut_horloge` (référence de la détection de saut). */
    @Query("SELECT * FROM journal WHERE appareil = :appareil AND type <> 'saut_horloge' ORDER BY sequence DESC LIMIT 1")
    fun precedente(appareil: String): LigneJournalEntity?

    @Query("SELECT * FROM journal WHERE remplace_uuid = :uuid")
    fun remplacantes(uuid: String): List<LigneJournalEntity>

    @Query("SELECT * FROM journal ORDER BY saisi_le_utc, appareil, sequence, uuid")
    fun toutes(): List<LigneJournalEntity>

    /** Lignes d'**une** tournée, dans l'ordre total. Le code `''` (lignes non rattachées) n'est jamais demandé. */
    @Query("SELECT * FROM journal WHERE code_tournee = :code AND code_tournee <> '' ORDER BY saisi_le_utc, appareil, sequence, uuid")
    fun deLaTournee(code: String): List<LigneJournalEntity>

    @Query("SELECT * FROM refus_journal ORDER BY id")
    fun tousLesRefus(): List<RefusJournalEntity>

    /**
     * Lignes des objets donnés, plus celles dont l'`uuid` a une version refusée, plus les cibles
     * (`remplace_uuid`) des lignes de ces objets, même d'un autre objet (pour reconnaître un remplacement
     * d'un autre objet comme la reconstruction complète).
     */
    @Query(
        "SELECT * FROM journal WHERE objet_uuid IN (:objets) OR uuid IN (SELECT uuid FROM refus_journal) " +
            "OR uuid IN (SELECT remplace_uuid FROM journal WHERE objet_uuid IN (:objets) AND remplace_uuid IS NOT NULL)",
    )
    fun lignesPourObjets(objets: Collection<String>): List<LigneJournalEntity>

    @Query("SELECT COUNT(*) FROM journal")
    fun compter(): Long

    // --- projection (reconstructible) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun ecrireProjection(lignes: List<ProjectionEntity>)

    @Query("DELETE FROM projection WHERE objet_uuid IN (:objets)")
    fun effacerProjection(objets: Collection<String>)

    @Query("DELETE FROM projection")
    fun effacerToutesLesProjections()

    @Query("SELECT * FROM projection ORDER BY objet_uuid, type")
    fun projection(): List<ProjectionEntity>

    @Query("SELECT * FROM projection WHERE objet_uuid = :objet AND type = :type")
    fun projectionDe(objet: String, type: String): ProjectionEntity?
}
