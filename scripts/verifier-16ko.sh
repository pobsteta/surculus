#!/usr/bin/env bash
# Contrôle de compatibilité avec les pages mémoire de 16 Ko (ADR 0004).
# Méthode : https://developer.android.com/guide/practices/page-sizes
#   1. zipalign -c -P 16 -v 4 : les .so non compressés sont alignés sur 16 Ko dans l'APK ;
#   2. readelf -lW : chaque segment LOAD des .so 64 bits est aligné sur au moins 2**14.
# Usage : scripts/verifier-16ko.sh chemin/vers/app.apk
# Requiert : ANDROID_HOME (build-tools), unzip, readelf (binutils).
set -euo pipefail

apk="${1:?usage : $0 chemin/vers/app.apk}"
[ -f "$apk" ] || { echo "ÉCHEC : APK introuvable : $apk" >&2; exit 1; }

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$sdk" ] || { echo "ÉCHEC : ANDROID_HOME non défini" >&2; exit 1; }
zipalign="$(ls -d "$sdk"/build-tools/*/zipalign 2>/dev/null | sort -V | tail -n 1)"
[ -x "$zipalign" ] || { echo "ÉCHEC : zipalign introuvable sous $sdk/build-tools" >&2; exit 1; }

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
journal="$tmp/zipalign.log"

echo "== zipalign -c -P 16 -v 4 ($zipalign)"
if ! "$zipalign" -c -P 16 -v 4 "$apk" > "$journal" 2>&1; then
    cat "$journal"
    echo "ÉCHEC : alignement zip sur 16 Ko" >&2
    exit 1
fi
grep -E '\.so' "$journal" || true
tail -n 1 "$journal"

unzip -q "$apk" 'lib/*' -d "$tmp" 2>/dev/null || true

# Un APK sans bibliothèque native rendrait le contrôle vide : on échoue plutôt que de passer.
mapfile -t libs < <(find "$tmp/lib/arm64-v8a" "$tmp/lib/x86_64" -name '*.so' 2>/dev/null | sort)
[ "${#libs[@]}" -gt 0 ] || { echo "ÉCHEC : aucune bibliothèque native 64 bits dans l'APK" >&2; exit 1; }

echo "== Alignement ELF des segments LOAD (minimum 2**14 = 0x4000)"
echec=0
for so in "${libs[@]}"; do
    nom="${so#"$tmp"/}"
    min=""
    while read -r align; do
        v=$((align))
        if [ -z "$min" ] || [ "$v" -lt "$min" ]; then min="$v"; fi
    done < <(readelf -lW "$so" | awk '$1 == "LOAD" { print $NF }')
    if [ -z "$min" ]; then
        echo "ÉCHEC  $nom : aucun segment LOAD"
        echec=1
    elif [ "$min" -lt 16384 ]; then
        printf 'ÉCHEC  %s : alignement 0x%x\n' "$nom" "$min"
        echec=1
    else
        printf 'OK     %s : alignement 0x%x\n' "$nom" "$min"
    fi
done

if [ "$echec" -ne 0 ]; then
    echo "ÉCHEC : au moins une bibliothèque native n'est pas alignée sur 16 Ko" >&2
    exit 1
fi
echo "Contrôle 16 Ko : réussi (${#libs[@]} bibliothèques 64 bits)"
