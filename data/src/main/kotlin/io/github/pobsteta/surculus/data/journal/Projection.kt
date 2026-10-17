package io.github.pobsteta.surculus.data.journal

/** Clé de projection : (objet, type). Notée `objet|type`. */
data class CleProjection(val objetUuid: String, val type: String) {
    override fun toString() = "$objetUuid|$type"
}

/** État courant d'une clé. `courant` est `null` quand plus aucune valeur n'est active. */
data class EtatCle(
    val cle: CleProjection,
    val courant: String?,
    val conflit: Boolean,
    /** Lignes actives (têtes), dans l'ordre total. */
    val tetes: List<String>,
    val anomalies: List<String>,
)

data class ResultatProjection(
    /** `uuid` des lignes retenues, dans l'ordre total. */
    val ordreTotal: List<String>,
    val cles: Map<CleProjection, EtatCle>,
    val lignesIgnorees: List<String>,
    val lignesRefusees: List<String>,
    /** Anomalies du journal, hors clé. */
    val anomalies: List<String>,
) {
    val lignesRetenues: Int get() = ordreTotal.size
}

/**
 * Projection pure du journal (ADR 0005, spec § 5). Aucun accès à la base : le résultat ne dépend que de
 * l'**ensemble** des lignes, jamais de leur ordre d'arrivée.
 *
 * Règles :
 * - une ligne reçue deux fois à l'identique est ignorée ; un même `uuid` en deux contenus différents :
 *   **les deux versions sont refusées** (anomalie [UUID_CONTENU_DIFFERENT]) ;
 * - une `annulation` désigne par `remplaceUuid` la ligne annulée et agit sur la clé de **cette** ligne ;
 *   annuler, c'est défaire (la valeur remplacée redevient courante) ; annuler une annulation refait ;
 * - une ligne est **tête** de sa clé si elle est active (non annulée) et qu'aucune autre ligne active ne la
 *   remplace, directement ou par une chaîne de `remplaceUuid` ; deux têtes ou plus = **conflit** ;
 * - la valeur courante est la dernière tête selon l'ordre total ;
 * - un `remplaceUuid` inconnu : la ligne reste tête, anomalie [REMPLACE_INCONNU] ; une annulation à cible
 *   inconnue est ignorée (même anomalie) et s'applique quand la cible arrive ;
 * - une ligne (correction ou annulation) dont la cible est une ligne d'un **autre objet** n'agit sur aucun
 *   objet : elle reste au journal, hors projection, avec l'anomalie [REMPLACE_AUTRE_OBJET].
 */
object Projection {
    const val UUID_CONTENU_DIFFERENT = "uuid_contenu_different"
    const val REMPLACE_INCONNU = "remplace_inconnu"

    /** Une ligne qui remplace ou annule une ligne d'un autre objet : gardée, mais sans effet (2026-10-17). */
    const val REMPLACE_AUTRE_OBJET = "remplace_autre_objet"

    /** Ordre total : (`saisiLeUtcMs`, `appareil`, `sequence` numérique, `uuid`). */
    val ORDRE_TOTAL: Comparator<LigneJournal> =
        compareBy<LigneJournal>({ it.saisiLeUtcMs }, { it.appareil }, { it.sequence }, { it.uuid })

    fun projeter(arrivees: Collection<LigneJournal>): ResultatProjection {
        val ignorees = sortedSetOf<String>()
        val refusees = sortedSetOf<String>()
        val retenues = ArrayList<LigneJournal>()
        for ((uuid, versions) in arrivees.groupBy { it.uuid }) {
            val distinctes = versions.distinct()
            when {
                distinctes.size > 1 -> refusees += uuid
                else -> {
                    retenues += distinctes.single()
                    if (versions.size > 1) ignorees += uuid
                }
            }
        }
        retenues.sortWith(ORDRE_TOTAL)
        val parUuid = retenues.associateBy { it.uuid }
        val anomaliesJournal = sortedSetOf<String>()
        if (refusees.isNotEmpty()) anomaliesJournal += UUID_CONTENU_DIFFERENT

        // Cible connue d'un autre objet : la ligne n'agit sur rien (ni tête, ni annulation).
        val (horsObjet, valides) = retenues.partition { l ->
            l.remplaceUuid?.let { parUuid[it] }?.let { it.objetUuid != l.objetUuid } == true
        }
        if (horsObjet.isNotEmpty()) anomaliesJournal += REMPLACE_AUTRE_OBJET

        val annulations = valides.filter { it.type == TypeLigne.ANNULATION }
        val annulationsParCible = annulations.filter { it.remplaceUuid != null }.groupBy { it.remplaceUuid!! }
        annulations.filter { it.remplaceUuid == null || it.remplaceUuid !in parUuid }
            .forEach { anomaliesJournal += REMPLACE_INCONNU }

        // Une ligne est annulée si une annulation active la désigne ; une annulation est active tant qu'elle
        // n'est pas elle-même annulée. Les cycles (impossibles avec des uuid v7) sont coupés : inactifs.
        val annulee = HashMap<String, Boolean>()
        val enCours = HashSet<String>()
        fun estAnnulee(uuid: String): Boolean {
            annulee[uuid]?.let { return it }
            if (!enCours.add(uuid)) return false
            val r = annulationsParCible[uuid].orEmpty().any { !estAnnulee(it.uuid) }
            enCours.remove(uuid)
            annulee[uuid] = r
            return r
        }

        // Les sauts d'horloge sont des événements, pas des états : ils n'ont pas de clé de projection.
        val saisies = valides.filter { it.type != TypeLigne.ANNULATION && it.type != TypeLigne.SAUT_HORLOGE }
        val actives = saisies.filter { !estAnnulee(it.uuid) }

        // Ascendants (par remplaceUuid, à travers toutes les lignes connues) de chaque ligne active.
        val remplacees = HashSet<String>()
        val inconnu = HashSet<String>()
        for (l in actives) {
            val vus = HashSet<String>()
            var r = l.remplaceUuid
            while (r != null && vus.add(r)) {
                val parent = parUuid[r]
                if (parent == null) { inconnu += l.uuid; break }
                remplacees += r
                r = parent.remplaceUuid
            }
        }

        val tetesParCle = actives.filter { it.uuid !in remplacees }.groupBy { CleProjection(it.objetUuid, it.type) }
        val cles = LinkedHashMap<CleProjection, EtatCle>()
        for (cle in saisies.map { CleProjection(it.objetUuid, it.type) }.distinct()) {
            val tetes = tetesParCle[cle].orEmpty() // déjà dans l'ordre total
            cles[cle] = EtatCle(
                cle = cle,
                courant = tetes.lastOrNull()?.uuid,
                conflit = tetes.size > 1,
                tetes = tetes.map { it.uuid },
                anomalies = if (tetes.any { it.uuid in inconnu }) listOf(REMPLACE_INCONNU) else emptyList(),
            )
        }
        return ResultatProjection(
            ordreTotal = retenues.map { it.uuid },
            cles = cles,
            lignesIgnorees = ignorees.toList(),
            lignesRefusees = refusees.toList(),
            anomalies = anomaliesJournal.toList(),
        )
    }
}
