# Mission 031 — La tournée dans chaque ligne du journal

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21,
> à la relecture de la 009 : le journal de la 008 n'a pas de champ tournée, donc une
> sauvegarde `.sursync` contient tout le journal du téléphone, et la 011 ouvrira
> plusieurs tournées. À fusionner avant la 011.

## Mission

**En une phrase (quoi, et pourquoi) :** Ajouter à chaque ligne du journal le code de la tournée à laquelle elle appartient (schéma Room, migration testée, spec § 5, format `.sursync`), et faire que la sauvegarde et la restauration ne portent que sur une tournée, pour que le fichier d'une tournée ne contienne jamais les saisies d'une autre.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : `data/journal/` (008), `exportSchema = true` et schémas dans `data/schemas/`, format `docs/formats/sursync-v1.md` (009) | |
| A-t-il les moyens ? | oui une fois la 009 fusionnée | ne part pas avant |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire. Le plan dit
en particulier : (1) ce que devient une ligne existante sans tournée à la migration
(aucun repli destructif, ADR 0005) ; (2) si le format `.sursync` reste v1 (aucun
fichier v1 n'existe encore hors des fixtures, l'application n'en écrit pas) ou passe en
v2 ; (3) l'effet sur la projection et sur les cas `fixtures/journal/` et
`fixtures/sauvegarde/`, qui ne sont pas modifiés sans accord.

## L'Homme

- **Agent :** `dev` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** aucun écran ; aucun branchement de la sauvegarde sur les saisies ; aucune fusion entre tournées (ADR 0006).

## Harnais

- **Peut :** modifier `data/` (journal, sauvegarde), ajouter une migration Room et son schéma, amender la spec § 5 (champs de la ligne de journal) et `docs/formats/sursync-v1.md`, compléter l'ADR 0005.
- **Ne doit pas :** modifier ou supprimer une ligne du journal autrement que par la migration ; casser un test existant ; modifier les fixtures du testeur sans accord ; toucher à `main` ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** spec § 5 ; ADR 0005 ; fiches 008 et 009 ; journal du 2026-10-21.

## Tests

- La migration Room est testée sur une base de la version précédente, lignes conservées.
- Deux tournées sur le même téléphone : la sauvegarde de A ne contient aucune ligne de B ; la restauration de A ne touche pas B.
- Tous les tests existants restent verts. Commande du projet : `.claude/test-command`.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Un `.sursync` écrit sur un téléphone qui porte deux tournées ne contient que les saisies
de la sienne.
