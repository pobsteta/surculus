#!/usr/bin/env python3
"""Hook PreToolUse (Edit|Write|Bash) : docs/journal.md est en ajout seul.

Règle du sommier (méthode de l'Impératrice, fiche n° 11) : on n'efface pas, on ajoute.
Une correction est une nouvelle ligne datée qui renvoie à l'ancienne.
Code de sortie 2 = l'action est bloquée ; le message part vers l'agent.
"""
import json
import os
import re
import sys

JOURNAL = os.path.join("docs", "journal.md")
MESSAGE = (
    "Modification refusée : docs/journal.md est en ajout seul (règle du sommier). "
    "On n'efface pas, on ajoute : écris une nouvelle ligne datée en fin de fichier, "
    "et pour corriger, une ligne qui renvoie à l'ancienne."
)


def bloquer():
    print(MESSAGE, file=sys.stderr)
    sys.exit(2)


def est_journal(chemin):
    return bool(chemin) and os.path.normpath(chemin).endswith(JOURNAL)


def main():
    try:
        entree = json.load(sys.stdin)
    except Exception:
        sys.exit(0)
    outil = entree.get("tool_name", "")
    params = entree.get("tool_input") or {}
    chemin = params.get("file_path", "")

    if outil == "Edit" and est_journal(chemin):
        ancien = params.get("old_string", "")
        nouveau = params.get("new_string", "")
        # Seul ajout permis : le nouveau texte reprend l'ancien tel quel, puis ajoute.
        if params.get("replace_all") or not nouveau.startswith(ancien):
            bloquer()

    elif outil == "Write" and est_journal(chemin):
        try:
            with open(chemin, encoding="utf-8") as f:
                existant = f.read()
        except FileNotFoundError:
            existant = ""
        if not params.get("content", "").startswith(existant):
            bloquer()

    elif outil == "Bash":
        commande = params.get("command", "") or ""
        if "journal.md" in commande:
            # Interdits : écrasement par > simple, sed -i, suppression, déplacement, troncature.
            if (re.search(r"(?<![>&0-9])>\s*\S*journal\.md", commande)
                    or re.search(r"\bsed\b[^|;&]*-i[^|;&]*journal\.md", commande)
                    or re.search(r"\b(rm|mv|truncate)\b[^|;&]*journal\.md", commande)
                    or re.search(r"git\s+(checkout|restore)\b[^|;&]*journal\.md", commande)):
                bloquer()
    sys.exit(0)


if __name__ == "__main__":
    main()
