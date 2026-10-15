# Surculus — brief

> **Brief amendé le 15 octobre 2026**, après la critique du `contradicteur` (porte n° 1),
> d'après les arbitrages de Pascal (voir `docs/journal.md`). Rédigé par l'orchestrateur,
> à valider par Pascal. Le détail technique est dans `docs/spec.md` ; ce brief n'en
> recopie rien.

## Le problème, et pour qui (Abstraction)

- **Problème** : le contrôle de reprise existe déjà, par totaux, dans sommieR et son
  projet QField (contrôle daté, localisé, rattaché à sa placette). Ce qui manque, c'est
  de **suivre le même plant d'un passage à l'autre** : savoir lequel est mort, lequel a
  été abrouti, lequel est un regarni, et pourquoi.
- **Pourquoi pas QField** (constat de Pascal) : plant par plant, QField est trop lent
  (un formulaire par plant), impossible à une main et gants aux doigts, ne retrouve pas
  le même plant d'un passage à l'autre et ne guide ni jusqu'au piquet ni d'un plant au
  suivant.
- **Bénéficiaires** :
  1. d'abord le **gestionnaire forestier** qui tient le sommier et décide des regarnis,
     dégagements et protections ;
  2. ensuite le **propriétaire**, qui paie ces travaux et doit parfois justifier une aide
     (densité de plants vivants d'essences objectif) : sortie dérivée, pas l'objet
     premier.
- **Coût complet aujourd'hui** : ordres de grandeur du DSF, faute de chiffres propres :
  15 % des plantations de l'année en échec en 2024, 38 % en 2022, 19 % en moyenne sur
  2007-2022 ([DSF, bilan 2024](https://agriculture.gouv.fr/telecharger/148150)). Un échec
  vu trop tard, c'est un regarni impossible ou payé deux fois. **[surfaces plantées par
  an, prix d'un regarni et temps de ressaisie : à compléter par Pascal]**

## Ce que fait Surculus

- **En une phrase** : application Android de terrain, hors ligne, qui suit plant par
  plant, sur des placettes fixes, ce qui a repris d'un passage à l'autre, et relève les
  travaux faits, pour le rendre au sommier forestier.
- **Les gestes sur le terrain** : l'agent ouvre la tournée préparée par sommieR ; il est
  guidé jusqu'au piquet de chaque placette, puis d'un plant au suivant ; il dit l'état de
  chaque plant à la voix ou aux boutons de volume, sans regarder l'écran et sans réseau ;
  il trace au GNSS l'intervention faite.
- **Deux protocoles, deux usages** :
  - les **placettes fixes** de sommieR, plants pointés, pour le suivi dans le temps
    (gestionnaire) ;
  - un **constat de reprise en cheminement** sur les lignes, selon une convention publiée
    (méthode cemagref reprise par l'INRAE et l'ONF, ou protocole du DSF), pour le taux à
    l'échelle de la plantation
    ([INRAE RENFOR](https://renfor.hub.inrae.fr/ressources/articles-techniques/protocole-constat-de-reprise)).
- **Ce qu'elle produit** : la tournée complétée (interventions localisées, placettes,
  plants et leurs états, photos) ; le taux de reprise à l'échelle de la plantation,
  **toujours avec son effectif** ; la densité de vivants par essence objectif, en sortie
  dérivée. sommieR l'importe dans son registre des travaux.
- **Alerte** : sous le seuil de reprise, et seulement au-delà d'un effectif minimal
  (une placette de 10 plants donne un taux à ±13 points). Le seuil retenu cite sa
  source (80 % : échec selon le DSF ; 70 % : regarni à N+2 selon l'ONF).
  **[effectif minimal et seuil : à fixer par Pascal]**

## Ce qu'elle ne fait pas (hors périmètre)

Voir `docs/spec.md`, « Hors périmètre ». En bref : ni programmation ni chantier, ni
calculs du bilan, ni martelage, ni inventaire. Surculus **n'écrit jamais dans la base de
sommieR** : il remplit un fichier, l'import de sommieR décide.

## Contraintes connues

- **Terrain** : tout fonctionne hors ligne ; saisie à une main, sans regarder l'écran.
  Le matériel (GNSS RTK, repli, outils de pointage) est décrit dans la spec et tranché
  en ADR. **[matériel de pointage requis : à nommer]**
- **Appareil perdu ou cassé** : le journal des saisies est sauvegardé hors de l'appareil
  à chaque fin de placette (fichier local partageable sans réseau). Une tournée ne se
  perd pas avec le téléphone.
- **Coordonnées** : tous les points en Lambert-93 (EPSG:2154) dans le GeoPackage ;
  azimut en degrés depuis le **nord géographique**, sens horaire ; distance
  **horizontale** en mètres. Un test vérifie qu'une placette française tombe en France.
- **Référentiels** :
  - **codes travaux** : Surculus n'embarque aucune liste ; il lit la couche `codes` de la
    tournée. La nomenclature relève de sommieR. Les 15 codes actuels de sommieR ne sont
    issus d'aucun référentiel : c'est à régler dans sommieR, l'`acquereur` peut y
    préparer une table de correspondance (fascicule 34 du CCTG, nomenclature de l'aide,
    actions de nemetonshiny) ;
  - **états des plants et causes** : nomenclature du DSF (sain, atteint, mort, absent ;
    causes en cinq groupes ; pertes indépendantes de l'entreprise distinguées), déposée
    par l'`acquereur` dans `docs/referentiels/`.
- **Lien avec l'existant, tranche 1** : Surculus ne dépend que du `travaux.gpkg` de
  sommieR 0.31.1, sans renommer ses couches ni ses champs. Le GNSS et la géodésie sont
  repris provisoirement de `groma-core` (GPL). nemetonshiny (le prévu) et les
  bibliothèques communes avec Marculus et Groma viennent en tranche ultérieure ;
  **préalable : Pascal licencie Marculus**.
- **Formats et licences** : GeoPackage, logiciel libre sous GPL-3.0-or-later comme Groma.
- **Successeur** : **[à désigner par Pascal]**.

## Comment saura-t-on que ça marche ?

- **Signe concret** : un agent contrôle une placette fixe de 50 m² (une dizaine de
  plants) en **moins de 10 minutes**, piquet retrouvé compris, sans papier, et la tournée
  rendue s'importe dans sommieR 0.31.1 sans une faute.
- **Terrain, automne 2026** : sur une plantation réelle de 1 à 3 ans, **deux opérateurs
  contrôlent les mêmes placettes le même jour** ; Pascal vérifie que les plants pointés
  coïncident et que les deux taux, et le sien, sont égaux. Le suivi d'un passage à
  l'autre (mêmes UUID au printemps 2027) n'est pas un critère de la tranche 1.
  **[plantation : à désigner par Pascal]**

## Qui tranche

Pascal Obstétar.
