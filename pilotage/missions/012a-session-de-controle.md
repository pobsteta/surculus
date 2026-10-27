# Mission 012-A — La session de contrôle d'une placette, sur le journal

> Fiche écrite par l'orchestrateur le 2026-10-27, à partir du gabarit
> `imperatrice/skills/mission/fiche-mission.md`, après le plan de l'`afficheur` pour la 012
> (`pilotage/missions/012-plan.md` sur la branche `mission/012-ecran-controle-de-placette`,
> 085eb5c) et les décisions de Pascal du même jour. Première des quatre missions de la 012
> (A session, B écran, C volume et poche, D dictée). Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire en Kotlin pur la session de contrôle d'une
placette (pointer, dire l'état, corriger, annuler, regarnir, terminer, totaux, refus avec leurs
codes de la spec § 6) sur le journal de la tournée ouverte, et brancher
`DepotTournees.placettesControlees` et le déclenchement de la sauvegarde, pour que les écrans de
la 012-B, C et D n'aient plus qu'à appeler une logique déjà testée et que la liste des placettes
sache ce qui est fait.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : spec § 4.3, § 5 (journal), § 6 (règles et codes), § 4.8 (sauvegarde) ; inventaire du plan de la 012, § 3 | |
| A-t-il les moyens ? | oui : domaine 004, journal 008/031/032, sauvegarde 009, fixtures du `testeur`, toutes dans `main` | |

**Temps de la main :** **appuyer** (point de suivi à l'ouverture de la PR ; toute question de
sens remonte à l'orchestrateur avant d'être codée).

## L'Homme

- **Agent :** `dev`
- **Modèle :** sonnet
- **Ce qui ne lui est pas demandé :** aucun écran Compose, aucun bouton « Contrôler », aucune
  touche de volume réelle, aucune voix, aucune dictée (012-B, C, D) ; ni export du journal
  (mission d'export), ni sortie de la sauvegarde hors de l'appareil (010).

## Décisions de Pascal à respecter (2026-10-27)

1. **« Contrôlée » à « Terminer »** (Q2 b) : une placette devient « Contrôlée » quand l'agent
   termine la placette, pas à la première saisie ni à l'ouverture. `terminer()` **fige les
   numéros** (spec § 6, numérotation de Groma) et émet l'événement `FIN_PLACETTE` de la
   sauvegarde. Une placette commencée mais non terminée reste « à faire ».
2. **Une placette terminée reste « Contrôlée » pour toute la tournée** (Q11 a), même les jours
   suivants ; un second contrôle un autre jour reste permis (un contrôle par jour, spec § 4.3),
   il n'est pas réclamé.
3. **Pointage au clavier** (Q3) : la session reçoit distance horizontale et azimut magnétique
   saisis (unités déclarées à l'ouverture de la tournée, ADR 0003) ; aucune boussole du
   téléphone.
4. **Cause d'un plant atteint** (Q5) : la session accepte la cause en même temps que l'état ou
   juste après (pour l'écran tant que la dictée n'est pas livrée).
5. **« vide » et « fin du vide »** (Q6) : rien n'est tracé en tranche 1 ; la session répond un
   refus nommé « pas encore disponible » (sans code de la spec § 6 inventé ; si un code est
   nécessaire, le demander).
6. **Regarni** (Q7) : « remplace » désigne le **plant courant**, mort ou absent ; le plant
   pointé ensuite est son regarni, lié par `remplace_uuid` (« regarni toujours lié », décision
   du 2026-10-16, spec § 6).
7. **État précédent** (Q8) : la session expose, pour chaque plant, l'état du passage précédent
   (lu par `LecteurTournee`, `plants` et `etats_plants` de sommieR) à côté de l'état du jour.
8. **Opérateur** (Q9) : l'`operateur` de chaque ligne est `projet.operateur` de la tournée.
9. **Format de `valeur`** (Q10) : la 012-A l'écrit, en code (constantes et sérialisation
   uniques) et dans une note `docs/formats/journal-valeurs-v1.md`, à côté de
   `docs/formats/sursync-v1.md` ; les clés suivent les noms de colonnes de la spec § 5 et § 4.7
   quand ils existent. Cette note sera relue par la mission d'export.

## Choix techniques laissés à l'agent (à dire dans la PR)

- Module (`:core` ou `:data`), port de journal, horloge injectée.
- La **liste fermée des cinq types** de ligne (`TypeLigne.TOUS`, spec § 5) ne change pas :
  « terminé » s'écrit avec les types existants (par exemple dans la `valeur` d'une ligne
  `controle`). Si c'est impossible, s'arrêter et le dire : un nouveau type est une décision de
  Pascal.
- Version de sommieR de l'en-tête de sauvegarde : `projet.version_sommier` de la tournée,
  `null` si vide (sommieR la laisse vide aujourd'hui, spec § 7). Sauvegarde déclenchée hors du
  fil principal.
- Table des gestes (spec § 4.3, colonne « en placette ») en fonction pure (geste vers action),
  testée ici ; la lecture des vraies touches est la 012-C.

## Harnais

- **Peut :** `:core`, `:data`, `app/.../tournee/DepotTournees.kt` (`placettesControlees`), et
  l'instanciation de la sauvegarde dans `app` sans écran ; `docs/formats/` (nouvelle note) ;
  `gpkg` pour passer la table des codes de refus en production.
- **Ne doit pas :** modifier `fixtures/` ; ajouter un écran ; toucher à `main` ; modifier
  sommieR, Marculus, Groma ou `terrain` ; inventer un code (état, cause, essence, refus) ;
  modifier `pilotage/journal.md` autrement qu'en ajout ; trancher une question de sens (la
  remonter à l'orchestrateur).
- **Sources autorisées :** spec ; ADR 0003, 0004, 0005 ; plan de la 012 (`012-plan.md`, § 3 et
  § 5, étape 1) ; fiche 012.

## Tests

Commande du projet : `.claude/test-command` (en local, tâche par tâche avec
`--max-workers=1` si le lancement d'un bloc échoue ; `clean` du module sur `NoSuchFileException`
ou `EOFException`). Vu rouge au moins une fois sur la règle la plus dangereuse, montré dans la PR.

- table des gestes en placette (volume haut et bas, court et long, double appui bas = annule) ;
- **une placette commencée et non terminée n'est pas « Contrôlée »** ; terminée, elle l'est ;
  terminée hier, elle l'est encore aujourd'hui ; une annulation de la fin la rend « à faire » ;
- une saisie annulée ne compte pas (`annulee = 0`, comme `compterSaisies`) ;
- **lecture avec le code de la tournée ouverte** : un test prouve qu'un oubli (défaut `""` de
  `etat(...)`) rendrait un autre état (relecture de la 032) ; une placette d'une autre tournée
  n'influe pas ;
- **date incohérente** (`CLAUDE.md`) : travaux sur une plantation plus âgée que l'itinéraire
  prévu ; jour civil du fix GNSS, sinon heure du téléphone ;
- **placette en Somalie** : une placette française tombe en France ; un fix hors de France ne
  pointe rien ;
- cas de `fixtures/regles/plants/`, `un-controle-par-jour.json`, `rattachement-15m.json`,
  `emprise-et-france.json` rejoués **à travers la session** ; `fixtures/export/scenario-couchey.json`
  rejoué par la session puis relu par l'export sans écart avec `registre-attendu-couchey.json` ;
- **codes de refus** : chaque `RefusPlacette` a son code de la spec § 6 ; la table quitte
  `gpkg/src/test/.../acceptation/BranchementExport.kt` pour le code de production, et le test
  d'acceptation l'appelle depuis la production ;
- regarni lié au plant courant ; regarni d'un plant sain refusé ;
- sauvegarde : `SuiviDeclenchement` appelé à chaque saisie et à `terminer()`,
  `fixtures/sauvegarde/declenchement.json`.

## Terrain

- **Échantillon que Pascal vérifiera :** rien à l'écran (aucun écran dans cette mission) ;
  Pascal lit la note de format et la liste des tests dans la PR.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal.
- **Point de suivi :** à l'ouverture de la PR.
- **Qui tranche :** Pascal. **Fusion : autorisation à demander à Pascal** à la livraison.

## Comment saura-t-on que ça marche ?

Tests verts en CI ; puis, avec la 012-B, une placette terminée passe « Contrôlée » dans l'écran
« Placettes » sur la release (capture).
