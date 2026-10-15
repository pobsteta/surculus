---
name: brief
description: Étape Abstraction de la méthode de l'Impératrice. Fait critiquer docs/brief.md par le contradicteur, aide Pascal à l'amender, puis prépare la spec et les ADR FRANCE des décisions structurantes. À lancer avant tout développement.
---
# /brief — prendre de la hauteur avant d'agir

Tu es l'orchestrateur. Objectif : que Pascal valide un brief et une spec qui disent
**quel problème on résout, et pour qui**. Porte n° 1 de la méthode.

## Étapes

1. **Lire `docs/brief.md`.** S'il est encore au stade de gabarit (rubriques en italique
   non remplies), ne lance personne : pose à Pascal les questions manquantes, au plus
   quatre, et attends ses réponses. Le brief est à Pascal ; tu ne l'inventes pas.
2. **Lancer le `contradicteur`** sur le brief, avec pour consigne explicite : critiquer,
   citer ses sources, chercher les conventions publiées (DSF, INRAE, CCTG, guides du ministère) avant
   toute invention.
3. **Rendre la critique à Pascal**, classée, sans l'adoucir. Pour chaque point : ce que
   tu proposes de changer dans le brief. Pascal tranche ; tu reportes ses arbitrages
   dans `docs/brief.md`.
4. **Rédiger `docs/spec.md`** à partir du brief validé : parcours utilisateur sur le
   terrain, données manipulées, ce qui est hors périmètre, critères d'acceptation, et
   les besoins en référentiels (à confier à l'`acquereur`).
5. **Lister les décisions structurantes** (stack, stockage, hors-ligne, GNSS, export,
   licence). Pour chacune, une ADR proposée dans `docs/adr/` au crible FRANCE
   (`docs/modeles/adr-france.md`), avec les valeurs sacrifiées nommées. Le
   `contradicteur` critique chaque ADR avant que Pascal la tranche.
6. Dès que la stack est tranchée, écrire la commande de tests du projet dans
   `.claude/test-command` (une seule ligne, ex. `npm test` ou `Rscript -e 'testthat::test_dir("tests")'`).
7. Ajouter une inscription à `docs/journal.md`.

## Sortie

Clore par une question, pas par un résumé :
**« Valides-tu le brief et la spec ? Qu'avons-nous décidé, qui fait quoi, d'ici quand ? »**
Rien ne passe à `/mission` sans le « oui » de Pascal.
