# Référentiels

> Une seule source de vérité pour les codes et conventions métier. **On n'invente jamais
> un code quand une convention existe.** Chaque fichier indique sa source, sa date et
> son propriétaire.

## À constituer (agent `acquereur`)

| Référentiel | Source | Propriétaire | Statut |
|---|---|---|---|
| Libellés de travaux de plantation et de suivi | RNTSF (ONF, 2010) et CNPTSF (ONF, v.2020) ; fiches CNPF « Itinéraires techniques par essence » (avril 2020) : PDF déposés ici | Pascal | **réécrit le 2026-10-15 sur sources primaires** : `codes-travaux.csv`, `codes-travaux.md`. **Aucun code de travaux n'existe dans ces sources** (libellés seulement, colonne `code` vide) ; à valider par Pascal |
| Correspondance avec les codes sommieR | `SOMMIER_CODES_TRAVAUX` (10 codes connus sur 15) rapprochés des libellés du RNTSF | Pascal | **réécrit le 2026-10-15** : `correspondance-sommier.md`. PS, RG, PG non documentés ; questions posées à Pascal |
| Densités, seuils de regarni et entretiens par essence | 20 fiches CNPF (avril 2020) | Pascal | **déposé le 2026-10-15** : `essences-itineraires.csv` (une ligne par essence, avec page). Contient aussi les codes réels d'essence (C, CB…) et d'itinéraire (I1 à I13) du CNPF |
| Inventaire des sources et conditions de réutilisation | les 24 PDF de ce dossier | Pascal | **déposé le 2026-10-15** : `SOURCES.md`. Aucune licence dans les documents : ne pas publier les PDF sans accord (à trancher) ; `guide-version-bfc.pdf` hors sujet |
| Essences et leurs codes | _fiches CNPF : codes d'essence à 1 ou 2 lettres (introduction_6.pdf p.8/8) ; à confronter aux codes CHS / CHP de Marculus_ | Pascal | **à acquérir** (codes CNPF relevés dans `essences-itineraires.csv`, pas de référentiel dédié) |

Chaque référentiel est déposé en format ouvert (CSV ou YAML) avec, en tête :
source, date de consultation, propriétaire, et la liste des écarts relevés entre sources.
