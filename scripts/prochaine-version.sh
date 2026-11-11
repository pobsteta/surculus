#!/usr/bin/env bash
# Version calendaire d'une release : AAAA.M.N (mission 108, comme nemeton).
#   AAAA.M : année et mois de l'instant du lancement, à l'heure de Paris (Europe/Paris), mois sans zéro devant ;
#   N      : 1 + le plus grand N des tags vAAAA.M.* du même mois (comparaison numérique), 1 s'il n'y en a aucun.
# Les tags qui n'ont pas la forme vAAAA.M.N (v0.7.x, essais...) sont ignorés. Un trou n'est pas comblé.
# Usage : prochaine-version.sh <instant ISO 8601 en UTC, ex. 2026-11-30T22:30:00Z>   (tags, un par ligne, sur l'entrée)
# Affiche AAAA.M.N (sans « v »). Ne lit aucun secret, n'appelle ni Git ni le réseau.
set -euo pipefail
instant="${1:?instant ISO 8601 en UTC (ex. 2026-11-30T22:30:00Z)}"
case "$instant" in
  [0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[0-9][0-9]:[0-9][0-9]:[0-9][0-9]Z) ;;
  *) echo "Instant mal formé : $instant (attendu AAAA-MM-JJThh:mm:ssZ, en UTC)" >&2; exit 2 ;;
esac
mois=$(TZ=Europe/Paris date -d "$instant" +%Y.%-m)
dernier=0
while IFS= read -r tag; do
  if [[ "$tag" =~ ^v([0-9]{4})\.([1-9]|1[0-2])\.([1-9][0-9]*)$ ]]; then
    [ "${BASH_REMATCH[1]}.${BASH_REMATCH[2]}" = "$mois" ] || continue
    n="${BASH_REMATCH[3]}"
    [ "$n" -gt "$dernier" ] && dernier="$n"
  fi
done
echo "$mois.$((dernier + 1))"
