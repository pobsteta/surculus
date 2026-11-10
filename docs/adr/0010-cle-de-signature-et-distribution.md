# ADR 0010 — Clé de signature et distribution

- **Date :** 2026-11-10
- **Statut :** **proposée**. Critiquée par le `contradicteur` le 2026-11-10 ; Pascal a tranché les huit
  points de la critique le même jour (voir « Révisions »). **Pascal l'acceptera une fois faites ses
  actions** (rubrique « Avant l'acceptation »).
- **Proposée par :** `archiviste` (Claude), à partir des décisions de Pascal du journal
- **Tranchée par :** Pascal

## La décision en une phrase

La clé qui signe les releases de Surculus existe en **deux copies lisibles** (le poste de Pascal et
une clé USB rangée hors du bâtiment du poste), **Pascal seul détient les mots de passe**, les quatre
secrets de signature vivent dans un **environnement GitHub « release »** limité à `main` et soumis à
l'approbation de Pascal, et les releases sont distribuées en APK signé sur les releases GitHub.

Cet ADR dit **où** sont les copies et **quoi faire** si la clé est perdue. Il ne contient ni mot de
passe, ni élément de la clé.

## Les décisions de Pascal (2026-11-10)

1. **Environnement « release ».** Les quatre secrets passent dans un environnement GitHub « release »,
   limité à `main`, avec Pascal comme approbateur obligatoire. Pascal déplace les secrets. Une mission
   de code adaptera `release.yml` (ajouter `environment: release` aux jobs qui lisent les secrets) :
   **elle reste à faire, elle n'est pas écrite**. Cet ADR ne touche pas à `release.yml`.
2. **Mots de passe.** Pascal vérifie s'ils sont passés par une session de Claude, et les change avant
   l'acceptation de cet ADR si c'est le cas.
3. **Clé perdue.** On publie sous un **nouvel identifiant d'application**, installé à côté de
   l'ancien (marche à suivre plus bas).
4. **Vérification des développeurs Android.** **Compte limité dès maintenant** (gratuit, nombre
   d'appareils plafonné, voir plus bas). On enregistre `io.github.pobsteta.surculus` tant que la clé
   est là ; on passe au compte complet si l'on dépasse le plafond d'appareils.
5. **Clé USB** rangée « hors du bâtiment du poste », sans adresse écrite.
6. **Test du successeur : « non »**, assumé par la décision de Pascal (mots de passe à lui seul) ;
   inscrit dans les valeurs sacrifiées, rouvert à chaque bilan.
7. **Copie USB essayée et relue.** Pascal lance `keytool -list -v` sur la copie USB ; l'empreinte
   SHA-256 complète du certificat (publique) va dans cet ADR et dans le `README.md`, avec le vrai
   format du fichier. La copie est relue à chaque bilan.
8. **Distribution.** Examen en une ligne de F-Droid, d'Obtainium, du registre des applications libres
   et de la mise à jour sans réseau (plus bas). Rotation de clé et APK local : constats sourcés,
   plus de question.

## Contexte

La clé a été créée le 2026-10-16, avant cet ADR (journal, l. 65) : RSA 4096, alias `surculus`,
valable jusqu'en 2054, gardée hors du dépôt (`*.keystore` et `*.jks` sont ignorés, `.gitignore`
l. 10-11). Le journal du 2026-10-16 demandait déjà à Pascal de la sauvegarder hors du poste ; les
bilans du 2026-10-21 et du 2026-11-09 ont relevé que l'ADR 0010 manquait.

### Ce que coûte la perte de la clé

Android n'installe une mise à jour que si elle porte la même signature que la version installée.
Perdre la clé (poste **et** clé USB) a ces effets :

- **Rien n'est désinstallé.** Les applications installées continuent de fonctionner telles quelles.
- **Plus aucune mise à jour** ne s'installe par-dessus l'ancienne version, et une clé neuve n'y change
  rien : il faut une nouvelle application (marche « clé perdue »).
- **Ce qui est perdu**, si le forestier désinstalle sans précaution :
  - les saisies postérieures au dernier `.sursync` (au plus 20 saisies ou 5 minutes, ADR 0005
    l. 50) ;
  - les lignes sans tournée (`code_tournee` vide), absentes de toute sauvegarde (ADR 0005 l. 46) ;
  - les réglages (dossier SD ou USB, binôme, WebDAV).
- **Ce qui survit** : le `.sursync` et les photos du stockage partagé survivent à la désinstallation
  (ADR 0005 l. 37, 50 et 54). Android attribue chaque fichier du stockage partagé à **l'installation**
  qui l'a créé, pas à la signature : après réinstallation, l'application doit redemander l'accès aux
  fichiers que « l'ancienne installation » a créés (https://developer.android.com/training/data-storage/shared/media,
  § « App attribution of media files », lu le 2026-11-10). La restauration passe par le sélecteur
  de fichier (ADR 0005 l. 54), qui ne dépend pas de cette attribution ; **elle n'a pas été essayée**
  après réinstallation (voir « Dette »).

`docs/publier.md` l. 27-34 a déjà tranché dans l'autre sens : pas d'APK sans clé, le repli sur la clé de
debug est supprimé pour les releases (décision du 2026-10-16), parce que la clé de debug de chaque
runner change à chaque exécution. Le repli debug **en local** (`assembleRelease`, pour les essais) y est
aussi dit : un APK local ne s'installe pas par-dessus une release. C'est décidé ; rien à rouvrir.

### Le secret GitHub n'est pas une sauvegarde, et il se lit par un workflow

GitHub ne montre plus la valeur d'un secret une fois posé, ni dans l'interface ni par l'API : on ne
peut pas en reprendre la clé (`RELEASE_KEYSTORE_BASE64`, `docs/publier.md` l. 69). Ce n'est donc pas
une sauvegarde. **Mais « illisible » ne veut pas dire « inaccessible »** : tout workflow qui reçoit le
secret peut le lire, et quiconque a l'accès en écriture au dépôt en pose un. GitHub le dit en toutes
lettres : « Any user with write access to your repository has read access to all secrets configured in
your repository »
(https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions,
section « Use secrets for sensitive information » ; l'intitulé « Access to secrets » cité par la
critique n'existe plus sous ce nom sur la page lue le 2026-11-10). `release.yml` lit les quatre secrets
aux l. 84-87, 102-104 et 130-132, **sans `environment:`** : aujourd'hui, un workflow poussé sur une
branche par quiconque a le droit d'écriture, ou un jeton qui porte le droit `workflow`, pourrait les
lire. **Les droits `workflow` des jetons des agents ne sont pas vérifiés** (non vérifié).

C'est ce que la décision 1 corrige : un secret d'environnement n'est remis qu'aux jobs qui désignent cet
environnement, **après** le respect de ses règles (approbateur, branche autorisée)
(https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-deployments/managing-environments-for-deployment,
lue le 2026-11-10 ; les environnements sont disponibles pour un dépôt public, ce qu'est
`pobsteta/surculus`, licence GPL-3.0). Tant que la mission de code n'est pas faite, **cette parade n'est
pas en place**.

### La vérification des développeurs Android

Pages lues le 2026-11-10.

- **Calendrier** (https://developer.android.com/developer-verification, § Timeline) : depuis le
  30 septembre 2026, protections dans quelques pays (Brésil, Indonésie, Singapour, Thaïlande) pour les
  installations par certains magasins ; « Verification capability will be expanded globally for all apps
  on certified Android devices in 2027 ». La France n'est pas concernée aujourd'hui, elle le sera.
- **Sans enregistrement** : « Unregistered apps can only be installed or updated when the advanced flow
  is enabled or by using ADB so if the advanced flow is disabled updates to unregistered apps will fail »
  (FAQ : https://developer.android.com/developer-verification/guides/faq). Le « mode avancé » est un
  réglage unique que l'utilisateur active ; les mises à jour d'une application non enregistrée
  échouent hors de ce mode.
- **Clé perdue** (même FAQ) : « If you lose your signing key you won't be able to register your
  packages. » La FAQ ajoute qu'une application peut avoir plusieurs clés enregistrées pour un même nom
  de paquet.
- **Compte limité** (https://developer.android.com/developer-verification/guides/limited-distribution,
  mis à jour le 2026-10-28) : gratuit, sans pièce d'identité, partage avec des appareils que
  l'utilisateur a **autorisés** (poignée de main par code QR ou lien, consentement sur l'appareil), pour
  « students, teachers, and hobbyists ». Compte complet : 25 USD (FAQ). Le passage du limité au complet
  est possible, pas l'inverse (FAQ).
- **Écart entre les pages** : la page d'accueil et la FAQ disent **20 appareils**, le guide du compte
  limité dit **50 appareils**. On retient **20** (le plus bas), jusqu'à ce que Pascal vérifie dans la
  console. Voir « Questions ouvertes ».
- **ADB** : l'installation par ADB n'exige pas la vérification (FAQ).

Conséquence : **la clé n'est pas seulement ce qui permet la mise à jour ; elle est ce qui permet
l'enregistrement.** Enregistrer `io.github.pobsteta.surculus` tant que la clé existe, c'est la seule
façon de garder, après 2027, des mises à jour sans « mode avancé ». C'est la décision 4.

### Options envisagées

| | Option | Remarque |
|---|---|---|
| a | Poste seul + secret GitHub | Situation du 2026-11-09 : une seule copie lisible. Écartée. |
| b | Poste + une clé USB rangée hors du bâtiment du poste | **Retenue par Pascal** (journal, 2026-11-10). |
| c | Poste + gestionnaire de mots de passe (pièce jointe chiffrée) | Évoquée au journal du 2026-11-10 ; non retenue par Pascal. |
| d | Deux détenteurs de mots de passe, ou dépôt sous séquestre | Rendrait un successeur possible. **Non retenue** : décision 6, assumée et rouverte à chaque bilan. |
| e | Signature par un service tiers (Play App Signing, par exemple) | Suppose de publier sur un magasin ; hors tranche 1 et hors de ce qui est décidé. |

## Où se trouvent les copies

| Copie | Emplacement | Qui y accède | Date de la mention |
|---|---|---|---|
| 1, de travail | Poste de Pascal, `~/.android-keys/` (droits 600), journal 2026-10-16 | Pascal | 2026-10-16 |
| 2, de secours | Clé USB, **hors du bâtiment du poste** ; l'adresse n'est pas écrite ici (décision 5) | Pascal | 2026-11-10 |
| 3, de signature seule | Secret GitHub `RELEASE_KEYSTORE_BASE64` (à déplacer dans l'environnement « release », décision 1), **illisible par l'interface et l'API, lisible par un workflow qui le reçoit** | Le workflow `release.yml` | 2026-10-16 |

Seules les copies 1 et 2 comptent comme sauvegarde. Les mots de passe (`RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_PASSWORD`) et l'alias (`RELEASE_KEY_ALIAS`) sont détenus par **Pascal seul**.

### Le fichier et son empreinte (à remplir par Pascal)

L'empreinte SHA-256 du journal (l. 65) est tronquée (« 8F:27:7A:71:…:AE:82 ») : elle ne permet pas de
vérifier une clé. Pascal lance `keytool -list -v` sur la **copie USB** (pas sur celle du poste) ; le
résultat remplit le tableau. L'empreinte d'un certificat est publique : elle peut figurer ici.

| Champ | Valeur |
|---|---|
| Nom du fichier | `surculus-release.keystore` (`docs/publier.md` l. 65) ; **à confirmer par Pascal sur la copie USB** |
| Format du fichier (ligne « Keystore type » de `keytool`) | **à remplir par Pascal** |
| Alias | `surculus` (journal l. 65) ; **à confirmer par `keytool`** |
| Empreinte SHA-256 complète du certificat | **à remplir par Pascal** |
| Date de l'essai (copie USB lue par `keytool`, empreinte égale à celle de la copie du poste) | **à remplir par Pascal** |

Cet ADR n'affirme pas le format. Depuis le JDK 9, `keytool` crée par défaut des fichiers PKCS12
(JEP 229, https://openjdk.org/jeps/229) : **probable, non vérifié** pour cette clé. L'empreinte sera
aussi reportée dans le `README.md` (rubrique « Signature »).

**Relecture.** Cette copie (lisible, empreinte égale) est relue **à chaque bilan** : c'est la seule
façon de savoir qu'elle sert avant d'en avoir besoin.

## Marches à suivre

### Reposer les secrets depuis la copie USB

À faire par Pascal, quand l'environnement « release » existe (ou si un secret est perdu). Le dépôt est
`pobsteta/surculus`, l'environnement `release`.

1. Brancher la clé USB ; ne rien copier dans le dépôt (`*.keystore` est ignoré).
2. `base64 -w0 <fichier-keystore> | gh secret set RELEASE_KEYSTORE_BASE64 --env release -R pobsteta/surculus`
3. Pour chacun de `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` :
   `gh secret set <NOM> --env release -R pobsteta/surculus`, puis saisir la valeur à l'invite
   (`gh` la lit sur l'entrée standard ; ne pas la mettre en argument, elle resterait dans l'historique
   du shell).
4. Vérifier que les quatre noms apparaissent (`gh secret list --env release -R pobsteta/surculus`),
   puis qu'une release signe : `apksigner verify --print-certs` sur l'APK, empreinte égale à celle du
   tableau ci-dessus.

Les options `--env` et `-R` sont celles de `gh secret set --help` (essayé le 2026-11-10) ; la pose
elle-même n'a **pas** été essayée ici.

### Clé perdue (poste et clé USB)

Décision 3. Perdre la clé ne casse aucun téléphone ; cela interdit toute mise à jour de l'application
installée. On publie une **autre application**, installée à côté.

**Pascal :**

1. Créer une nouvelle clé (`keytool -genkeypair`, `docs/publier.md` l. 65), en faire deux copies
   (poste, clé USB), noter son empreinte dans cet ADR (nouvelle ligne datée, sans effacer l'ancienne).
2. Choisir un **nouvel identifiant d'application** (par Pascal, à ce moment-là) et l'enregistrer dans
   la console des développeurs avec la nouvelle clé. Le changer dans le code est une mission
   (`app/build.gradle.kts` l. 67).
3. Poser les secrets (marche précédente) et publier. L'ancienne application ne recevra plus de mise à
   jour ; le dire dans les notes de la release.

**Le forestier, sur chaque téléphone** (il le fait lui-même ; il n'y a pas d'autre moyen) :

1. Finir sa tournée dans l'**ancienne** application et la rendre. Les lignes sans tournée n'étant dans
   aucune sauvegarde (ADR 0005 l. 46), c'est la tournée rendue qui les met à l'abri.
2. Sortir son `.sursync` (sauvegarde déclenchée à la fin de la placette ou du constat, ADR 0005 l. 50 ;
   vérifier le fichier dans `Documents/Surculus/`).
3. Installer la **nouvelle** application (les deux cohabitent : l'autorité du fournisseur de fichiers
   suit l'identifiant, `AndroidManifest.xml` l. 43, et le type `fumee` s'installe déjà à côté avec
   `applicationIdSuffix`, `app/build.gradle.kts` l. 125 ; **cohabitation complète non essayée**).
4. La restaurer depuis le `.sursync` par le sélecteur de fichier (ADR 0005 l. 54), puis **constater**
   que les saisies sont là.
5. **Alors seulement**, désinstaller l'ancienne.
6. Refaire les réglages (dossier SD ou USB, binôme, WebDAV). Les photos : l'attribution tient à
   l'installation, la nouvelle application ne les retrouve pas forcément (non vérifié).

## Distribution : ce que l'on retient, ce que l'on écarte

Distribution actuelle : à chaque fusion dans `main`, `release.yml` signe l'APK, le dépose sur la release
GitHub ; le forestier le télécharge et l'installe (`README.md` l. 42-45, Android 8.0 ou plus récent).

| Voie | Décision | Raison |
|---|---|---|
| **F-Droid** | **Écartée** pour l'instant | Garder notre signature suppose un build reproductible, avec copie de signature (https://f-droid.org/docs/Reproducible_Builds/, lue le 2026-11-10) ; la reproductibilité de Surculus n'est pas essayée, et F-Droid ajoute un tiers entre la fusion et le forestier. Sa signature par défaut en l'absence de build reproductible : non vérifié. |
| **Obtainium** | **Retenue comme option facultative**, sans engagement | Installe et met à jour depuis la page des releases GitHub (https://github.com/ImranR98/Obtainium, README, lu le 2026-11-10), donc avec **notre** APK et **notre** signature ; rien à changer côté Surculus. Licence et activité de la communauté : non vérifiées ici. Aucun essai sur les téléphones des forestiers. |
| **Registre des applications libres** de la vérification Android | **Écartée** pour l'instant | Il sert aux dépôts libres (F-Droid et assimilés) qui s'intègrent à la console ; ce guide dit que l'application n'a pas à y être pour continuer d'être distribuée ailleurs (https://developer.android.com/developer-verification/guides/open-source-app-registration, lu le 2026-11-10). Surculus n'est sur aucun dépôt libre. Ce qu'il lui faut : l'enregistrement de son nom de paquet et de sa clé (décision 4). Redevient pertinent si F-Droid est retenu. |
| **Mise à jour sans réseau** (APK passé par le binôme ou par câble) | **Retenue comme repli** | L'APK signé est un fichier : un binôme peut le transmettre, ou le câble avec ADB. Une application enregistrée s'installe comme aujourd'hui ; ADB n'exige pas la vérification (FAQ). Mode d'emploi à écrire dans `docs/publier.md` ; **non essayé**. |

La distribution **par GitHub, en APK signé, reste la voie principale** : elle ne demande aucun
service de plus.

## Rotation et expiration de la clé

La rotation est une fonction d'**Android**, pas d'un magasin : depuis Android 9, le schéma de signature
v3 permet à une mise à jour de changer de clé grâce à une preuve de rotation signée par l'ancienne clé
(https://source.android.com/docs/security/features/apksigning/v3, lu le 2026-11-10). Constats :

- elle **exige l'ancienne clé** : elle est **impossible après une perte**, le seul cas où l'on en aurait
  besoin ; d'où la marche « clé perdue » ;
- elle « n'est pas recommandée » sous Android 12 et avant (même page) ; Surculus vise Android 8.0 ou
  plus récent (`README.md`) : elle ne couvre pas tous les téléphones visés.

La clé court jusqu'en 2054 ; aucune rotation n'est prévue, et il n'y a plus de question à ce sujet.

## Marculus et Groma

L'ADR 0004 (l. 21) dit que Surculus reprend le code de Marculus et de Groma. Ont-ils leur propre clé, et
où est-elle ? **Non vérifié : rien dans ce dépôt ne le dit** (recherche de « clé », « keystore » et
« signature » dans les documents). **À demander à Pascal** (voir « Questions ouvertes »). Une clé commune
aux trois ferait d'une perte un sinistre triple ; des clés séparées en font trois copies à surveiller.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Un fichier, deux copies lisibles, un détenteur, un environnement GitHub | Une clé USB à ranger et à relire à chaque bilan ; une approbation de Pascal par release | **Oui** : décisions 1, 5 et 7 de Pascal |
| **R**esponsable | Qui fait quoi ? | Pascal garde la clé, les mots de passe et approuve chaque release ; la CI signe ; en cas de perte, **le forestier sort son `.sursync`, le restaure dans la nouvelle application, puis désinstalle l'ancienne** | Tout repose sur Pascal ; une manipulation chez chaque forestier en cas de perte | **Oui** : décisions 3 et 6 |
| **A**nticiper | Si l'imprévu arrive ? | Poste perdu : la clé USB reste. Les deux perdues : nouvelle application à côté, marche « clé perdue ». Pascal indisponible : personne ne publie ni ne récupère les mots de passe | Pour la perte : une réinstallation et une restauration par forestier, sans mise à jour d'ici là. Pour Pascal indisponible : plus de publication (valeur sacrifiée) | **Oui** : décisions 3 et 6, assumées |
| **N**ovateur | Toutes les idées écoutées ? | Options a à e, voies de distribution F-Droid, Obtainium, registre libre, mise à jour sans réseau | Le temps de l'examen (fait ici) ; Obtainium et le repli sans réseau restent à essayer | **Oui** |
| **C**oût | Coût pour l'ensemble ? | Pas d'argent aujourd'hui (compte limité gratuit) ; 25 USD si l'on passe au compte complet (au-delà de 20 appareils) ; une mission de code (`release.yml`) | 25 USD éventuels, une mission, le temps des relectures de la copie USB à chaque bilan | **Oui** : décision 4 |
| **É**cologique | Que réutilisons-nous ? | Outils standard (`keytool`, `apksigner`, `gh`), format du fichier à confirmer (voir tableau), aucun service propriétaire pour signer | Dépendance à GitHub Actions pour la signature automatique, et à la console Google pour l'enregistrement | **Oui** |

## Valeurs sacrifiées

- **La continuité si Pascal est indisponible.** Le **test du successeur est « non »** pour la clé : un
  seul détenteur de mots de passe. Sacrifice assumé par Pascal (décision 6), **rouvert à chaque
  bilan**.
- **La résistance à un sinistre commun.** La clé USB est rangée hors du bâtiment du poste, sans
  adresse écrite : un incendie ou un vol du bâtiment ne prend pas les deux copies. Un sinistre qui
  prendrait aussi l'autre lieu, non.
- **Le confort d'installation, payé par le forestier** (`CLAUDE.md`, « Plier ou tenir ») : pas de
  magasin, donc pas de mise à jour automatique ; le forestier télécharge et installe à la main ; en
  cas de perte de la clé, il refait lui-même la manipulation « clé perdue » ; avec la vérification des
  développeurs, il devra autoriser son téléphone auprès du compte limité (code QR ou lien, une fois).
  C'est un compromis que le forestier paie, pas Pascal.

## Dette

- **Test « restauration d'un `.sursync` après réinstallation »** : à écrire. Il couvre la
  réinstallation sous la même signature et sous un autre identifiant d'application (marche « clé
  perdue »), et vérifie le sort des photos. Pas de fiche de mission encore ; `CLAUDE.md` l'interdit sans
  tests, mais c'est une dette, pas une fonction à livrer. (ADR 0005 l. 54 dit « après une réinstallation,
  même voie » : aucun test cité.)
- **Mission de code `release.yml`** (décision 1) : `environment: release` sur les jobs qui lisent les
  secrets (l. 84-87, 102-104, 130-132). À écrire. Tant qu'elle n'est pas faite, la parade n'existe pas.
- **`docs/publier.md`** (l. 69-75) dit encore « secrets du dépôt » : à aligner à la même occasion.
- **Cohabitation de deux identifiants d'application** : à essayer sur un téléphone.

## Avant l'acceptation : les actions de Pascal

Cet ADR reste **proposée** jusqu'à ce que Pascal ait fait, et dit au journal :

1. **Mots de passe.** Vérifier s'ils sont passés par une session de Claude (la recherche de
   l'orchestrateur dans les sessions est expirée et non concluante, journal du 2026-11-10). **Si oui, les
   changer avant la fusion de cet ADR** (nouveau mot de passe du keystore et de la clé ; reposer les
   secrets avec la marche ci-dessus).
2. **Environnement « release ».** Le créer (limité à `main`, Pascal approbateur ; ne pas cocher
   l'interdiction d'approuver soi-même, Pascal étant à la fois celui qui fusionne et celui qui
   approuve), y déplacer les quatre secrets. Coût : chaque fusion dans `main` attend son approbation
   avant de signer.
3. **`keytool -list -v`** sur la copie USB : remplir le tableau « Le fichier et son empreinte », puis le
   `README.md`.
4. **Compte limité** dans la console des développeurs, et enregistrement de
   `io.github.pobsteta.surculus` avec l'empreinte SHA-256 complète. Relever le plafond d'appareils
   affiché (20 ou 50).
5. Répondre aux questions ci-dessous.

## Questions ouvertes pour Pascal

Découvertes en reprenant l'ADR ; aucune n'est tranchée ici.

1. **Plafond du compte limité : 20 ou 50 appareils ?** Les pages de Google se contredisent (20 : page
   d'accueil et FAQ ; 50 : guide du compte limité). La console dira ce qui s'applique. Le chiffre fixe le
   moment où il faut passer au compte complet (décision 4).
2. **Le compte limité vise « students, teachers, and hobbyists », usage personnel et sans but commercial.**
   Des forestiers qui utilisent Surculus pour leur employeur y entrent-ils ? Si la console le refuse, le
   compte complet (25 USD) devient nécessaire dès le départ. Non vérifié.
3. **Marculus et Groma ont-ils leur propre clé, et où ?** Non vérifié, à demander à Pascal.

## Révisions

- **2026-11-10, rédaction** (26a0f3c) : première version, proposée.
- **2026-11-10, reprise 1/3** : suite à la critique du `contradicteur` et aux huit décisions de Pascal.
  Corrigé : le coût de la perte (la phrase « les saisies non rendues sont perdues » est supprimée :
  contraire à l'ADR 0005 l. 37, 50 et 54), le secret « illisible » (lisible par un workflow), la
  vérification des développeurs (ajoutée, avec ses sources), le crible FRANCE (plus de « à confirmer »),
  la rotation (constat sourcé, plus de question), le repli debug local (renvoi à `docs/publier.md`
  l. 33, question retirée), Marculus et Groma. Ajoutés : les deux marches à suivre, l'examen des voies de
  distribution, la dette, les actions de Pascal. Le format « Java KeyStore ouvert » est retiré : le
  format sera rempli d'après `keytool`.

## Sources

- `pilotage/journal.md` : l. 65 (2026-10-16, création de la clé) ; l. 1151 (5) (2026-11-09,
  « sauvegardée », emplacement non écrit) ; l. 1169, 1170 et 1173 (2026-11-10 : poste de Pascal, mots de
  passe à Pascal seul, clé USB) ; **dernière ligne de la branche `pilotage/2026-11-10`** (les huit
  décisions de Pascal, 2026-11-10 ; pas encore sur `main`, on cite la date).
- `docs/publier.md` l. 27-34 (décision a, pas d'APK sans clé, repli debug en local), l. 65 (commande de création et nom du
  fichier), l. 60-75 (les quatre secrets, renvoi à cet ADR).
- `.github/workflows/release.yml` l. 84-87, 102-104 et 130-132 (lecture des secrets, sans
  `environment:`) ; `app/build.gradle.kts` l. 67 (identifiant), l. 85-110 (signature release, repli
  debug en local seulement), l. 125 (`applicationIdSuffix` du type `fumee`) ;
  `app/src/main/AndroidManifest.xml` l. 43 (autorité suivant l'identifiant).
- `docs/adr/README.md` ; ADR 0004 l. 21, 56 et 105 ; ADR 0005 l. 37, 46, 50 et 54 (sauvegarde,
  désinstallation, lignes sans tournée, restauration).
- `README.md` l. 42-45 (installation par la release GitHub) ; `.gitignore` l. 10-11 ; `CLAUDE.md`
  (« Plier ou tenir », formats ouverts).
- Pages ouvertes et lues le 2026-11-10 : https://developer.android.com/developer-verification ;
  https://developer.android.com/developer-verification/guides/faq ;
  https://developer.android.com/developer-verification/guides/limited-distribution ;
  https://developer.android.com/developer-verification/guides/open-source-app-registration ;
  https://source.android.com/docs/security/features/apksigning/v3 ;
  https://developer.android.com/training/data-storage/shared/media ;
  https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions ;
  https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-deployments/managing-environments-for-deployment ;
  https://f-droid.org/docs/Reproducible_Builds/ ; README d'Obtainium
  (https://github.com/ImranR98/Obtainium).
- JEP 229 (https://openjdk.org/jeps/229) : citée par la critique, **non rouverte** ici.
- **Non vérifié** : le format du fichier de la clé ; l'essai de signature avec la copie USB ; les droits
  `workflow` des jetons des agents ; le plafond d'appareils du compte limité (20 ou 50) ; l'éligibilité
  des forestiers au compte limité ; les clés de Marculus et de Groma ; la cohabitation de deux
  identifiants ; le sort des photos après réinstallation ; la restauration d'un `.sursync` après
  réinstallation ; la licence et la communauté d'Obtainium ; la signature par défaut de F-Droid sans
  build reproductible ; la pose des secrets par `gh secret set --env`.

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
