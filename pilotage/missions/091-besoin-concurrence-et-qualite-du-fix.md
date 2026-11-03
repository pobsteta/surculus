# Mission 091 — Besoin, concurrence et qualité du fix

> Fiche écrite par l'orchestrateur le 2026-11-03, à partir du gabarit `imperatrice/skills/mission/fiche-mission.md`,
> après la relecture de la PR #155 (090) et la décision de Pascal du même jour (« Fusionner + mission 091 » : **aucun
> vrai import dans sommieR avant la 091**). Depuis `main` (67ff17a). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Faire saisir à la fin de chaque contrôle le besoin (le travail qui s'impose) et
la concurrence, et enregistrer au journal la précision, la source et le caractère simulé du fix, pour que la tournée
rendue ne fasse plus passer une placette « à jour » dans sommieR sans que l'agent l'ait dit, et que la position de
chaque contrôle dise ce qu'elle vaut.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Le constat (relecture de la PR #155)

- Surculus ne saisit ni `besoin` ni `concurrence` (`ValeurControle`, `core/.../session/Valeurs.kt`) ; le rendu les écrit
  vides. Or sommieR 0.31.1 calcule l'état de suivi de la placette par `is.na(dernier_besoin) | dernier_besoin ==
  "aucun"` → `a_jour` (`R/travaux_qfield.R` l. 335-337, tag v0.31.1) : **un contrôle rendu met sa placette « à jour »**,
  même avec une forte mortalité.
- Domaines de sommieR (tag v0.31.1) : `besoin` = un code de la couche `codes` de la tournée, ou `"aucun"`
  (`R/registre6.R` l. 137-138 ; `R/travaux_qfield.R` l. 61-64, qui ajoute « aucun » à la couche `codes`) ;
  `concurrence` ∈ `faible`, `moyenne`, `forte` (`SOMMIER_CONCURRENCES`, `R/registre6.R` l. 64). Surculus n'embarque
  aucune liste : il lit la couche `codes` (CLAUDE.md).
- Pour une position mesurée, `precision_m` et `source_gnss` sortent vides, et `simulee` est écrit à faux sans avoir été
  su (`ExportTravaux.kt` l. 242), alors que la spec § 5 les exige et que l'ADR 0003 veut une position simulée
  « détectée et enregistrée ».
- **Décision de Pascal (2026-11-03) :** une tournée sans aucun contrôle terminé est refusée au rendu, avec une phrase
  qui dit pourquoi (« Aucun contrôle terminé dans cette tournée : rien à rendre. »).

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | à moitié : les domaines sont connus (sommieR v0.31.1), mais le moment et la forme de la saisie (écran, voix, boutons de volume, gants) et le format du journal (valeurs v3 ? sauvegarde ?) sont à proposer | **tenir** : le `dev` rend un plan d'abord |
| A-t-il les moyens ? | oui | le `testeur` écrit d'abord les tests |

**Temps de la main :** tenir : justesse des données et format du journal.

## Ma réponse d'abord

_Rempli par Pascal seul._

## L'Homme

- **Agents :** `testeur` d'abord (tests depuis la spec, cette fiche et les domaines de sommieR, **sans lire** le code du
  contrôle, du journal et du rendu), puis `dev`, qui **rend un plan** avant de coder.
- **Modèle :** opus.
- **Ce qui ne lui est pas demandé :** proposer un besoin à partir des alertes (tranche 2) ; la 089.

## Questions que le plan du `dev` doit proposer (Pascal tranche)

1. Quand et comment l'agent dit le besoin et la concurrence (à « Terminer » ? obligatoires ? à la voix, aux boutons,
   à l'écran ?), en tenant les règles du terrain (gants, une main, sans regarder l'écran).
2. Les contrôles déjà terminés au journal sans besoin : refusés au rendu (« le rouvrir et dire le besoin »), ou autre ?
3. Format : nouvelle version des valeurs du journal (v3) et donc de la sauvegarde (v3), ou champs ajoutés sans changer
   de version ? Ce qu'un Surculus ancien fait d'une sauvegarde récente (règle de la 085 : refuser, jamais perdre sans le
   dire).
4. La source du fix (téléphone, RTK et son statut) et la détection d'une position simulée : ce qui est disponible
   aujourd'hui dans `SourcePosition` et le service RTK.

## Repris de la relecture de la 090 (mineurs, dans cette mission)

- Tournée sans contrôle terminé : refusée (décision ci-dessus).
- Un test du rendu avec un regarni saisi au journal (`SaisiesDuJournal.kt` l. 145-148, non couvert).
- Message « le fichier n'a pas pu être écrit » trop large (`RenduApp.kt` l. 104-106) : dire la vraie cause, en
  français.
- Synchronisation sur disque du zip avant de poser la marque `rendue.json` (`RenduTournee.kt` l. 142-173).
- En-tête GPL de `gpkg/src/test/.../outil/RendreScenario.kt`.
- La tolérance de 1 mm du recalcul des positions (`SaisiesDuJournal.kt` l. 99) : le plan dit d'où elle vient ou propose
  mieux.

## Harnais

- **Testeur peut :** tests dans `core/`, `gpkg/`, `data/`, `app/src/test/` ; fixtures ; CI d'import (`ci/sommier/`) pour
  exiger `besoin`, `concurrence` et l'état de suivi après import (F4) ; vus rouges.
- **Dev peut :** ce que son plan nomme et que Pascal accepte.
- **Ne doivent pas :** embarquer une liste de codes travaux ; toucher à `docs/` (signaler) ; toucher à `main`.
- **Sources autorisées :** spec § 4.3, § 5, § 6 ; ADR 0003, 0005, 0007 ; `docs/formats/` ; sommieR (`~/dev/sommieR`,
  lecture, tag v0.31.1) ; fiches 087, 088, 090.

## Valeur provisoire (chiffre, réglage)

- Aucune, sauf si le plan en propose une (il la nomme alors ici, avec ce qui la mesure).

## Dépend de / rouge attendu jusqu'à

- **Dépend de :** 090 (#155, 67ff17a).
- **PR en brouillon jusqu'à :** la fin du travail du `dev`.
- **CI rouge attendue :** les tests du `testeur`, entre son push et celui du `dev`.

## Tests

- Un contrôle terminé porte un besoin (code de la couche `codes` ou « aucun ») et une concurrence (faible, moyenne,
  forte) ; un code absent de la couche `codes` est refusé (`hors_referentiel`).
- Le rendu écrit `besoin` et `concurrence` ; après import (CI, F4), l'état de suivi de la placette suit le besoin dit
  (`a_programmer` si un besoin est dit, `a_jour` si « aucun »).
- Une position mesurée sort avec `precision_m`, `source_gnss` et `simulee` justes ; sans position mesurée, NULL (087).
- Tournée sans contrôle terminé : refusée avec la phrase décidée.
- Journal et sauvegarde anciens relus sans perte ; un Surculus ancien refuse une sauvegarde récente avec un message.
- `.claude/test-command` au code 0 ; CI verte.

## Terrain

- **Échantillon que Pascal vérifiera :** sur la release, un contrôle à Couchey terminé avec un besoin (par exemple un
  regarni) et une concurrence, rendu, importé dans sommieR : la placette passe « à programmer », pas « à jour ».

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Critère d'arrêt de la relecture :** 3 allers-retours.
- **Qui tranche :** Pascal (plan, questions 1 à 4, textes, fusion).

## Comment saura-t-on que ça marche ?

Une placette contrôlée avec un besoin passe « à programmer » dans sommieR après l'import de la tournée rendue, et la
position de son contrôle porte sa précision et sa source.
