# Mission 012-B — Plan de l'afficheur (rendu avant tout code, le 2026-10-27)

> Temps de la main : **appuyer**. Aucun code n'est écrit, ni `docs/`, ni le journal. Sources lues : fiche 012-B,
> `CLAUDE.md`, `SessionControle` et `Valeurs.kt` (012-A, `main` à 63b0659), `docs/formats/journal-valeurs-v1.md`,
> `Navigation.kt`, `MainActivity.kt`, `EcranCarte.kt`, `EcranGuidage.kt`, `Guidage.kt`, `Palette.kt`, plan 012 (§ 2, § 5
> étape 2, § 7), fiche 063 (décisions du 2026-10-27), spec § 1 et § 3, ADR 0003, Groma `Position.kt:79-111`
> (lecture seule). Les `fichier:ligne` sont ceux de `main` à 63b0659. **La 063 n'est pas fusionnée** : je suis ses règles
> de couleurs sans dépendre de son code (voir § 2).

## 1. Redite

Réaliser l'écran où le forestier, debout dans la placette, pointe les plants (azimut lu par la boussole du téléphone,
distance tapée), dit leur état et leur cause, pose les regarnis, voit « 7 sur 12 » et les refus avec leur code, et
termine la placette, en l'ouvrant par « Contrôler » sur la carte ou à l'arrivée du guidage et en l'appuyant sur la
`SessionControle` de la 012-A, pour que Pascal contrôle une placette de bout en bout sur son téléphone et la voie
passer « Contrôlée » dans l'écran « Placettes ».

## 2. Ce que l'écran montre (fiche d'affichage)

* **Question** : « sur quel plant suis-je, quel est son état, combien en reste-t-il ? »
* **Pour** : le forestier en forêt, gants, soleil ou pluie, une main libre, sans réseau.
* **Une seule chose en grand** : « Plant n° 7 » et son état du jour (mot + pastille), ou « pas encore dit ».
* **Comparaison** : « 7 sur 12 » (dits sur pointés) ; état d'aujourd'hui contre état précédent, en petit et en gris.
* **Source et date, en petit** : code de la placette et jour du contrôle (`EtatSession.jour`), badge du fix. La date
  de la dernière sauvegarde n'est pas exposée par la session (`EcouteSauvegarde` ne la rend pas) : non montrée (§ 7).
* **Lieu** : une main, gants : cibles d'état de 64 dp, aucune cible sous 48 dp ; pas de réseau requis.

**Règle du 1664 : une idée par vue, deux vues.** La vue **Plant** (par défaut) dit l'état. La vue **Pointer** place un
nouveau plant. Un appui sur « + Pointer » ouvre la seconde ; « Pointer » la referme avec le nouveau plant courant,
prêt à recevoir son état. Une troisième vue, **Terminer**, confirme avec les totaux.

Croquis, vue Plant (portrait, six zones, quatre couleurs au plus : vert, orange, rouge, noir, sur fond du thème) :

```
┌──────────────────────────────────────┐
│ ←  P35-01 · 27 oct.        [RTK fixe]│  bandeau compact (BandeauCompact) : source, date, fix
├──────────────────────────────────────┤
│  Plant n° 7              7 sur 12    │  1. EN GRAND : numéro ; « 7 sur 12 » en titre moyen
│  ● sain                              │  2. état du jour : pastille + mot (« pas encore dit »)
│  avant : atteint                     │  3. état précédent, petit, gris
│ ┌────────┐┌────────┐                 │
│ │  Sain  ││Atteint │                 │  4. quatre états, 64 dp, deux par ligne
│ ├────────┤├────────┤                 │     (après Atteint ou Mort : les quatre groupes de cause,
│ │  Mort  ││ Absent │                 │      + « Sans cause »)
│ └────────┘└────────┘                 │
│        ·  ⊙ ·                        │  5. plan à l'échelle (rayon en mètres), toucher un plant
│     ·   [P]  ●                       │     = le choisir ; courant cerné ; nord en haut
│ [+ Pointer] [Annuler] [Terminer]     │  6. trois actions, 56 dp
└──────────────────────────────────────┘
```

* **Pastille d'état** : sain vert, atteint orange, mort rouge, absent noir, **forme redondante** (disque plein,
  demi-disque, croix, anneau vide) pour ne pas porter l'état par la seule couleur (question Q7).
* **Règles de la 063 appliquées d'emblée** (sans toucher à `plaqueEtat()`, qui reste en l'état jusqu'à la fusion de
  la 063) : en sombre, **texte en `onSurface`**, état porté par la **pastille cernée d'un filet blanc**, erreurs et refus
  en `colorScheme.error`, **aucune plaque blanche** ; en clair, rien de neuf. L'écran ne lit pas `Palette.*` pour un
  texte : seulement pour la pastille et le plan. Si la 063 est fusionnée avant le code, je reprends ses aides.
* **Vue Pointer** (une idée : « où est ce plant ? ») :

```
│ ←  Pointer un plant                  │
│  Cap (magnétique)     143°  ± 8°     │  en grand : l'azimut lu, vivant
│  Distance (m)        [ 6,2 ]         │  clavier numérique, point ou virgule
│  [      POINTER LE PLANT      ]      │  64 dp
│  tenir le téléphone à plat, bord     │  1 ligne d'aide, remplacée par
│  haut vers le plant                  │  l'alerte de précision quand il le faut
```

* **Vue Terminer** : « 12 plants · 9 vivants · 2 abroutis · hauteur moyenne 78 cm » (`EtatSession.totaux`) et deux
  boutons « Terminer » / « Reprendre ». Sans état dit : le message `RienASaisir`, en clair, et le bouton « Terminer »
  inactif.
* **Refus** : en `colorScheme.error`, « **hors_15_m** : la placette est à 22 m, au-delà de 15 m. Lisez le code du piquet. »
  (le `code` de la spec § 6 d'abord, le `message` de la session ensuite) ; jamais un reproche, la règle seulement.
  **Pointage hors rayon** : ce n'est pas un refus mais `SuiteSession.AConfirmer` : phrase orange et deux boutons
  « Confirmer » / « Refaire ».
* **Alerte « placette déjà terminée »** (décision 7) : bandeau permanent sous l'en-tête quand `EtatSession.termine`
  est vrai, en `colorScheme.error` container ; la saisie reste possible, la placette reste « Contrôlée ».
* **« vide »** : aucun bouton (décision 9 : « bouton absent ou message »). Je propose **absent** : un bouton qui répond
  « pas encore disponible » apprend à ne pas s'y fier.
* **Pièges de la fiche n° 7** : aucun classement ni score (image qui accuse) ; « 7 sur 12 » et un plan à l'échelle (pas
  d'axe tronqué) ; jour du contrôle toujours affiché (pas d'image figée sans date) ; un seul chiffre en grand.

## 3. Navigation (deux portes, retours, récepteur GNSS)

**État de navigation** (`Navigation.kt`) : on ajoute `controleId: String?` (la placette en cours de contrôle). On ne
réutilise pas `placetteId`, qui veut dire « guidage vers » (`Navigation.kt:28`, `MainActivity.kt:91`).

| Porte | Transition | Retour |
|---|---|---|
| **Carte, « Contrôler »** (bouton à côté de « naviguer vers », `EcranCarte.kt:459-465`, visible quand une placette est choisie) | `controler(id)` = `copy(controleId = id, centreId = id)`, `placetteId = null`, carte gardée | `retour()` → la carte, recentrée sur cette placette (`choisieUuid` repart de `centreId`, `EcranCarte.kt:308`) |
| **Guidage, arrivée à moins de 30 m** | ouverture automatique : `copy(controleId = placetteId, arriveeVue = true)`, `placetteId` gardé | `retour()` → le **guidage** (`placetteId` encore posé) |
| **Guidage, bouton « Contrôler »** (affiché une fois arrivé, pour rouvrir à la main) | idem, sans condition | idem |

`retour()` gagne **sa première ligne** : `controleId != null -> copy(controleId = null)`. Les vues internes de l'écran
(Plant, Pointer, Terminer) sont un état d'écran, pas de navigation : le geste « retour » d'Android depuis Pointer ou
Terminer revient à Plant (un `BackHandler` de l'écran, prioritaire sur celui de `MainActivity.kt:80`), puis, depuis
Plant, quitte l'écran. Aucune confirmation à la sortie : tout est déjà écrit au journal, la session se relit à
chaque appel.

**Pas de réouverture en boucle.** Au retour au guidage, `GuidagePret` est recomposé avec un `DetecteurArrivee` neuf
(`EcranGuidage.kt:139`), dont `entree()` rendrait vrai tout de suite à moins de 30 m : c'est la boucle. Remède : un
drapeau `arriveeVue` **dans `EtatNavigation`**, sauvegardé par `enListe()` (survit à la rotation) : posé à l'ouverture
automatique ; l'ouverture automatique et le bip d'arrivée sont inhibés tant qu'il est vrai ; le guidage le baisse
quand la distance atteint `Guidage.SEUIL_REARMEMENT_M` (40 m), la constante qui existe déjà (`Guidage.kt:27`). Aucune
modification de `:core`. Un test pur (`NavigationTest`) joue : arrivée, ouverture, retour, pas de réouverture, sortie
à 41 m, nouvelle arrivée, nouvelle ouverture ; et la zone d'oscillation 29–39 m, qui ne rouvre pas.

**Récepteur GNSS.** `recepteurDemande` vaut déjà `tourneeId != null && recepteurOuvert` (`Navigation.kt:38`) ;
`controler()` pose `recepteurOuvert = true` comme `guider()`. Le contrôle ouvert garde donc le récepteur, comme la carte
et le guidage ; on revient aux « Placettes » en gardant le récepteur (comportement de la fiche 047, inchangé) et à
l'accueil en le levant. Un test l'affirme pour les deux portes. La sérialisation `enListe()`/`depuisListe()` gagne
deux champs en fin de liste (`controleId`, `arriveeVue`) : une liste de six éléments (ancien état sauvegardé) reste
lisible, avec les valeurs par défaut.

**Arrivée dans l'écran (rattachement).** `SessionControle.ouvrir` veut une `Arrivee` (position moyennée) et un
éventuel `codeLu`. L'écran du contrôle commence donc par une **étape d'arrivée**, partagée avec le guidage et non
recopiée : j'extrais d'`EcranGuidage.kt:127-162` l'état `Confirmation` et la boucle de moyenne de 5 s (`MoyennePosition`)
dans un état réutilisable (`Rattachement` côté `app/carte/`), appelé par les deux écrans. L'étape : « moyenne de la
position, 3 / 5 » puis `ouvrir(...)`. Succès : la vue Plant. Refus (`hors_15_m`, `code_inconnu`, `hors_france`,
`point_hors_foret`) : le refus avec son code, un champ « code du piquet » et « Recommencer ». Hors de France
(**placette en Somalie**) : `hors_france`, aucune session, aucun pointage, aucune ligne au journal (test). Sans fix : « pas
de position », et le champ du code reste utilisable (`Arrivee` exige une position : voir § 7, c'est une limite).

**La session survit à la rotation** (elle garde `courant` et `regarniEnAttente` en mémoire seulement, `SessionControle.kt:202-203`) :
je la mets dans un `ViewModel` par (tournée, placette) ; l'ajout de `lifecycle-viewmodel-compose` est dans le catalogue
(`libs.versions.toml:54`) mais pas encore dans `app/build.gradle.kts`. À défaut, un objet tenu par l'activité,
comme `SourcePosition`. Ses méthodes sont bloquantes (journal) : appelées sur `Dispatchers.IO`, résultat poussé dans un
`StateFlow<EtatEcran>`. Après la mort du processus, l'arrivée est refaite (moyenne de 5 s) : le journal a tout, rien
n'est perdu, sauf un « remplace » encore en attente.

**Permission de stockage** (décision 8) : à la première ouverture de l'écran, si l'API est 28 ou moins et la permission
non accordée, `WRITE_EXTERNAL_STORAGE` est demandée (déclarée avec `maxSdkVersion="28"`, `AndroidManifest.xml:19`) ;
refus : une ligne en petit, « la sauvegarde hors de l'appareil attend cette permission », qui ne bloque aucune saisie
(`SauvegardeApp.kt:23-26` dit déjà que l'écriture échoue sans effet sur la session). À l'API 29 et plus : rien.

## 4. La boussole du téléphone

**Capteur.** `Sensor.TYPE_ROTATION_VECTOR` (fusion accéléromètre, magnétomètre, gyroscope), puis
`SensorManager.getRotationMatrixFromVector` et `getOrientation` : `orientation[0]` est l'azimut du **bord haut** du
téléphone par rapport au **nord magnétique** (documentation Android, `SensorManager.getOrientation`). Groma lit le même
capteur (`Position.kt:85, 94-95`) et lisse sur le cercle unité (`:96-104`, coefficient 0,15). Repli si le capteur manque :
accéléromètre + champ magnétique (`getRotationMatrix`) ; ni l'un ni l'autre : le pointage à la boussole est
indisponible, l'écran le dit et propose le clavier (Q2).

**Magnétique contre géographique, et la déclinaison une seule fois.** **Piège repéré** : Groma, ligne 98, **ajoute**
`GeomagneticField(...).declination` au cap : sa boussole rend un cap **géographique**. La reprendre telle quelle ferait
ajouter la déclinaison deux fois (une fois par Android, une fois par `ChaineAzimut.orienter`, `Reperes.kt:104-114`,
avec WMM2025 embarqué), avec deux modèles différents. Quatre garde-fous :

1. La classe de Surculus (`BoussoleTelephone`) **ne connaît pas la position** et n'importe pas `GeomagneticField` ; elle
   rend un `CapMagnetique` (type dédié, valeur en degrés [0, 360[), jamais un `Double` nu.
2. Le seul chemin vers la session : `AzimutSaisi.valider(cap.degres, UniteAzimut.DEGRES)` puis
   `session.pointer(distance, azimut, SourceAzimut.BOUSSOLE_TELEPHONE)`. La session ajoute la déclinaison **une fois**
   (`PointDerive.deriver`, `SessionControle.kt:238`) et l'écrit (`declinaison_deg`, `azimut_geo_deg`).
3. **Test de source** (même genre que `SansPrechargementTest`, `EcranPlacettesSourcesTest`) : aucun fichier de `app/src/main`
   ne cite `GeomagneticField`.
4. **Test de comportement** : une lecture simulée de 100,0° magnétique, à Couchey le 2026-10-27, écrit au journal
   `azimut_mag = 100.0`, `unite_azimut = "deg"`, `source_azimut = "boussole_telephone"`, et
   `azimut_geo_deg = (100,0 + declinaison_deg) mod 360` à 1e-9 près : l'écart avec `azimut_mag` est exactement la
   déclinaison, pas le double. Vu rouge : en ajoutant la déclinaison dans la boussole, ce test échoue.

À l'écran, le cap de la vue Pointer porte l'étiquette « (magnétique) » ; le guidage montre un cap **géographique**
(`Guidage.kt:19-21`) : l'étiquette évite que l'agent compare les deux. L'unité du téléphone est le degré : `unite_azimut`
vaut `deg` pour un cap pris au capteur, même si la tournée déclare des grades (la colonne décrit la valeur écrite).

**Précision.** Trois signaux : `onAccuracyChanged` (`SENSOR_STATUS_ACCURACY_HIGH/MEDIUM/LOW/UNRELIABLE`) ; pour le
vecteur de rotation, l'incertitude de cap `values[4]` en radians (−1 si absente) ; l'inclinaison de l'appareil, tirée de la
matrice. L'écran montre « 143° ± 8° » quand `values[4]` existe, sinon seulement le niveau. Classement testé en Kotlin
pur (`ClasseBoussole.depuis(statut, incertitudeRad, inclinaisonDeg)`) : **bonne** (HIGH ou MEDIUM, inclinaison faible),
**douteuse** (LOW, ou téléphone trop incliné : « tenez-le à plat »), **inutilisable** (UNRELIABLE).
Les seuils d'inclinaison et d'incertitude sont à fixer à l'essai en forêt, pas devinés ici.

**Étalonnage.** Android n'a pas d'appel public pour lancer l'étalonnage : l'écran affiche, quand la classe est douteuse
ou inutilisable, « Boussole peu fiable. Dessinez un 8 avec le téléphone. » avec un petit pictogramme, et se tait dès que la
précision remonte. Le bouton « Pointer » reste actif (on n'enferme pas le forestier) mais le cap passe à
`colorScheme.error` et le texte dit « cap douteux » (Q3).

**Perturbation magnétique en forêt** (canne métallique, récepteur RTK, coque aimantée, véhicule). Proposition : mesurer
la norme du champ (`TYPE_MAGNETIC_FIELD`) et afficher « champ perturbé » quand elle sort d'une plage autour du champ
terrestre de la région (de l'ordre de 45 µT en France ; bornes à fixer à l'essai) (Q4). Le champ de la 012-B ne corrige
rien : il avertit.

**Geste de visée et lecture.** Téléphone à plat, bord haut vers le plant (sans `remapCoordinateSystem`, qui n'est utile
que téléphone vertical). L'appui sur « Pointer » prend la **moyenne circulaire des lectures de la dernière seconde** (le
même lissage que Groma, appliqué sur une fenêtre fixe) pour ne pas enregistrer un tremblement ; la confirmation dit
« Plant n° 7 pointé : 143°, 6,2 m ». Une erreur se corrige par « Annuler » (`SessionControle.annuler`, qui défait le
pointage). Rotation de l'écran : indifférente, car le capteur est lié au bord haut du téléphone dans son orientation
naturelle.

**Clavier en secours (question de l'orchestrateur)** : voir Q2. S'il est retenu : un champ « Cap (°) » dans la vue
Pointer, visible sous un petit lien « Saisir au clavier », `SourceAzimut.SAISIE`, unité = celle de la tournée.
**Ce qui manque** : l'unité d'azimut déclarée par la tournée n'est lue nulle part dans l'application (`UnitesTournee`
n'a aucun usage dans `app/`, `gpkg/` ni `data/`) et n'est pas dans `ContexteControle` ; il faut décider d'où elle vient
(§ 7).

## 5. Amendements proposés (texte exact, à valider par Pascal ; rien n'est écrit dans `docs/`)

### 5.1 Spec § 1, lignes 44-45 (« Matériel de pointage à nommer par Pascal »)

Remplacer :

> * Pointage d'un plant sans RTK fixe : distance et azimut depuis le piquet. **Matériel de
>   pointage à nommer par Pascal** (boussole de visée, télémètre, ruban).

par :

> * Pointage d'un plant sans RTK fixe : **distance horizontale tapée** (ruban ou télémètre, au choix de l'agent) et
>   **azimut lu par la boussole du téléphone** (capteur magnétique, nord magnétique), depuis le piquet ; décision de
>   Pascal du 2026-10-27. L'écran montre la précision du capteur, demande l'étalonnage quand elle est faible et prévient
>   quand le champ magnétique est perturbé. [*Si Pascal retient Q2 :* Un azimut tapé au clavier, dans l'unité de la
>   tournée, reste possible en secours.] La source de l'azimut (`boussole_telephone`, [`saisie`]) est enregistrée dans
>   chaque pointage. **À confirmer par un essai en forêt contre une boussole de visée** (§ 10, question 1).

Et § 3, ligne 75 : « Azimut saisi | lu à la boussole, donc **magnétique** ; unité de la boussole **déclarée** (degrés ou
grades), convertie en degrés » devient « Azimut saisi | lu à la boussole du téléphone (degrés) [ou tapé, dans l'unité
déclarée de la tournée], donc **magnétique** ; converti en degrés ; la déclinaison est ajoutée **une seule fois**, par
la session ». § 10, question 1 : ajouter « *Tranchée le 2026-10-27 : boussole du téléphone ; essai en forêt à faire.* »

### 5.2 ADR 0003

Table « Règles », ligne **Plant** : remplacer « azimut magnétique brut, unité de la boussole (degrés ou grades) » par
« azimut magnétique brut (boussole du téléphone en degrés, ou saisi dans l'unité de la tournée), sa source, ».
Ligne **Déclinaison** : ajouter « ; ajoutée **une seule fois**, par la session de contrôle ; le capteur du téléphone
fournit un cap magnétique, jamais `GeomagneticField` d'Android (deux modèles ne se cumulent pas) ».
Ligne **Perturbation magnétique** : remplacer « visée inverse proposée ; compas tenu loin de la canne et du téléphone »
(le compas **est** le téléphone) par « visée inverse proposée ; téléphone tenu à plat, loin de la canne, du récepteur
RTK et de toute masse métallique ; précision du capteur et champ magnétique affichés ; étalonnage demandé quand le
capteur se dit peu fiable ».
Crible FRANCE : **F** « boussole et ruban pour les plants » devient « boussole du téléphone et ruban pour les plants » ;
**A** ajouter « boussole du téléphone perturbée par le métal : précision et champ affichés, clavier [si Q2] » ;
**C** : aucun matériel de plus pour le pointage. Un paragraphe daté « 2026-10-27, amendement : boussole du téléphone
au lieu d'une boussole de visée, sous réserve de l'essai en forêt ».

## 6. Étapes et tests

Chaque étape est un commit livrable, tests verts (`.claude/test-command`), **vu rouge au moins une fois** (la
déclinaison, § 4). Aucun `fontSize` (styles de Marculus), aucun code inventé (états, causes : `EtatDsf`, `GroupeCause`
de la session ; libellés d'état de `etats-plants.yaml`, libellés de cause de `causes_dsf_groupes_bilan` ; refus : les
`code` de la session).

1. **Navigation** : `controleId`, `arriveeVue`, `controler()`, `retour()`, sérialisation ; boutons « Contrôler » (carte,
   guidage) ; ouverture automatique à 30 m. Tests `NavigationTest` : deux portes, retours (carte et guidage),
   rotation (`enListe` / `depuisListe`, y compris l'ancienne liste de six éléments), `recepteurDemande` vrai par les
   deux portes, pas de réouverture en boucle (29-39 m), réarmement à 40 m.
2. **Arrivée partagée** : extraction de la moyenne et du rattachement de `EcranGuidage` ; `SessionControle.ouvrir`
   depuis l'écran ; refus avec code ; permission de stockage. Tests : **Somalie** (fix hors de France : `hors_france`,
   aucune ligne écrite), refus `hors_15_m` / `code_inconnu` / `point_hors_foret` montrés avec leur code, session ouverte
   par l'identité du piquet.
3. **État de l'écran en Kotlin pur** (`EtatEcranControle`, fonction `EtatSession → texte et zones`) et vue Plant : plant
   courant, « 7 sur 12 », état précédent, alerte « déjà terminée », état et cause (quatre groupes), « Terminer » sans état
   refusé (`RienASaisir` dit en clair), « Annuler ». Tests : table de cas ; **un test par refus** (une boucle sur la liste
   de tous les `RefusPlacette` et `RefusRepere` : chaque refus produit un texte qui contient son `code`), état précédent à
   côté de l'état du jour, saisie après « Terminer » (alerte, placette toujours « Contrôlée »).
4. **Boussole** (`BoussoleTelephone`, `CapMagnetique`, `ClasseBoussole`, lissage circulaire sur une fenêtre) : tests
   purs du lissage (359° et 1° donnent 0°), de la classe de précision, de la déclinaison **non doublée**, du test de
   source sur `GeomagneticField`.
5. **Vue Pointer, plan, regarni** : pointage (distance tapée, cap du téléphone), alerte hors rayon (`AConfirmer`, pas un
   refus), « remplace » puis pointage du regarni, plan à l'échelle (dessin pur : conversion distance et azimut
   géographique vers pixels, testée). Tests : hors rayon confirmé ou refait, regarni écrit avec `remplace_uuid`, plan
   (un plant à 2 m et 90° est à l'est du piquet).
6. **Terminer et « Contrôlée »** : vue Terminer avec les totaux ; la liste « Placettes » relit
   `DepotTournees.placettesControlees` (déjà branché, `DepotTournees.kt:84`). Contrôle de bout en bout sur Couchey, la
   fixture existante.
7. **Contraste et cibles** : test ≥ 4,5:1 des textes de l'écran en clair et en sombre (sur le modèle de
   `ContrasteTest`), pastilles ≥ 3:1, cibles ≥ 48 dp (la constante du composant testée), aucune plaque blanche.
8. **Capture** après la fusion (règle du 2026-10-27) : release installée, écran ouvert depuis la carte, une placette de
   Couchey terminée, puis « Contrôlée » dans « Placettes » ; test des dix secondes par Pascal.

Le **vu rouge** : étape 4 (casser le code en ajoutant la déclinaison dans la boussole, le test échoue).
Le test des plants hors de France reste dans l'étape 2.

## 7. Ce qui manque à la session (je ne la modifie pas, je le dis)

1. **Unité d'azimut de la tournée** : absente de `ContexteControle`, lue nulle part. Nécessaire pour le clavier (Q2).
2. **Reprise sans position** : `ouvrir` exige une `Arrivee` ; après la mort du processus, ou sans fix, il faut refaire
   l'arrivée. Une fabrique `reprendre(...)` sur un contrôle déjà écrit le jour même éviterait la moyenne de 5 s. Et
   `regarniEnAttente` n'existe qu'en mémoire (perdu à la mort du processus).
3. **Plants d'un passage précédent sans position** : `PlantAnterieur` n'a ni distance ni azimut (`SessionControle.kt:68-75`),
   donc `PlantSession.position` est `null` : ils ne peuvent pas être dessinés sur le plan (liste et « suivant »
   seulement). Question Q8.
4. **Précision de la boussole** : `ValeurPointage` n'a pas de clé pour elle ; on la montre mais on ne la garde pas (Q6).
5. **Date de la dernière sauvegarde** : `EcouteSauvegarde` ne la rend pas ; la fiche d'affichage la voudrait en petit.
6. **Essences** : le chargeur d'`Essences` n'existe pas dans `app/` ; la 012-B passe `Essences(emptySet())` et
   `essence = null` (la fiche ne demande pas d'essence). Hors périmètre, dit ici.
7. **Numéros après « Terminer »** : un plant pointé après la fin reçoit « le numéro suivant » mais la ligne `controle`
   figée ne le porte pas ; l'écran affiche donc le numéro calculé par la session, sans le réécrire. Je le signale pour
   la mission d'export, sans le toucher.
8. **Sans fix** : `Arrivee` n'accepte pas « pas de position + code lu seul ». `codeLu` existe, mais la position mesurée
   est requise. Question Q9.

Ajouts à `:core` pour la 012-B : **aucun prévu** (le drapeau d'arrivée est dans `app/`). Si l'étape 4 montre qu'un type
`CapMagnetique` mérite d'être en `:core`, je l'annoncerai dans la PR.

## 8. Taille estimée et découpage

Estimation : navigation et arrivée ~250 lignes de code ; vue Plant et état ~350 ; vue Pointer, plan, boussole ~450 ;
vue Terminer et permission ~100 ; tests ~700. **Environ 1 150 lignes de code et 700 de tests**, plus que ce qu'un
relecteur et Pascal prennent d'un bloc (le plan 012 le disait pour 2 000 lignes).

**Proposition : deux PR, deux phrases.**
* **012-B1** : « ouvrir l'écran du contrôle par les deux portes, l'arrivée et le rattachement, dire les états et
  causes des plants du passage précédent, terminer la placette et la voir « Contrôlée » ». Étapes 1, 2, 3, 6, 7 (sans
  la boussole). Utile seul sur les passages suivants (plants déjà là), et il fait passer la voie « Contrôlée ».
* **012-B2** : « pointer un plant (azimut à la boussole du téléphone, distance tapée), le placer sur le plan, poser les
  regarnis ». Étapes 4, 5, et l'amendement de la spec et de l'ADR. C'est la partie qui dépend de l'essai en forêt.

Le test des dix secondes se fait deux fois (sur B1 : « plant 7, sain, 7 sur 12 » ; sur B2 : « le cap et la distance »).

## 9. Test des dix secondes (à faire par Pascal, sur la release après la fusion)

* **Qui** : Pascal, ou quelqu'un qui n'a jamais vu l'écran, plein soleil, un gant, téléphone à bout de bras.
* **Ce qu'il doit avoir compris après dix secondes** : le **numéro** du plant dont on parle, son **état** (ou « pas
  encore dit »), et **combien restent** (« 7 sur 12 »).
* **Question de contrôle** : comprend-on la situation en dix secondes ?

## 10. Questions pour Pascal (je n'en tranche aucune)

* **Q1. Une PR ou deux ?** Proposition : deux (B1 puis B2, § 8), parce que 1 150 lignes ne se relisent pas d'un bloc et
  que B2 attend un essai en forêt.
* **Q2. Un clavier en secours pour l'azimut ?** (une boussole de téléphone se trompe près du métal) Proposition de
  l'orchestrateur et la mienne : **oui**, un lien « Saisir au clavier » dans la vue Pointer, `SourceAzimut.SAISIE`, unité
  de la tournée. Prérequis : décider d'où vient l'unité (§ 7.1 : la lire dans `projet` ou la demander à l'ouverture de
  la tournée, comme l'ADR 0003 le prévoit).
* **Q3. Précision faible : avertir sans bloquer ?** Proposition : oui, « Pointer » reste actif, le cap passe en couleur
  d'erreur avec « cap douteux ».
* **Q4. Mesurer le champ magnétique et afficher « champ perturbé » ?** Proposition : oui, bornes fixées à l'essai ; avertit
  sans corriger.
* **Q5. Geste de visée** : téléphone à plat, bord haut vers le plant, cap pris à l'appui sur « Pointer » (moyenne de
  la dernière seconde). Proposition : celui-ci ; l'autre (téléphone vertical, visée par la caméra) demande une
  recomposition des axes et un autre mode d'emploi.
* **Q6. Garder la précision de la boussole dans le pointage ?** Elle changerait le format de `valeur` (clé de plus,
  `docs/formats/journal-valeurs-v1.md`, 012-A, export). Proposition : **non pour la 012-B** (on la montre seulement) ;
  à reprendre après l'essai en forêt.
* **Q7. Couleurs et formes des quatre états.** Proposition : sain vert, atteint orange, mort rouge, absent noir (la
  palette de quatre couleurs existante), chacun avec sa forme ; en sombre, pastille cernée de blanc (063).
* **Q8. Plants d'un passage précédent sur le plan.** Proposition : le plan ne montre que les plants pointés ; les plants
  anciens se choisissent par « suivant » ou par leur numéro, et le plan le dit en petit (« plants du passage précédent
  non dessinés »). Les dessiner demande une extension de la session (position dans `PlantAnterieur`), donc une mission à
  part.
* **Q9. Sans position GNSS, rattacher par le code du piquet seul ?** Aujourd'hui `ouvrir` exige une position, même avec
  un `codeLu`. Proposition : **non en 012-B** (l'écran dit « pas de position »), une mini-mission A si le terrain le
  réclame.
* **Q10. Ouverture automatique : la moyenne de 5 s part-elle toute seule, ou au bouton ?** Proposition : toute seule à
  l'ouverture (on est arrivé, les mains sont prises) ; un bouton « Recommencer » reste.
* **Q11. « vide » : bouton absent ou message ?** Proposition : absent (§ 2).
* **Q12. Sortie du contrôle sans confirmation**, tout étant déjà au journal. Proposition : oui, sans confirmation.
