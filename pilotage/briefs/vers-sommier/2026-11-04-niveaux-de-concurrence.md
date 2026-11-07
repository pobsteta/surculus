# Brief `surculus` → `sommieR` — définir les niveaux de concurrence et le besoin d'un contrôle

> **Statut** : ouvert, 2026-11-04 · **Émis par** : session `surculus` (orchestrateur), sur la
> décision de Pascal · **Urgence** : moyenne · **Issue** : pobsteta/sommieR#69

## Décisions de Pascal (2026-11-04, missions 091 et 092 de Surculus)

« sommieR, par un brief » (qui définit les niveaux de concurrence) ; « Faible · Moyenne · Forte » (libellés
d'écran de Surculus).

## Ce que Surculus va faire

- À la fin de chaque contrôle, l'agent dira **obligatoirement** le `besoin` (un code de la couche `codes` de
  la tournée, ou « aucun ») et la `concurrence` (`faible`, `moyenne`, `forte`, exactement
  `SOMMIER_CONCURRENCES`, `R/registre6.R` l. 64, tag v0.31.1). Surculus n'embarque aucune liste : il lit
  la couche `codes` et reprend vos trois niveaux (`docs/referentiels/concurrences.csv` de Surculus).
- Constat qui a motivé la 091 : un contrôle importé **sans** `besoin` fait passer sa placette `a_jour`
  (`R/travaux_qfield.R` l. 335-337, `is.na(dernier_besoin) | dernier_besoin == "aucun"`). Les tournées
  rendues par Surculus avant sa 091 ont un `besoin` vide : **elles ne doivent pas être importées**.

## Ce qui manque (écarts relevés par l'acquéreur de Surculus, mission 092)

Aucune des cinq sources publiées lues (guide « Réussir la plantation forestière » 2014 p. 52 ; DSF, bilan
2024 p. 13 et 16 ; RENFOR / INRAE ; CNPF CONQueTh ; MRNF Québec 2024 p. 12) ne définit trois niveaux de
concurrence au contrôle d'une plantation : les critères publiés sont **binaires et portent sur le plant**
(« aucune touffe de végétation concurrente au pied des plants », guide 2014 p. 52).

1. **Définition** : qu'est-ce qui distingue `faible`, `moyenne`, `forte` sur le terrain (recouvrement en %,
   hauteur de la végétation par rapport au plant, part des plants à dégager) ? Sans règle, deux contrôleurs
   notent différemment la même placette.
2. **Échelle** : niveau de placette contre critère binaire par plant : faut-il une règle de passage ?
3. **Nature** : un seul niveau pour l'herbacée et la ligneuse (fougère aigle, ronce) ?
4. **« aucun » et NA** : l'état de suivi traite un besoin absent comme « aucun ». Un besoin absent
   devrait-il plutôt laisser la placette `a_programmer` ou la signaler ?

## Demande

Définir les trois niveaux (et, le cas échéant, la règle de passage) dans sommieR, où vit la nomenclature ;
répondre dans `~/dev/briefs/vers-surculus/`. Surculus affichera la définition à l'écran dès qu'elle existe.
Le fichier de Surculus : https://github.com/pobsteta/surculus/blob/mission/092-referentiel-des-concurrences/docs/referentiels/concurrences.csv
