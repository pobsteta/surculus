# Surculus — brief

> **Brief amendé le 15 octobre 2026**, après la critique du `contradicteur` (porte n° 1),
> d'après les arbitrages de Pascal (voir `pilotage/journal.md`). Rédigé par l'orchestrateur,
> à valider par Pascal. Le détail technique est dans `docs/spec.md` ; ce brief n'en
> recopie rien.
>
> **Amendé le 22 octobre 2026** (style commun, liste du départ), après la critique du
> `contradicteur`, d'après les quatre arbitrages de Pascal du même jour (journal).

## Ma réponse d'abord

**2026-10-22, Pascal** (amendement : thème commun avec Groma et Marculus, liste des
travaux au départ ; recopié mot pour mot de sa réponse dans la session) :

> le thème commun donne une cohérence au projet nemeton Marculus, Groma et Surculus
> soutienne le projet d'où l'intérêt d'un style commun. Comme Marculus, Surculus peut
> lister des tavaux de plusieurs projets

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
  - un **constat de reprise en cheminement** selon la **méthode cemagref** reprise par
    l'INRAE et l'ONF (arbitrage de Pascal : le constat sert d'abord la décision de
    regarni ; c'est la méthode la plus robuste quand la mortalité se regroupe), pour le
    taux à l'échelle de la plantation ; comptage en plein pour les peupliers, les hautes
    tiges et les petits chantiers
    ([INRAE RENFOR](https://renfor.hub.inrae.fr/ressources/articles-techniques/protocole-constat-de-reprise)).
- **Ce qu'elle produit** : la tournée complétée (interventions localisées, placettes,
  plants et leurs états, photos) ; le taux de reprise à l'échelle de la plantation,
  **toujours avec son effectif** ; la densité de vivants par essence objectif, en sortie
  dérivée. sommieR l'importe dans son registre des travaux.
- **Alertes** (arbitrages de Pascal, voir `docs/referentiels/seuils-densites.csv`) :
  - **taux de reprise** sous **80 %**, **90 %** pour les peupliers et les hautes tiges
    (garantie de reprise du fascicule 34 du CCTG ; 80 % est aussi l'échec selon le DSF
    et le seuil de regarni de la plupart des ITTS de l'ONF, qui vont de 60 à 80 % selon
    l'essence et la densité), source affichée à l'écran ;
  - **seulement quand le seuil est au-dessus de la marge** du taux (intervalle de
    confiance) : un comptage en plein alerte toujours ; un sondage vise au moins
    **300 plants** pour que sa marge soit petite (une placette de 10 plants donne un
    taux à ±25 points) ;
  - **vides** : des plants morts ou absents groupés sur **10 ares** ou plus déclenchent
    une alerte même si le taux moyen est bon (ITTS de l'ONF : 10 ares pour les chênes du
    Sud-Ouest, 10 à 20 ares pour le chêne rouge, 50 ares ailleurs).
- **Liste du départ** (arbitrages de Pascal du 2026-10-22) : l'unité reste la
  **tournée de sommieR**. Une tournée ouverte liste d'abord ses **placettes**, telles
  que sommieR les livre avec leur `etat_suivi`. Ce n'est pas une liste de travaux :
  les **travaux à faire** (le prévu de nemetonshiny) attendent que sommieR les porte
  dans la tournée (lot 3) ; Surculus les y lira, jamais ailleurs.
- **Style commun** avec Marculus et Groma, pour la cohérence du projet nemeton, **sans
  jamais passer devant la lisibilité au soleil** : thème clair forcé, contraste du texte
  d'au moins 7:1, couleurs des placettes gardées, essai au soleil par Pascal avant la
  fusion. Le thème vient de la bibliothèque commune `terrain`, pas d'une copie.
- **Plant atteint** : un plant atteint (abrouti, malade) compte comme **vivant** dans le
  taux ; l'atteinte et sa cause sont notées à part pour le gestionnaire.

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
- **Coordonnées** : repères et unités explicites partout, détaillés dans `docs/spec.md`
  (§ 3). Un test vérifie qu'une placette française tombe en France.
- **Référentiels** :
  - **codes travaux** : Surculus n'embarque aucune liste ; il lit la couche `codes` de la
    tournée. La nomenclature relève de sommieR. Les 15 codes actuels de sommieR ne sont
    issus d'aucun référentiel : c'est à régler dans sommieR, l'`acquereur` peut y
    préparer une table de correspondance (fascicule 34 du CCTG, nomenclature de l'aide,
    actions de nemetonshiny) ;
  - **états des plants et causes** : nomenclature du DSF (sain, atteint, mort, absent ;
    causes en **quatre** groupes, détail facultatif), `docs/referentiels/etats-plants.yaml`.
    L'agent dit l'état et la cause observée ; **la perte « indépendante de l'entreprise »
    (fascicule 34) se décide au bureau, dans sommieR**, pas sur le terrain.
- **Lien avec l'existant, tranche 1** : Surculus ne dépend que du `travaux.gpkg` de
  sommieR 0.31.1, sans renommer ses couches ni ses champs. Le GNSS et la géodésie sont
  repris provisoirement de `groma-core` (GPL). nemetonshiny (le prévu) et les
  bibliothèques communes avec Marculus et Groma viennent en tranche ultérieure
  (Marculus est sous GPL-3.0-or-later). Application dédiée à toute la tournée :
  `docs/adr/0001-application-dediee-plutot-que-qfield.md`.
- **Formats et licences** : GeoPackage, logiciel libre sous GPL-3.0-or-later comme Groma.
- **Successeur** : **[à désigner par Pascal]**.

## Comment saura-t-on que ça marche ?

- **Signe concret** : un agent contrôle une placette fixe de 50 m² (une dizaine de
  plants) **déjà pointée** en **moins de 10 minutes** (médiane sur au moins 10
  placettes), piquet retrouvé compris, sans papier, et la tournée rendue s'importe dans
  sommieR 0.31.1 sans une faute. Le premier passage, où chaque plant est pointé, est
  chronométré sans cible.
- **Terrain, automne 2026** : sur une plantation réelle de 1 à 3 ans, **deux opérateurs
  contrôlent les mêmes placettes le même jour, dans deux tournées séparées** ; Pascal
  vérifie que les plants pointés coïncident **à 0,5 m près** et que les deux taux, et le
  sien, sont égaux. Le suivi d'un passage à
  l'autre (mêmes UUID au printemps 2027) n'est pas un critère de la tranche 1.
  **[plantation : à désigner par Pascal]**

## Qui tranche

Pascal Obstétar.
