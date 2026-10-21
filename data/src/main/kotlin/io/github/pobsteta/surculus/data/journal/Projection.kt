package io.github.pobsteta.surculus.data.journal

/**
 * Clé de projection : (tournée, objet, type). Notée `objet|type`, préfixée de `tournée|` quand la tournée n'est pas
 * vide. `codeTournee = ""` : lignes non rattachées, qui forment leur propre état (jamais fondues dans une tournée).
 */
data class CleProjection(val objetUuid: String, val type: String, val codeTournee: String = "") {
    override fun toString() = if (codeTournee.isEmpty()) "$objetUuid|$type" else "$codeTournee|$objetUuid|$type"
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
 * - une saisie dont `remplaceUuid` désigne une ligne d'un **autre objet** (ou, dans le même objet, d'un
 *   **autre type**) ne remplace rien : la ligne désignée garde sa valeur, la saisie est tête de sa propre
 *   clé, avec l'anomalie [REMPLACE_AUTRE_OBJET] (ou [REMPLACE_AUTRE_TYPE]) rattachée à cette clé, comme
 *   pour un remplacement inconnu (décisions de Pascal, 2026-10-17) ; une correction normale de cette
 *   saisie efface l'anomalie de la nouvelle tête (elle reste tracée au journal) ;
 * - une annulation dont l'objet diffère de celui de sa cible n'annule rien : la cible garde sa valeur,
 *   l'annulation est tête de la clé (objet déclaré, `annulation`) avec l'anomalie [REMPLACE_AUTRE_OBJET] ;
 * - **état par tournée** (mission 032, décision de Pascal du 2026-10-21) : la clé est (tournée, objet, type) ; une
 *   saisie ou une annulation dont `remplaceUuid` désigne une ligne **d'une autre tournée** ne remplace ni
 *   n'annule rien : la ligne est gardée, tête de sa propre clé, avec l'anomalie [REMPLACE_AUTRE_TOURNEE] ;
 *   le journal d'une autre tournée n'influe jamais sur l'état d'une tournée ;
 * - si plusieurs différences coexistent, la première de la liste objet, tournée, type est seule signalée.
 */
object Projection {
    const val UUID_CONTENU_DIFFERENT = "uuid_contenu_different"
    const val REMPLACE_INCONNU = "remplace_inconnu"

    /** Une saisie qui désigne une ligne d'un autre objet : elle ne la remplace pas (2026-10-17). */
    const val REMPLACE_AUTRE_OBJET = "remplace_autre_objet"

    /** Une saisie qui désigne une ligne d'une autre tournée : elle ne la remplace pas (mission 032). */
    const val REMPLACE_AUTRE_TOURNEE = "remplace_autre_tournee"

    /** Une saisie qui désigne une ligne d'un autre type du même objet : elle ne la remplace pas (2026-10-17). */
    const val REMPLACE_AUTRE_TYPE = "remplace_autre_type"

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

        // Une annulation dont la cible connue est d'un autre objet ou d'une autre tournée est ignorée : la
        // cible garde sa valeur.
        val annulationsAutreObjet = retenues.filter { l ->
            l.type == TypeLigne.ANNULATION &&
                l.remplaceUuid?.let { parUuid[it] }
                    ?.let { it.objetUuid != l.objetUuid || it.codeTournee != l.codeTournee } == true
        }

        val annulations = retenues.filter { it.type == TypeLigne.ANNULATION && it !in annulationsAutreObjet }
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
        // Une annulation d'un autre objet n'annule rien : elle reste tête de la clé (objet déclaré, annulation).
        val saisies = retenues.filter {
            it.type != TypeLigne.SAUT_HORLOGE && (it.type != TypeLigne.ANNULATION || it in annulationsAutreObjet)
        }
        val actives = saisies.filter { !estAnnulee(it.uuid) }

        // Ascendants de chaque ligne active, par les liens `remplaceUuid` valides (même objet, même type).
        // Un lien inconnu, d'un autre objet ou d'un autre type ne remplace rien et signale une anomalie.
        val remplacees = HashSet<String>()
        val anomaliesLigne = HashMap<String, MutableSet<String>>()
        for (l in actives) {
            val vus = HashSet<String>()
            var cur = l
            while (true) {
                val r = cur.remplaceUuid ?: break
                if (!vus.add(r)) break
                val parent = parUuid[r]
                val anomalie = when {
                    parent == null -> REMPLACE_INCONNU
                    parent.objetUuid != cur.objetUuid -> REMPLACE_AUTRE_OBJET
                    parent.codeTournee != cur.codeTournee -> REMPLACE_AUTRE_TOURNEE
                    parent.type != cur.type -> REMPLACE_AUTRE_TYPE
                    else -> null
                }
                if (anomalie != null) {
                    // L'anomalie est celle de la saisie elle-même : une correction normale l'efface (2026-10-17).
                    if (cur === l) anomaliesLigne.getOrPut(l.uuid) { sortedSetOf() } += anomalie
                    break
                }
                remplacees += r
                cur = parent!!
            }
        }

        val tetesParCle = actives.filter { it.uuid !in remplacees }.groupBy { CleProjection(it.objetUuid, it.type, it.codeTournee) }
        val cles = LinkedHashMap<CleProjection, EtatCle>()
        for (cle in saisies.map { CleProjection(it.objetUuid, it.type, it.codeTournee) }.distinct()) {
            val tetes = tetesParCle[cle].orEmpty() // déjà dans l'ordre total
            cles[cle] = EtatCle(
                cle = cle,
                courant = tetes.lastOrNull()?.uuid,
                conflit = tetes.size > 1,
                tetes = tetes.map { it.uuid },
                anomalies = tetes.flatMap { anomaliesLigne[it.uuid].orEmpty() }.distinct().sorted(),
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
