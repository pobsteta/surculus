# Mission 027 — Registre attendu du scénario Couchey

> Fiche écrite par l'orchestrateur le 2026-10-21, à partir du gabarit
> `fiche-mission.md` du skill `imperatrice:mission`. Décidée par Pascal le 2026-10-21
> (critère F4) : les valeurs attendues au registre sont écrites à la main par le
> `testeur`, depuis la spec, pour que l'export ne se compare pas à lui-même.

## Mission

**En une phrase (quoi, et pourquoi) :** Écrire à la main, depuis la spec et sans lire le code de `:gpkg`, le registre attendu après import du scénario Couchey (`fixtures/export/scenario-couchey.json`) : pour chaque contrôle, `nb_total`, `nb_vivants`, `nb_abroutis`, `h_moy_cm`, `visite_le`, `placette` désignée par son code, et le point en WGS 84 avec sa tolérance, qui doit tomber en France métropolitaine, pour que la CI vérifie que le contrôle du forestier arrive juste au registre.

**Redite par l'agent :** _à compléter par l'agent avant d'agir — s'il n'y arrive pas, il s'arrête._

## Les trois questions (règle de staffing)

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase, avec son but ? | oui | |
| Sait-il comment faire ? | oui : spec § 4.3 et § 6 (totaux, `h_moy_cm` demi vers le haut), format des cas des missions 019, 020 | |
| A-t-il les moyens ? | oui : scénario Couchey, démo Couchey de sommieR | |

**Temps de la main :** **appuyer** : l'agent rend ses calculs avant d'écrire.

## L'Homme

- **Agent :** `testeur` — **Modèle :** sonnet.
- **Ce qui ne lui est pas demandé :** brancher ces attentes dans la CI (mission 028) ; lire `gpkg/src/` ou `ci/sommier/`.

## Harnais

- **Peut :** écrire `fixtures/export/registre-attendu-couchey.json` (chaque valeur avec sa source et son calcul), compléter `fixtures/README.md`.
- **Ne doit pas :** modifier le scénario ; inventer une règle (il la remonte) ; toucher à `main`.

## Qui veille, qui tranche

- **Relecteur :** `relecteur`, puis Pascal. **Qui tranche :** Pascal.

## Comment saura-t-on que ça marche ?

Pascal refait le calcul d'une placette à la main et retrouve les mêmes nombres.
