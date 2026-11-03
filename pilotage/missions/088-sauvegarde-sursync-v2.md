# Mission 088 — Sauvegarde `.sursync` v2

> Fiche écrite par l'orchestrateur le 2026-11-03, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> d'après les décisions de Pascal du 2026-11-02 (« nouvelle version », « v2 partagée », découpage 086 → 087 → 088) et
> du 2026-11-03 (« Attendre la 088 » : la #152 n'est fusionnée dans `main` qu'avec la 088). **Branche partie de
> `mission/087-ouvrir-un-controle-sans-position`, PR vers cette branche** (pas vers `main`) : une seule release
> portera la 087 et la 088. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire passer la sauvegarde `.sursync` à sa version 2, qui porte les valeurs v2
du journal (contrôles sans position mesurée) et relit la v1, pour qu'un Surculus ancien refuse une sauvegarde récente
avec un message (« mettre l'application à jour ») au lieu de perdre sans le dire un contrôle sans position.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui pour la règle (spec § 4.8 ; `docs/formats/sursync-v1.md`, en-tête et « Version » ; `docs/formats/journal-valeurs-v2.md`, « Sauvegarde `.sursync` ») ; le document `sursync-v2.md` est à écrire | **tenir** : le `dev` rend un plan d'abord |
| A-t-il les moyens ? | oui : lecteur v1 existant (`FormatSursync.kt`, refus `version_future` déjà en place), fixtures `fixtures/sursync/` | le `testeur` écrit d'abord les tests |

**Temps de la main :** tenir : un format d'échange engage la durée (test du successeur) et la justesse des données.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis la spec, les formats et cette fiche, **sans lire** le code de la
  sauvegarde et de la restauration), puis `dev`, qui **rend un plan** (court, fichiers et étapes) avant de coder ;
  l'orchestrateur le présente à Pascal.
- **Modèle :** opus (format d'échange, justesse des données).
- **Ce qui ne lui est pas demandé :** la partie photos de la v2 (type de ligne `photo`) : le format la **réserve** et la
  décrit (spec § 4.8, amendement du 2026-10-28) mais aucun code ne l'écrit ni ne la restaure ici ; ouvrir sans position
  depuis le guidage (089).

## La règle (spec § 4.8 et formats : ils font foi)

- Dès que le journal écrit des valeurs v2 (toujours, depuis la 087), la sauvegarde s'écrit en **v2** ; même structure
  que la v1, seul le numéro de version monte (`sursync-v1.md`, en-tête « Version suivante décidée »).
- Un Surculus à jour **lit la v1 et la v2** ; une v1 relue donne exactement ce qu'elle donnait avant.
- Un Surculus ancien (lecteur v1) refuse la v2 en entier, motif `version_future`, message « mettre l'application à
  jour » : comportement déjà en place, **à prouver** par un test qui lit une fixture v2 avec le lecteur tel qu'il est
  sur `main` avant la 087 (ou par la règle « version *N*+1 » du lecteur v1).
- Restaurer une v2 qui contient un contrôle sans position rend le contrôle sans position, à l'identique (drapeau
  `sans_position_mesuree`, positions mesurées nulles), relu après redémarrage, puis exporté comme la 087 l'exige.
- Empreinte SHA-256, nom de fichier, rotation, non-écrasement, union sur l'UUID : inchangés (décisions de la 024 et
  de la 009).
- **Document** `docs/formats/sursync-v2.md`, son schéma `sursync-v2.schema.json` et ses fixtures
  `fixtures/sursync/exemple-v2*.sursync` : écrits par cette mission (la spec § 4.8 le confie à « la mission de code ») ;
  `sursync-v1.md` reçoit seulement un renvoi vers la v2.

## Harnais

- **Testeur peut :** tests dans `core/`, `data/`, `app/src/test/` ; fixtures `fixtures/sursync/exemple-v2*` (dont une
  v2 avec un contrôle sans position, une v2 à empreinte fausse, une v2 tronquée) et le cas « v3 refusée » ; vus rouges.
- **Dev peut :** `data/.../sauvegarde/FormatSursync.kt` et le code de sauvegarde et de restauration qui en dépend,
  `EcranRestauration.kt` et ses ressources si un texte change ; `docs/formats/sursync-v2.md`,
  `docs/formats/sursync-v2.schema.json`, renvoi dans `docs/formats/sursync-v1.md`. Il ne réécrit pas les tests du
  testeur ; tout autre fichier, il le demande dans son plan.
- **Ne doivent pas :** toucher au reste de `docs/` (signaler) ; coder la partie photos ; toucher à `main` ; pousser sur
  `mission/087-…` (la PR de la 088 vise cette branche, c'est l'orchestrateur qui fusionne).
- **Sources autorisées :** spec § 4.8 et § 5, ADR 0005, `docs/formats/sursync-v1.md` et son schéma,
  `docs/formats/journal-valeurs-v1.md` et `-v2.md`, fiches 009, 024, 031, 085 et 087.

## Valeur provisoire (chiffre, réglage)

- Aucune.

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 087 (#152, relue, à fusionner, **non fusionnée** : décision de Pascal du 2026-11-03).
- **Ordre de fusion :** 088 dans `mission/087-…`, puis #152 dans `main` : une seule release porte les deux.
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev`.

## Tests

- Une sauvegarde écrite après la 087 est en v2 ; une v1 (fixtures existantes) se relit à l'identique.
- Une v2 avec un contrôle sans position se restaure à l'identique, puis s'exporte avec la ligne à géométrie NULL.
- Un lecteur v1 refuse la v2 (`version_future`) ; une v3 est refusée (`version_future`) ; une version illisible reste
  `version_illisible`.
- Empreinte fausse et fichier tronqué en v2 : mêmes règles qu'en v1.
- `.claude/test-command` au code 0 ; CI verte (`CI` et « Import sommieR »).

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release (087 et 088), un contrôle ouvert sans position, la sauvegarde
  écrite dans `Documents/Surculus/` (en-tête en version 2), puis restaurée sur le même téléphone (ou un second) : le
  contrôle revient « sans position » et l'export le garde.

## Qui veille, qui tranche

- **Relecteur :** `relecteur` (format contre la spec et les formats, compatibilité v1, refus d'un ancien), puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Point de suivi :** au push du `testeur`, au plan du `dev`, puis à la PR.
- **Qui tranche :** Pascal (plan, fusion).

## Comment saura-t-on que ça marche ?

Une sauvegarde v2 contenant un contrôle sans position se restaure à l'identique sur un Surculus à jour, et un Surculus
ancien la refuse avec « mettre l'application à jour ».
