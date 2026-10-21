# Mission 030 — Référentiels de la PR #4, revérifiés

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21 :
> la PR #4 (ébauche du 2026-10-15, antérieure à la porte n° 1) est fermée ; ses cinq
> documents encore utiles sont repris ici et revérifiés, le reste est abandonné (codes
> travaux contraires à la décision du 2026-10-15, spec, brief et journal périmés).

## Mission

**En une phrase (quoi, et pourquoi) :** Revérifier sur les textes intégraux les cinq documents repris de la PR #4 (coûts de plantation et de regarni, échecs de plantations du DSF, itinéraires techniques CNPF par essence, densités MFR de Bourgogne-Franche-Comté, inventaire des sources), et ne garder dans `docs/referentiels/` que des chiffres sourcés à la page, pour que le coût complet d'une plantation et d'un regarni (spec § 9, question 3) repose sur des chiffres vérifiés.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : CQQCOQP, plusieurs sources comparées, références et non fichiers (décision du 2026-10-15) | |
| A-t-il les moyens ? | oui : les documents de départ sont dans `docs/referentiels/a-reverifier/` sur cette branche ; WebFetch avait échoué le 2026-10-15 (ENOTFOUND), à réessayer | si les sites restent injoignables, il s'arrête et le dit |

**Temps de la main :** **appuyer** : l'agent rend son plan avant d'écrire.

## L'Homme

- **Agent :** `acquereur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** aucune liste de codes travaux (la nomenclature relève de sommieR) ; aucun calcul de coût complet pour Surculus (c'est à Pascal) ; aucune modification de la spec ou du brief.

## Harnais

- **Peut :** écrire dans `docs/referentiels/` seulement : déplacer chaque document revérifié de `a-reverifier/` vers `docs/referentiels/`, le corriger, le compléter ; mettre à jour `docs/referentiels/README.md` (tableau des référentiels, statut « à arbitrer ») ; vider puis supprimer `a-reverifier/`.
- **Ne doit pas :** garder un chiffre « extrait » sans le dire ; garder un chiffre qu'il n'a pas lu dans le texte intégral sans le marquer « non vérifié » ; inventer un code ; versionner un PDF (`.gitignore`) ; toucher à `main` ; trancher une question de sens (la remonter à l'orchestrateur).
- **Sources autorisées :** DSF (bilans nationaux des plantations), CNPF (fiches « Itinéraires techniques par essence », 2020), DRAAF Bourgogne-Franche-Comté (arrêté MFR), barèmes d'aide publiés, Cour des comptes ; toujours le texte publié, cité à la page.

## Tests

- Chaque ligne de chiffres porte sa source, sa page et un niveau de vérification (`verifie`, `extrait`, `non_trouve`).
- Aucune ligne `verifie` sans page ni URL du texte intégral.
- Les écarts entre sources sont listés en tête de chaque fichier.
- `a-reverifier/` n'existe plus à la fin : chaque document est soit déplacé et revérifié, soit retiré avec sa raison dans la PR.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal (statut « à arbitrer »).

## Comment saura-t-on que ça marche ?

Pascal ouvre deux lignes au hasard, suit le lien et la page, et y lit le même chiffre.
