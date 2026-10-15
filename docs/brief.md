# Surculus — brief

> **Proposition de l'orchestrateur, à amender par Pascal.** Rempli le 15 octobre 2026 à
> la demande de Pascal, à partir des échanges du jour et des dépôts sommieR (0.31.1),
> Marculus, Groma et nemetonshiny. Une phrase par rubrique quand c'est possible. Les
> passages marqués **[à trancher]** sont des propositions, pas des décisions. Une fois
> amendé, lancer `/brief` pour le faire critiquer par le `contradicteur`.

## Le problème, et pour qui (Abstraction)

- **Problème** : on enregistre qu'une plantation a été faite, presque jamais si elle a
  réussi, car le contrôle des plants se fait sur papier ou dans QField, plant par plant,
  et ne remonte au sommier que sous forme d'un taux de reprise unique, sans date ni
  position.
- **Bénéficiaire** : le gestionnaire forestier qui tient le sommier et décide des
  regarnis, dégagements et protections, puis le propriétaire, qui paie ces travaux.
- **Coût complet aujourd'hui** : des plantations perdues découvertes trop tard pour un
  regarni, des travaux payés deux fois ou pas au bon endroit, un bilan de l'aménagement
  ou du PSG qui ne sait pas dire ce qu'a donné l'argent investi, et du temps de bureau
  à ressaisir le terrain. **[à chiffrer par Pascal]**

## Ce que fait Surculus

- **En une phrase** : application Android de terrain, hors ligne, qui relève les
  travaux faits dans une plantation et suit, placette par placette et plant par plant,
  ce qui a repris, pour le rendre au sommier forestier.
- **Les gestes sur le terrain** : l'agent ou le technicien ouvre sur son téléphone la
  tournée préparée par sommieR ; il trace au GNSS l'intervention faite (code, surface,
  prévue ou non), retrouve le piquet de chaque placette, pointe chaque plant et le dit
  vivant, mort ou abrouti, à la voix ou aux boutons, sans réseau.
- **Ce qu'elle produit** : la tournée complétée (interventions localisées, placettes,
  plants et leurs états, photos), avec taux de reprise et densité calculés sur place
  et une alerte sous le seuil de reprise ; sommieR l'importe dans son registre des
  travaux.

## Ce qu'elle ne fait pas (hors périmètre)

- La programmation des travaux et le chantier (fiche de chantier, déclaration,
  facturation).
- Les calculs du bilan : ils restent dans sommieR.
- Le martelage (Marculus) et l'inventaire des peuplements (Groma).
- Écrire dans une base de données : Surculus remplit un fichier, l'import de sommieR
  décide.

## Contraintes connues

- **Terrain** : pas de réseau en forêt, tout fonctionne hors ligne ; récepteur GNSS
  RTK externe (u-blox ZED-F9P en Bluetooth, corrections NTRIP Centipède quand le réseau
  passe), repli sur le GNSS du téléphone ; saisie vocale à grammaire fermée et boutons
  de volume, comme Marculus.
- **Référentiels** : codes travaux issus des ITTS de l'ONF et des fiches travaux du
  CNPF. **Attention** : la nomenclature de 15 codes déjà en place dans sommieR
  (`SOMMIER_CODES_TRAVAUX` : PS, PL, RG, PG, PI, DG, CL, NT…) a été proposée par Claude
  le 1er octobre et n'est **pas** issue de ces référentiels ; elle est à confronter à
  ceux que l'`acquereur` déposera dans `docs/referentiels/`. **[à trancher]**
- **Lien avec l'existant** : sommieR prépare la tournée et la relit, il est la seule
  destination des saisies ; nemetonshiny fournit le prévu (son plan d'action) ; Surculus
  reprend les bibliothèques communes avec Marculus et Groma (GNSS, GeoPackage, carte,
  dictée, journal) sans les recopier ; nemeton n'est pas sur le chemin. **[à trancher]**
- **Formats et licences** : GeoPackage (le `travaux.gpkg` de sommieR, sans renommer ses
  couches ni ses champs), logiciel libre sous GPL-3.0-or-later comme Groma.

## Comment saura-t-on que ça marche ?

- **Signe concret** : un agent contrôle une placette de 50 m² (une dizaine de plants)
  en moins de 10 minutes, piquet retrouvé compris, sans papier, et la tournée rendue
  s'importe dans sommieR sans une faute. **[durée à fixer par Pascal]**
- **Terrain** : une plantation réelle de 1 à 3 ans, avec au moins trois placettes
  contrôlées deux fois, où Pascal vérifie sur place que les plants pointés sont
  retrouvés au même endroit et que le taux de reprise de Surculus égale son propre
  comptage. **[plantation à désigner par Pascal]**

## Qui tranche

Pascal Obstétar.
