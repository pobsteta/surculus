#!/bin/sh
# Refait les exemples .sursync v3 (mission 091, testeur) à partir de exemple-v3.jsonl, avec des outils libres
# (sh, sed, grep, head, wc, cut, sha256sum, gzip). Octets reproductibles : gzip -n (ni nom ni date dans l'en-tête gzip), -9.
# Même recette que exemple-v2-fabrication.sh (mission 088).
#
#   exemple-v3.jsonl                     contenu décompressé lisible, pied compris (recalculé par ce script, qui ignore
#                                        le pied présent) : en-tête (version 3, tournée COUCHEY-2026) et quatre saisies
#                                        `controle`, dans l'ordre total du journal (saisi_le_utc_ms, appareil, sequence) :
#                                        1. P35-03, valeur **v2** (contrôle ancien, terminé sans besoin ni concurrence :
#                                           une sauvegarde v3 porte aussi les valeurs v1 et v2 du journal, sans perte) ;
#                                        2. P35-01, valeur v3, récepteur RTK fixe, 0,02 m, non simulée, besoin « aucun »,
#                                           concurrence « moyenne », referentiel_source « RGF93 » (flux NTRIP Centipède) ;
#                                        3. P35-02, valeur v3, ouvert sans position mesurée : precision_m, source_gnss,
#                                           simulee et referentiel_source à null (jamais « faux »), besoin « RG »,
#                                           concurrence « forte » ;
#                                        4. P35-04, valeur v3, GNSS du téléphone, 3,8 m, position simulée (isMock), besoin
#                                           « aucun », concurrence « faible », referentiel_source « WGS84 ».
#                                        Mission 094 (testeur, décisions de Pascal du 2026-11-06, point 3) : ajout
#                                        compatible en v3, la clé referentiel_source (RGF93, WGS84 ou null) est la
#                                        dernière de la valeur, après concurrence ; P35-03 (v2) ne la porte pas.
#                                        Écrit à la main (clés des saisies dans l'ordre de exemple-v2.jsonl). Dans la
#                                        valeur v3, les clés nouvelles sont placées ainsi : precision_m, source_gnss,
#                                        simulee après sans_position_mesuree ; besoin, concurrence, referentiel_source en fin. C'est une
#                                        proposition du testeur : docs/formats/journal-valeurs-v3.md (orchestrateur) fait
#                                        foi ; la sauvegarde recopie la valeur au caractère près, quel que soit cet ordre.
#                                        Domaines : besoin = un code de la couche `codes` de la tournée (RG, sommieR 0.31.1,
#                                        R/registre6.R l. 17-45) ou « aucun » ; concurrence ∈ faible, moyenne, forte
#                                        (R/registre6.R l. 64) ; source_gnss ∈ telephone, recepteur_autonome,
#                                        recepteur_dgps, recepteur_rtk_flottant, recepteur_rtk_fixe (décision de Pascal
#                                        du 2026-11-04, point 4). Lambert-93 (EPSG:2154), x = Est, y = Nord, mètres.
#   exemple-v3.sursync                   exemple-v3.jsonl + pied {"sha256": SHA-256 des octets décompressés qui
#                                        précèdent, fins de ligne comprises}, en gzip
#   exemple-v3-empreinte-fausse.sursync  une valeur changée (position_mesuree_x de P35-01, 846165.36 -> 846169.36),
#                                        pied de l'original inchangé : empreinte fausse ; P35-02 n'est pas touché
#   exemple-v3-tronque.sursync           exemple-v3.sursync amputé de ses 10 derniers octets (comme exemple-v2-tronque)
#
# Lancer : sh fixtures/sursync/exemple-v3-fabrication.sh   (depuis n'importe quel dossier)
set -eu
cd "$(dirname "$0")"
src=$(mktemp)
trap 'rm -f "$src"' EXIT
grep -v '^{"sha256":' exemple-v3.jsonl > "$src"   # contenu sans le pied (le pied est recalculé)
empreinte=$(sha256sum "$src" | cut -d' ' -f1)
pied="{\"sha256\":\"$empreinte\"}"
{ cat "$src"; printf '%s\n' "$pied"; } > exemple-v3.jsonl   # comme exemple-v2.jsonl : le pied compris
gzip -n -9 < exemple-v3.jsonl > exemple-v3.sursync
{ sed 's/"position_mesuree_x\\":846165.36,/"position_mesuree_x\\":846169.36,/' "$src"; printf '%s\n' "$pied"; } | gzip -n -9 > exemple-v3-empreinte-fausse.sursync
taille=$(wc -c < exemple-v3.sursync)
head -c $((taille - 10)) exemple-v3.sursync > exemple-v3-tronque.sursync
echo "empreinte : $empreinte"
