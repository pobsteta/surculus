#!/bin/sh
# Refait les exemples .sursync v2 (mission 088, testeur) à partir de exemple-v2.jsonl, avec des outils libres
# (sh, sed, head, wc, cut, sha256sum, gzip). Octets reproductibles : gzip -n (ni nom ni date dans l'en-tête gzip), -9.
#
#   exemple-v2.jsonl                     contenu décompressé lisible, pied compris (recalculé par ce script, qui
#                                        ignore le pied présent) : en-tête (version 2) et deux
#                                        saisies `controle` en valeurs v2 du journal (docs/formats/journal-valeurs-v2.md) :
#                                        la première ouverte sans position mesurée (exemple du format, au caractère
#                                        près), la seconde avec une position mesurée à 2,5 m du piquet.
#                                        Écrit à la main (clés dans l'ordre de exemple-v1.jsonl).
#   exemple-v2.sursync                   exemple-v2.jsonl + pied {"sha256": SHA-256 des octets décompressés qui
#                                        précèdent, fins de ligne comprises}, en gzip
#   exemple-v2-empreinte-fausse.sursync  une valeur changée (position_mesuree_x du contrôle AVEC position,
#                                        846201.5 -> 846209.5), pied de l'original inchangé : empreinte fausse ;
#                                        le contrôle sans position n'est pas touché
#   exemple-v2-tronque.sursync           exemple-v2.sursync amputé de ses 10 derniers octets (comme exemple-v1-tronque)
#
# Lancer : sh fixtures/sursync/exemple-v2-fabrication.sh   (depuis n'importe quel dossier)
set -eu
cd "$(dirname "$0")"
src=$(mktemp)
trap 'rm -f "$src"' EXIT
grep -v '^{"sha256":' exemple-v2.jsonl > "$src"   # contenu sans le pied (le pied est recalculé)
empreinte=$(sha256sum "$src" | cut -d' ' -f1)
pied="{\"sha256\":\"$empreinte\"}"
{ cat "$src"; printf '%s\n' "$pied"; } > exemple-v2.jsonl   # comme exemple-v1.jsonl : le pied compris
gzip -n -9 < exemple-v2.jsonl > exemple-v2.sursync
{ sed 's/"position_mesuree_x\\":846201.5,/"position_mesuree_x\\":846209.5,/' "$src"; printf '%s\n' "$pied"; } | gzip -n -9 > exemple-v2-empreinte-fausse.sursync
taille=$(wc -c < exemple-v2.sursync)
head -c $((taille - 10)) exemple-v2.sursync > exemple-v2-tronque.sursync
echo "empreinte : $empreinte"
