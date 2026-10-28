# Mission 070 — Corriger la citation de Purfürst dans l'ADR 0003

> Fiche écrite par l'orchestrateur le 2026-10-28 (cycle 13), sur l'ordre de Pascal « Code les briefs
> si il y en a qui ne sont pas codés ». Source : brief Ramulus → Surculus du 2026-10-28
> (`~/dev/briefs/vers-surculus/2026-10-28-ramulus-citation-purfurst.md`, recopié ci-dessous).
> Depuis `main`. Une mission = une phrase = une branche = une PR.

## Mission

**En une phrase (quoi, et pourquoi) :** Corriger par un ajout daté, sans rien effacer, la citation de
Purfürst 2022 dans l'ADR 0003 (les chiffres « 6,1 m, CEP95 13,4 m » sont ceux de la ligne « All » du
tableau 4, qui inclut le récepteur de référence Geo 7X) et vérifier qu'aucun seuil de Surculus n'en
dépend, pour que la justification du repli téléphone repose sur les chiffres des seuls téléphones.

**Redite par l'agent :** Ajouter à l'ADR 0003, sous la phrase d'origine conservée, une correction datée du 2026-10-28 qui donne les chiffres des téléphones seuls (attribués à Ramulus, source non relue faute d'accès au web) et dit quels seuils de Surculus en dépendent (aucun).

## Le brief (constat de Ramulus)

- ADR 0003 l. 24-25 : « 6,1 m en moyenne, CEP95 13,4 m, jusqu'à 42 m pour un modèle (Purfürst 2022,
  tableau 4) ».
- Ligne « All » du tableau 4 : inclut le Geo 7X (n = 12 096 sur 131 338).
- Téléphones seuls : moyenne 6,50 m (calculée par l'acquéreur de Ramulus) ; CEP95 13,38 m
  multifréquence, 13,64 m monofréquence ; pire téléphone Xcover 4, CEP95 41,99 m.
- Limite : tableau lu sur le XML d'Europe PMC ; Mi 8 donné à 7,20 m ou 7,22 m (incohérence de la
  source).
- Référence de Ramulus : `docs/referentiels/gnss-telephone.yaml`, écart E1.

## Ce qui est attendu

1. **Relire la source soi-même** (https://pmc.ncbi.nlm.nih.gov/articles/PMC8838512/, tableau 4, ou
   Europe PMC) et refaire le calcul de la moyenne des téléphones seuls ; si l'on ne peut pas lire la
   source, l'écrire (« non lue ») au lieu de recopier Ramulus.
2. **Ajout daté dans l'ADR 0003** (« Correction du 2026-10-28 »), sous la phrase d'origine, qui reste :
   les chiffres des téléphones seuls, la source, la limite, le renvoi au brief de Ramulus.
3. **Seuils** : chercher dans la spec, les ADR et le code tout seuil qui dépend de ces chiffres (fix
   refusé, rayons, 15 m, 0,5 m, précision affichée) ; dire lequel en dépend, ou qu'aucun.
4. Ne trancher aucune conséquence : si un seuil en dépend, le signaler pour Pascal.

## Les trois questions

| Question | Réponse | Conséquence |
|---|---|---|
| Peut-il redire la mission en une phrase ? | oui | |
| Sait-il comment faire ? | oui | |
| A-t-il les moyens ? | oui si la source se lit ; sinon il le dit | |

**Temps de la main :** **appuyer**.

## L'Homme

- **Agent :** `archiviste` (mémoire du projet, ADR).
- **Pas demandé :** changer un seuil ou une décision ; toucher au code.

## Harnais

- **Peut :** `docs/adr/0003-gnss-et-reperes.md` (ajout seul) ; pousser sur
  `mission/070-citation-purfurst` ; ouvrir la PR.
- **Ne doit pas :** effacer une phrase de l'ADR ; toucher au code, à `fixtures/`, à
  `pilotage/journal.md`, à `main` ; trancher une question de sens.

## Qui veille, qui tranche

- **Relecteur**, puis Pascal. 3 allers-retours. **Fusion : autorisation de Pascal.**

## Comment saura-t-on que ça marche ?

L'ADR 0003 porte la phrase d'origine et sa correction datée, sourcée, et dit si un seuil en dépend.
