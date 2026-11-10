# ADR 0010 — Clé de signature et distribution

- **Date :** 2026-11-10
- **Statut :** proposée (pas encore critiquée par le `contradicteur`, pas encore tranchée par Pascal)
- **Proposée par :** `archiviste` (Claude), à partir des décisions de Pascal du journal
- **Tranchée par :** Pascal

## La décision en une phrase

La clé qui signe les releases de Surculus (`surculus-release.keystore`) existe en **deux copies
lisibles** : l'une sur le **poste de Pascal**, l'autre **hors du poste, sur une clé USB** ;
**Pascal seul détient les mots de passe** ; les releases sont distribuées en APK signé sur les
releases GitHub du dépôt, et **le secret GitHub n'est pas une copie de secours**.

Cet ADR dit **où** sont les copies. Il ne contient ni mot de passe, ni élément de la clé.

## Contexte

La clé a été créée le 2026-10-16, avant cet ADR (journal, l. 65) : RSA 4096, alias `surculus`,
valable jusqu'en 2054, gardée hors du dépôt (`*.keystore` et `*.jks` sont ignorés, `.gitignore`
l. 10-11). Le journal du 2026-10-16 demandait déjà à Pascal de la sauvegarder hors du poste ; les
bilans du 2026-10-21 et du 2026-11-09 ont relevé que l'ADR 0010 manquait (`docs/publier.md`
l. 75 la disait « à venir »).

**Ce que la perte coûte.** Android n'installe une mise à jour que si elle porte la même signature
que la version installée. Sans la clé :
- aucune nouvelle version ne s'installe par-dessus l'ancienne sur les téléphones ;
- il faut désinstaller, et **les saisies non rendues sont perdues à la désinstallation** ;
  l'ADR 0005 (l. 37 et 50) prévoit pourtant une sauvegarde `.sursync` dans le stockage partagé qui
  survit à la désinstallation : sa restauration sur une version de signature différente n'est
  pas vérifiée ici, donc on ne s'y fie pas ;
- une clé neuve ne répare rien pour les téléphones déjà installés.
`docs/publier.md` l. 27-34 l'a déjà tranché dans l'autre sens : pas d'APK sans clé, le repli sur la
clé de debug est supprimé pour les releases, parce que la clé de debug de chaque runner change à
chaque exécution. Mieux vaut pas d'APK qu'un APK qui piège le forestier.

**Un secret GitHub ne se relit pas.** `RELEASE_KEYSTORE_BASE64` (`docs/publier.md` l. 69) permet
au workflow `release.yml` de signer, mais GitHub ne montre plus la valeur d'un secret une fois
posé. Ce n'est donc **pas** une sauvegarde : si le poste et la clé USB disparaissent, le secret
continue de signer tant que le dépôt vit, mais personne ne peut en extraire la clé, ni signer
ailleurs, ni changer de forge. Il dépend en outre d'un compte et d'un service.

**Options envisagées**

| | Option | Remarque |
|---|---|---|
| a | Poste seul + secret GitHub | Situation du 2026-11-09 : une seule copie lisible. Écartée. |
| b | Poste + une clé USB rangée ailleurs | **Retenue par Pascal** (journal, 2026-11-10). |
| c | Poste + gestionnaire de mots de passe (pièce jointe chiffrée) | Évoquée par le journal du 2026-11-10 ; non retenue par Pascal. |
| d | Deux détenteurs de mots de passe, ou dépôt sous séquestre | Rend un successeur possible ; **non décidé**, voir les questions ouvertes. |
| e | Signature par un service tiers (Play App Signing, par exemple) | Suppose de publier sur un magasin ; hors tranche 1 et hors de ce qui est décidé. |

**Distribution actuelle.** À chaque fusion dans `main`, `release.yml` signe l'APK avec les quatre
secrets, le dépose sur la release GitHub ; le forestier le télécharge et l'installe
(`README.md` l. 42-45, Android 8.0 ou plus récent). Pas de magasin d'applications.

## Où se trouvent les copies

| Copie | Emplacement | Qui y accède | Date de la mention |
|---|---|---|---|
| 1, de travail | Poste de Pascal, `~/.android-keys/` (droits 600), journal 2026-10-16 | Pascal | 2026-10-16 |
| 2, de secours | Clé USB, **hors du poste** ; lieu de rangement non écrit ici | Pascal | 2026-11-10 |
| 3, de signature seule | Secret GitHub `RELEASE_KEYSTORE_BASE64`, **illisible** | Le workflow `release.yml` | 2026-10-16 |

Seules les copies 1 et 2 comptent comme sauvegarde. Les mots de passe (`RELEASE_KEYSTORE_PASSWORD`,
`RELEASE_KEY_PASSWORD`) et l'alias (`RELEASE_KEY_ALIAS`) sont détenus par **Pascal seul**. L'empreinte
SHA-256 de la clé est notée au journal du 2026-10-16 : c'est ce qui permet de vérifier, sans la
clé, qu'un APK est bien signé de la bonne.

## Le crible FRANCE

| Lettre | Question | Réponse (une ligne) | Ce que ça coûte | Prêts à payer ? |
|---|---|---|---|---|
| **F**rugale | La plus simple qui tienne ? | Un fichier, deux copies, un détenteur | Une clé USB à ranger et à retrouver | À confirmer par Pascal |
| **R**esponsable | Qui fait quoi ? | Pascal garde la clé et les mots de passe ; la CI signe | Tout repose sur une personne | À confirmer par Pascal |
| **A**nticiper | Si l'imprévu arrive ? | Poste perdu : la clé USB reste. Poste et USB perdus : plus de mise à jour possible, désinstallation et perte des saisies non rendues. Pascal indisponible : personne ne publie ni ne récupère les mots de passe | Risque assumé tant que la question du successeur n'est pas tranchée | **Non tranché** |
| **N**ovateur | Toutes les idées écoutées ? | Options a à e ci-dessus | — | Oui |
| **C**oût | Coût pour l'ensemble ? | Presque nul en argent ; la perte de la clé coûte une réinstallation chez chaque forestier | Une réinstallation générale en cas de perte | Oui |
| **É**cologique | Que réutilisons-nous ? | Outils standard (`keytool`, `apksigner`), format Java KeyStore ouvert, aucun service propriétaire pour signer | Dépendance à GitHub Actions pour la signature automatique | Oui |

## Valeurs sacrifiées

- **La continuité si Pascal est indisponible** : un seul détenteur de mots de passe est un point
  de défaillance unique. Sacrifice accepté par Pascal à ce jour, **à revoir** (test du successeur,
  `CLAUDE.md`).
- **La résistance à un sinistre commun** : si la clé USB est rangée avec le poste, la seconde copie
  ne protège que du disque en panne.
- **Le confort de l'installation** : pas de magasin, donc pas de mise à jour automatique ni de
  vérification par un tiers ; le forestier installe à la main.

## Questions ouvertes pour Pascal

Aucune n'est tranchée ici.

1. **Le successeur.** Si Pascal est indisponible, qui publie ? Quelqu'un d'autre doit-il pouvoir
   accéder aux mots de passe (enveloppe scellée, gestionnaire partagé, personne de confiance), ou
   assume-t-on qu'il n'y a pas de publication sans Pascal ?
2. **La copie USB a-t-elle été essayée ?** Le journal dit qu'elle existe, pas qu'on a signé un APK
   avec elle ni comparé l'empreinte SHA-256 à celle du journal. Faut-il l'essayer une fois, et
   inscrire la date ?
3. **Où est rangée la clé USB ?** Le lieu est volontairement absent de cet ADR ; faut-il l'écrire
   ailleurs (hors du dépôt) pour qu'un tiers de confiance puisse la retrouver ?
4. **Les mots de passe ont-ils une sauvegarde à eux ?** Le keystore sans mot de passe ne sert à
   rien, et ils tiennent à une seule mémoire.
5. **Rotation et expiration.** La clé court jusqu'en 2054. Veut-on prévoir une rotation de clé, ou
   assume-t-on qu'une perte entraîne une réinstallation générale ? (Mécanisme de rotation d'Android
   : non vérifié ici.)
6. **Vérification des développeurs d'Android.** L'ADR 0004 (l. 56 et 105) renvoie à cet ADR pour
   la « vérification des développeurs » ; aucune analyse n'existe encore. Faut-il l'ouvrir avant la
   prochaine tranche, et la distribution hors magasin tiendra-t-elle sur les téléphones des
   forestiers ? Non vérifié.
7. **Le repli debug en local.** `docs/publier.md` garde le repli sur la clé de
   debug pour `assembleRelease` en local. Cela ne concerne pas les releases publiées, mais un APK
   local ne s'installe pas par-dessus une release : le dire aux essais ? (Question de périmètre,
   laissée à Pascal.)

## Sources

- `pilotage/journal.md` : l. 65 (2026-10-16, création de la clé) ; l. 1151 (5) (2026-11-09,
  « sauvegardée », emplacement non écrit) ; l. 1169, 1170 et 1173 (2026-11-10 : poste de Pascal,
  mots de passe à Pascal seul, clé USB).
- `docs/publier.md` l. 27-34 (décision a, pas d'APK sans clé), l. 60-75 (les quatre secrets), l. 75
  (renvoi à cet ADR).
- `.github/workflows/release.yml` l. 84-91 (lecture des quatre secrets, décodage du keystore) ;
  `app/build.gradle.kts` l. 85-110 (signature release, repli debug en local seulement).
- `docs/adr/README.md` l. 18 (0010 réservée) ; ADR 0004 l. 56 et 105 ; ADR 0005 l. 37 et 50 (sauvegarde
  et désinstallation).
- `README.md` l. 42-45 (installation par la release GitHub) ; `.gitignore` l. 10-11.
- Non vérifié : l'essai de signature avec la copie USB ; le lieu de rangement ; la rotation de clé
  d'Android ; les exigences de la vérification des développeurs
  (https://developer.android.com/developer-verification, citée par l'ADR 0004).

## Question de contrôle

Cette décision tient-elle nos six valeurs, et que sommes-nous prêts à payer pour cela ?
