# Correspondance avec la nomenclature provisoire de sommieR

Propriétaire : Pascal. Consultation : 2026-10-15 (agent `acquereur`, seconde passe, sur sources primaires : voir `SOURCES.md`).
Décision éclairée : garder, remplacer ou rattacher les codes `SOMMIER_CODES_TRAVAUX`.

**La liste complète des 15 codes n'est pas disponible dans ce dépôt** (le code de sommieR n'y est pas). Ne sont connus que : PS, PL, RG, PG, PI, DG, CL, NT (`docs/brief.md`, contrainte « Référentiels ») et RN, DS (`briefs/vers-sommier/2026-10-15-travaux-3-programme.md`, point 4, table `SOMMIER_TYPES_ACTION`). Cinq codes manquent. Le sens de **PS, RG et PG n'est documenté nulle part** dans le dépôt : les rapprochements ci-dessous pour ces trois codes sont des **hypothèses de libellé**, à confirmer par Pascal ou dans le code de sommieR. Rien n'est inventé : les colonnes « libellé de la source » citent le RNTSF (ONF, 2010), le CNPTSF (ONF, v.2020) ou une fiche CNPF.

## Constat

Aucune convention externe fournie ne contient de code de travaux (voir `codes-travaux.md`). Les 15 codes de sommieR n'ont donc **aucune source externe** : l'objection O3 de la critique du brief (« codes inventés ») est **confirmée**. En revanche, **les libellés de la typologie du RNTSF §1.1 (imp. p.13) recouvrent bien la plupart des codes connus** : on peut rattacher chaque code à un libellé officiel, sans en emprunter le code.

## Tableau

| Code sommieR | Sens documenté (action) | Libellé de source le plus proche | Référence | Remarque |
|---|---|---|---|---|
| PL | action `plantation` | « Plantation, y compris regarnis et semis » | RNTSF §1.1, imp. p.13 | Dans le RNTSF le regarni fait partie de la plantation |
| RN | action `regeneration` | « Préparation du terrain préalable à la régénération naturelle (relevé de couvert, griffage, crochetage) » ; fiches CNPF I1 : dégagements, vides à regarnir en régénération naturelle | RNTSF §1.1, imp. p.13 | Ambigu : travail du sol préalable ou suivi de la régénération ? |
| PI | action `protection` | « Protection contre le gibier » ; CNPTSF distingue « engrillagements » et « protections individuelles de plants » | RNTSF §1.1 ; CNPTSF §3.3.1, imp. p.25 | Hypothèse : PI = protection individuelle, PG = protection par grillage (non documenté) |
| CL | action `cloisonnement` | « Création et entretien de cloisonnements » ; sylvicole 2 à 2,5 m, d'exploitation 4 m | RNTSF §1.1 ; glossaire imp. p.41-42 | **Équivalent trouvé** (la première passe n'en avait pas) |
| NT | action `depressage` | « Nettoiement » et « Dépressage » : deux libellés distincts | RNTSF §1.1 ; glossaire imp. p.42 et 44 | **Incohérence possible** : si NT signifie nettoiement (hauteur > 3 m), l'action `depressage` pointe vers le mauvais libellé |
| DG | action `entretien` | « Dégagement » (jeunes peuplements < 3 m) | RNTSF §1.1 ; glossaire imp. p.42 | L'action s'appelle « entretien », le code évoque « dégagement » ; les « premiers entretiens » CNPF I12 couvrent aussi interlignes, tailles |
| DS | action `desserte` | « Entretien courant de routes et chemins » ; « Création d'infrastructures (routes, pistes) » | RNTSF §1.1, imp. p.13 | Hors plantation |
| PS | non documenté | Hypothèse : « Préparation du terrain préalable à la régénération artificielle » | RNTSF §1.1 | **Collision de sigle** : dans les fiches CNPF, « PS » est le code de l'essence pin sylvestre |
| RG | non documenté | Hypothèse : regarni | RNTSF §1.1 (le regarni est dans « Plantation ») ; fiches CNPF (seuils de regarni) | Si RG est un code travaux distinct, il n'a pas d'équivalent ONF : le RNTSF n'isole pas le regarni |
| PG | non documenté | Hypothèse : protection par grillage (voir PI) | CNPTSF §3.3.1 | Non confirmé |

## Libellés des sources sans code sommieR identifiable

- Taille de formation et élagage (RNTSF §1.1 ; fiches CNPF : tailles de formation sur 200 à 400 tiges/ha, noyers annuelles en vert du 15 juin à fin juillet).
- Traitement phytosanitaire (RNTSF §2.7.2) ; détourage ; débroussaillement DFCI.
- Préparation du sol (labour, sous-solage, potets) : couverte par PS si l'hypothèse est exacte.
- Contrôle / constat de reprise : ce n'est pas un travail de l'entreprise mais un geste de Surculus (le RNTSF et le CNPTSF décrivent la réception du chantier, pas la reprise). Pas de code travaux.

## Questions pour Pascal

1. Garder les 15 codes comme **codes internes** de sommieR, avec une table de rattachement aux libellés RNTSF ci-dessus (rien à emprunter à l'ONF, qui ne publie pas de code), ou demander à l'ONF un bordereau codifié (CCTP) ?
2. Sens exact de PS, RG, PG (et des 5 codes non listés) : à lire dans le code de sommieR, hors dépôt.
3. Corriger la table `SOMMIER_TYPES_ACTION` : `depressage` → NT est-il voulu, sachant que le RNTSF distingue dépressage et nettoiement ?
4. Faut-il un code distinct pour le regarni, alors que le RNTSF le range dans la plantation (le regarni peut s'enregistrer comme PL avec un motif) ?
