# CLAUDE.md — surculus

Application Android de suivi des travaux sylvicoles et des plantations
(Surculus). Fait partie de la plateforme Néméton, à côté de Marculus
(martelage) et de Groma (inventaire par vidéo).

## Règles

1. **Ne jamais modifier `sommieR`, `nemeton`, `nemetonshiny`, `marculus` ni
   `groma`** (lecture seule, aucune opération git dans leurs dossiers). Leurs
   évolutions sont livrées en **briefs** : copie de référence dans
   `briefs/vers-<dépôt>/AAAA-MM-JJ-sujet.md`, copie déposée dans
   `/home/pascal/dev/briefs/vers-<dépôt>/`.
2. **Le contrat avec sommieR est le `travaux.gpkg`** écrit par
   `sommier_projet_qfield_travaux()` et relu par
   `sommier_importer_qfield_travaux()`. Surculus n'en renomme ni les couches
   ni les champs, n'écrit dans aucune couche d'aller (sauf `placettes`, en
   ajout) et ne parle jamais à la base de sommieR. Toute évolution du
   contrat passe d'abord par un brief sommieR.
3. **Une saisie = un UUID v4**, fixé à la création, jamais réutilisé : c'est
   lui qui rend l'import rejouable et la fusion multi-opérateurs possible.
   Le journal est append-only ; une correction après import passe par
   sommieR (`corrige_id`), pas par le téléphone.
4. **Les règles d'import de sommieR sont vérifiées sur le téléphone**, et
   chaque règle a un cas dans `fixtures/` (résultat attendu écrit à la main)
   rejoué en Kotlin et, par le brief, en R.
5. **Ne pas copier les briques communes** (GNSS/NTRIP, géodésie,
   GeoPackage, carte, dictée, journal) : elles viennent des bibliothèques
   communes à Marculus et Groma (`briefs/vers-marculus/`). En attendant
   qu'elles existent, une reprise provisoire est signalée dans
   `docs/dependances.md` avec son origine.
6. Signaler toute dépendance non commerciale ou copyleft incompatible avant
   de l'ajouter (`docs/dependances.md`).
7. Tenir `PLAN.md` à jour à chaque étape.

## Conventions

* Kotlin 2.2, JVM 17 pour `:core` (Kotlin pur, testé JVM), Android
  minSdk 26 ; Compose / Material 3, comme Marculus.
* Code et commentaires en anglais ; interface, messages, documentation et
  noms de champs en français. Les noms de couches et de champs du
  GeoPackage sont ceux de sommieR, au caractère près.
* Unités : mètres, hauteurs de plants en cm, surfaces en ha, Lambert-93
  (EPSG:2154) dans le GeoPackage, WGS 84 pour le GNSS.
* Conventional Commits (en français après le type : `feat(core): …`) ; ils
  pilotent les versions (release-please). PR fusionnées en squash, titre
  conforme. Ne jamais modifier les numéros de version à la main.

## Commandes (à partir de la tranche 1)

```bash
cd android && ./gradlew :core:test
cd android && ./gradlew :app:assembleDebug   # SDK Android requis
```
