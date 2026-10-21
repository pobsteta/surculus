package io.github.pobsteta.surculus.data.journal.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.pobsteta.surculus.data.journal.LigneJournal

/**
 * Table `journal` : une ligne par saisie, **jamais modifiée ni effacée** (le DAO n'offre ni mise à jour ni
 * suppression, et des déclencheurs SQLite refusent `UPDATE` et `DELETE`). Les instants sont des
 * millisecondes UTC.
 */
@Entity(
    tableName = "journal",
    indices = [
        Index(value = ["objet_uuid", "type"]),
        Index(value = ["remplace_uuid"]),
        Index(value = ["appareil", "sequence"]),
        // Version 2 : l'ordre total range d'abord par `saisi_le_utc` (MIGRATION_1_2).
        Index(value = ["saisi_le_utc"]),
        // Version 3 : la sauvegarde lit les lignes d'une tournée (MIGRATION_2_3).
        Index(value = ["code_tournee"]),
    ],
)
data class LigneJournalEntity(
    @PrimaryKey @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "objet_uuid") val objetUuid: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "valeur") val valeur: String?,
    @ColumnInfo(name = "remplace_uuid") val remplaceUuid: String?,
    @ColumnInfo(name = "operateur") val operateur: String,
    @ColumnInfo(name = "appareil") val appareil: String,
    @ColumnInfo(name = "sequence") val sequence: Long,
    @ColumnInfo(name = "saisi_le_utc") val saisiLeUtcMs: Long,
    @ColumnInfo(name = "heure_gnss") val heureGnssMs: Long?,
    @ColumnInfo(name = "monotone_ms") val monotoneMs: Long,
    @ColumnInfo(name = "demarrage") val demarrage: Long,
    @ColumnInfo(name = "age_fix_ms") val ageFixMs: Long?,
    // Version 3 : tournée de la ligne ; '' = non rattachée (lignes antérieures, MIGRATION_2_3).
    @ColumnInfo(name = "code_tournee", defaultValue = "''") val codeTournee: String = "",
) {
    fun versLigne() = LigneJournal(
        uuid, objetUuid, type, valeur, remplaceUuid, operateur, appareil, sequence,
        saisiLeUtcMs, heureGnssMs, monotoneMs, demarrage, ageFixMs, codeTournee,
    )

    companion object {
        fun de(l: LigneJournal) = LigneJournalEntity(
            l.uuid, l.objetUuid, l.type, l.valeur, l.remplaceUuid, l.operateur, l.appareil, l.sequence,
            l.saisiLeUtcMs, l.heureGnssMs, l.monotoneMs, l.demarrage, l.ageFixMs, l.codeTournee,
        )
    }
}

/**
 * Table `refus_journal` : la **seconde version** d'un `uuid` reçu avec un contenu différent. Le journal ne
 * pouvant rien effacer, la première version reste dans `journal` ; la projection écarte les deux (ADR 0005),
 * pour que le résultat ne dépende pas de l'ordre d'arrivée. Ajout seul, comme `journal`.
 */
@Entity(tableName = "refus_journal", indices = [Index(value = ["uuid"]), Index(value = ["objet_uuid"])])
data class RefusJournalEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "objet_uuid") val objetUuid: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "valeur") val valeur: String?,
    @ColumnInfo(name = "remplace_uuid") val remplaceUuid: String?,
    @ColumnInfo(name = "operateur") val operateur: String,
    @ColumnInfo(name = "appareil") val appareil: String,
    @ColumnInfo(name = "sequence") val sequence: Long,
    @ColumnInfo(name = "saisi_le_utc") val saisiLeUtcMs: Long,
    @ColumnInfo(name = "heure_gnss") val heureGnssMs: Long?,
    @ColumnInfo(name = "monotone_ms") val monotoneMs: Long,
    @ColumnInfo(name = "demarrage") val demarrage: Long,
    @ColumnInfo(name = "age_fix_ms") val ageFixMs: Long?,
    // Version 3 : tournée de la ligne ; '' = non rattachée (lignes antérieures, MIGRATION_2_3).
    @ColumnInfo(name = "code_tournee", defaultValue = "''") val codeTournee: String = "",
) {
    fun versLigne() = LigneJournal(
        uuid, objetUuid, type, valeur, remplaceUuid, operateur, appareil, sequence,
        saisiLeUtcMs, heureGnssMs, monotoneMs, demarrage, ageFixMs, codeTournee,
    )

    companion object {
        fun de(l: LigneJournal) = RefusJournalEntity(
            uuid = l.uuid, objetUuid = l.objetUuid, type = l.type, valeur = l.valeur, remplaceUuid = l.remplaceUuid,
            operateur = l.operateur, appareil = l.appareil, sequence = l.sequence, saisiLeUtcMs = l.saisiLeUtcMs,
            heureGnssMs = l.heureGnssMs, monotoneMs = l.monotoneMs, demarrage = l.demarrage, ageFixMs = l.ageFixMs,
            codeTournee = l.codeTournee,
        )
    }
}

/**
 * Table `projection` : état courant par clé (`objet_uuid`, `type`). **Reconstructible** depuis le journal
 * (`JournalRepository.reconstruireProjection`) ; mise à jour par clé touchée à chaque ajout, jamais par
 * rejeu complet. `tetes` et `anomalies` : listes séparées par des virgules (les `uuid` n'en contiennent pas).
 */
@Entity(tableName = "projection", primaryKeys = ["objet_uuid", "type"])
data class ProjectionEntity(
    @ColumnInfo(name = "objet_uuid") val objetUuid: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "courant_uuid") val courantUuid: String?,
    @ColumnInfo(name = "conflit") val conflit: Boolean,
    @ColumnInfo(name = "tetes") val tetes: String,
    @ColumnInfo(name = "anomalies") val anomalies: String,
)
