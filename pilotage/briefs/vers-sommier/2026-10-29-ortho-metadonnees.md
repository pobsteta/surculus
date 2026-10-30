# Brief `surculus` → `sommieR` — les métadonnées de l'ortho tuilée

> **Statut** : ouvert, 2026-10-29 · **Émis par** : session `surculus` (orchestrateur), sur la
> décision de Pascal · **Urgence** : faible · **Précise** le point 3 du brief du 2026-10-27
> (`2026-10-27-ortho-tuilee-3857.md`, pobsteta/sommieR#66).

## Décisions de Pascal (2026-10-29, mission 059 de Surculus)

« « Mise à jour » » ; « Oui, brief » ; « « Source non indiquée » » ; « Seulement si écrite ».

## Ce que Surculus lira dans `ortho.mbtiles` (table `metadata`)

| Clé | Contenu | Exemple |
|---|---|---|
| `attribution` | source, telle qu'elle doit être affichée, **licence comprise** si on veut qu'elle s'affiche | « © IGN, BD ORTHO®, Géoplateforme, Licence Ouverte 2.0 » |
| `date_mise_a_jour` | date de dernière mise à jour de l'information (Licence Ouverte 2.0), AAAA-MM-JJ | « 2026-10-27 » |

- `attribution` est une clé de la spécification MBTiles 1.3. `date_mise_a_jour` **n'en est pas une** :
  c'est une convention entre sommieR et Surculus, proposée ici, à confirmer ou à renommer par la session
  `sommieR`.
- Surculus affiche sous la carte : « Ortho chantier · <attribution> · mise à jour le <date> ». Sans
  `date_mise_a_jour`, il n'ajoute rien. Sans `attribution`, il écrit « source non indiquée par le
  fichier ». Il **n'ajoute jamais** la licence de lui-même.
- Pour un `ortho.gpkg` : Surculus ne prend **pas** la `description` de `gpkg_contents` pour une source.
  Si sommieR livre un GeoPackage, dire où il écrit l'attribution et la date (proposition : une table
  `metadata` de même forme, clé-valeur).

## Constat sur le fichier d'essai

`couchey-demo-avec-ortho.zip` (Surculus, 2026-10-27) porte `attribution` = « © IGN, BD ORTHO
(ORTHOIMAGERY.ORTHOPHOTOS), Géoplateforme, téléchargée le 2026-10-27 pour un essai de Surculus », et
pas de date. Il n'a pas été produit par sommieR, qui ne livre aujourd'hui qu'un `ortho.tif`.

## Demande

Quand sommieR produira l'ortho tuilée (#66), écrire ces deux clés ; répondre dans
`~/dev/briefs/vers-surculus/` si le nom de la clé de date change.
