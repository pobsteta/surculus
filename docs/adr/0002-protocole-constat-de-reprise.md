# ADR 0002 — Protocole de constat de reprise : cemagref à deux départs par zone, trois états d'alerte

- **Date :** 2026-10-15
- **Statut :** acceptée par Pascal (2026-10-15), après critique du `contradicteur`
- **Proposée par :** Claude (orchestrateur)
- **Tranchée par :** Pascal

## La décision en une phrase

Le constat de reprise en cheminement suit la méthode cemagref, avec deux départs tirés
au sort dans chacune de H zones du chantier ; on compte au moins 300 plants (taux de
sondage = max(grille cemagref, 300/N), comptage en plein dès N ≤ 600) ; le taux est
estimé par le rapport des sommes, sa marge par la variance entre départs dans les zones ;
et Surculus dit « sous le seuil », « au-dessus » ou « indéterminé » avec un intervalle à
90 %, « indéterminé » déclenchant un comptage en plein.

## Contexte

Le constat sert la **décision de regarni du gestionnaire** (arbitrage de Pascal, spec
§ 4.4). Ce que le gestionnaire doit recevoir à temps : « regarnissez », « ne regarnissez
pas » ou « comptez tout ». Rater un regarni coûte une saison (regarni « au plus tard dans
les deux ans », ITTS plateau landais ; « après la première année de végétation »,
CNPF) ; une fausse alerte coûte une visite.

**Erreur corrigée** (l'orchestrateur s'était trompé) : un seul couple de départs donne
une variance sans biais mais sur **un degré de liberté** ; le quantile de Student vaut
alors 12,7, la marge devient inutilisable et nulle quand les deux taux coïncident.

Options envisagées :

1. **Cemagref, deux départs dans chaque zone** (retenue par Pascal) : environ H degrés de
   liberté ; un cheminement interrompu laisse des zones complètes.
2. **Cemagref, un seul départ, différences successives** (estimateurs v2/v3 de Wolter) :
   beaucoup de degrés de liberté, biais connu et négatif sous forte tendance (Wolter 1984,
   tableau 6).
3. **Deux départs pour tout le chantier** : 1 degré de liberté, écartée.
4. **Guide 2014, lignes entières** : opposable en réception, moins efficace quand la
   mortalité suit les lignes ; écartée pour le regarni.

Sur la robustesse : RENFOR trouve au cemagref un gain quand la mortalité suit les lignes,
et pas de perte sinon (« n'apporte rien » sous mortalité aléatoire, « pas de gain
significatif » sous mortalité regroupée sans effet de ligne). C'est un choix prudent, pas
« le plus robuste dans tous les cas ».

## Règles

| Paramètre | Valeur | Origine |
|---|---|---|
| Taux de sondage | **max(grille cemagref, 300/N)** ; grille : 50 % (≤ 400), 25 % (401-600), 16 % (601-1 100), 10 % (1 101-2 500), 5 % (> 2 500) | RENFOR ; arbitrage de Pascal |
| Comptage en plein | densité définitive (peupliers, noyers, hautes tiges), ou **N ≤ 600** (300/N ≥ 50 %) | fascicule 34 ; arbitrage de Pascal |
| Zones | H bandes de lignes contiguës, de taille voisine ; **deux départs tirés sans remise dans chaque zone**, au pas double | Cochran, ch. 8 (cité de mémoire) |
| Domaine du tirage | ligne de départ parmi celles de la zone **et** position dans le motif 2-3 plants ; règle de bordure fixée avant de tirer (un segment qui sort de la plantation reprend à la ligne suivante, sans recompter) | `contradicteur` |
| Estimateur | **rapport des sommes** (vivants comptés / plants comptés), pas une moyenne de quotients | Cochran, ch. 6 |
| Variance | entre les deux départs de chaque zone, sommée sur les zones (environ H ddl), par linéarisation ; correction de population finie sur les départs possibles ; quantile de Student à H ddl | Wolter 2007, ch. 2 (groupes aléatoires) |
| Plancher | la marge n'est jamais plus étroite que la marge binomiale du même effectif | `contradicteur` |
| Marge affichée | à partir de **H ≥ 6** zones complètes (à éprouver au premier essai contre un comptage en plein) | hypothèse, critère de la tranche 2 |
| Périodicité | le pas ne doit pas être un multiple de la période des cloisonnements ou des lignes d'essences alternées ; si la tournée ne les porte pas, **l'agent les déclare avant le tirage** | Wolter 1984, population A7 |
| Sans lignes lisibles | **placeaux entiers** comptés comme grappes quand ils existent (variance entre placeaux) ; sinon placettes à surface fixe et rendu en **densité de vivants à l'hectare** (comparée aux densités de l'aide), pas en taux ; à défaut, comptage en plein | `contradicteur` ; `seuils-densites.csv` |
| Alerte | intervalle à **90 %** (risque de fausse alerte de 5 % de chaque côté) : borne haute sous le seuil = **« sous le seuil »** ; borne basse au-dessus = **« au-dessus »** ; sinon **« indéterminé »** → **comptage en plein** proposé (comme le guide 2014 en cas de désaccord). Comptage en plein : comparaison directe au seuil | arbitrage de Pascal |
| Seuil | 80 %, 90 % pour peupliers et hautes tiges (fascicule 34), source affichée | arbitrage de Pascal |

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte |
|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Non pour le geste ; oui pour la décision : trois états clairs, une action par état. | Plus de marche (diagonale, deux départs par zone) ; environ deux fois plus de plants comptés sur 1 ha (300 au lieu de 160). |
| **R**esponsable | Qui fait quoi ? | Surculus tire, guide, calcule ; l'agent compte et déclare les cloisonnements ; le gestionnaire décide du regarni et du « hors entreprise ». | Le taux du téléphone est brut (nm = 0) : il ne vaut pas réception. |
| **A**nticipateur | Si l'imprévu arrive ? | Un cheminement interrompu garde ses zones complètes ; « indéterminé » a une action définie. | Les données de la plantation manquent à la tournée 0.31.1 : saisie sur le terrain en attendant sommieR. |
| **N**ovateur | Toutes les idées ? | Quatre protocoles, trois estimateurs, trois règles d'alerte examinés. | Une demi-journée de calculs à écrire et à tester par le `testeur`. |
| **C**oût | Coût pour l'ensemble ? | Une fausse alerte de plus sur vingt contre beaucoup moins de regarnis ratés. | Temps de comptage ; comptages en plein déclenchés par « indéterminé ». |
| **É**cologique | Que réutilise-t-on, qui réutilisera ? | Conventions publiées (cemagref, guide 2014, fascicule 34) ; code libre. | Un protocole outillé qui n'est pas celui de la réception contractuelle. |

## Valeurs sacrifiées

- **Frugalité du geste** : diagonale, deux départs par zone, 300 plants. Acceptée pour une
  alerte de regarni fiable.
- **Sobriété des fausses alertes** : intervalle à 90 % plutôt que 95 %. Acceptée : une
  fausse alerte coûte une visite, un regarni raté une saison.
- **Opposabilité** : le taux ne vaut pas réception de chantier.

## Sources

- `docs/referentiels/protocole-constat-reprise.md`, `docs/referentiels/seuils-densites.csv` ;
  `docs/spec.md` § 4.4-4.5.
- INRAE RENFOR, « Protocole constat de reprise » (2021, modifié 2023), tableau 2 et
  recommandations : https://renfor.hub.inrae.fr/ressources/articles-techniques/protocole-constat-de-reprise
- Wolter, RR-84-02, U.S. Census Bureau (1984), p. 1, p. 22, tableaux 6 et 8 :
  https://www.census.gov/library/working-papers/1984/adrm/rr84-02.html ;
  *Introduction to Variance Estimation*, Springer, 2007, ch. 2 et 8 (non lu).
- Gautschi, « Some Remarks on Systematic Sampling », *Ann. Math. Statist.* 28(2), 1957 :
  https://doi.org/10.1214/aoms/1177706966 (résumé seul).
- Cochran, *Sampling Techniques*, 3e éd., Wiley, 1977, ch. 6 et 8 (cité de mémoire).
- Table de Student : NIST/SEMATECH e-Handbook,
  https://www.itl.nist.gov/div898/handbook/eda/section3/eda3672.htm
- DSF, bilan 2024, p. 16 (groupes de 10 le long des lignes, absents estimés) :
  https://agriculture.gouv.fr/telecharger/148150

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
