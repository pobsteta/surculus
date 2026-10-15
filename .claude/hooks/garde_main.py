#!/usr/bin/env python3
"""Hook PreToolUse (Bash) : main est protégée.

Méthode de l'Impératrice : tout passe par une PR que Pascal valide.
Refuse : un commit fait directement sur main/master, un push vers main/master,
et tout push forcé.
Code de sortie 2 = l'action est bloquée ; le message part vers l'agent.
"""
import json
import re
import subprocess
import sys

PROTEGEES = ("main", "master")


def branche_courante(cwd):
    try:
        return subprocess.run(
            ["git", "rev-parse", "--abbrev-ref", "HEAD"],
            cwd=cwd, capture_output=True, text=True, timeout=5,
        ).stdout.strip()
    except Exception:
        return ""


def bloquer(message):
    print(message, file=sys.stderr)
    sys.exit(2)


def main():
    try:
        entree = json.load(sys.stdin)
    except Exception:
        sys.exit(0)
    commande = (entree.get("tool_input") or {}).get("command", "") or ""
    cwd = entree.get("cwd") or None

    # Une ligne de commande peut enchaîner plusieurs commandes.
    for morceau in re.split(r"&&|\|\||;|\n", commande):
        mots = morceau.split()
        if len(mots) < 2 or mots[0] != "git":
            continue
        # git -C chemin commit ...
        if mots[1] == "-C" and len(mots) > 3:
            cwd, mots = mots[2], ["git"] + mots[3:]
        sous_commande = mots[1]

        if sous_commande == "commit" and branche_courante(cwd) in PROTEGEES:
            bloquer(
                "Commit refusé : tu es sur main. Une mission = une branche = une PR. "
                "Crée une branche (git switch -c mission/NNN-titre) puis recommence."
            )

        if sous_commande == "push":
            arguments = mots[2:]
            if any(a in ("-f", "--force", "--force-with-lease") or a.startswith("--force")
                   for a in arguments):
                bloquer("Push forcé refusé : on n'efface pas l'historique (règle du sommier).")
            cibles = [a for a in arguments if not a.startswith("-")]
            refspecs = cibles[1:]  # cibles[0] est le dépôt distant
            vise_main = any(
                r.split(":")[-1].removeprefix("refs/heads/") in PROTEGEES for r in refspecs
            )
            if not refspecs and branche_courante(cwd) in PROTEGEES:
                vise_main = True
            if vise_main:
                bloquer(
                    "Push vers main refusé : tout passe par une PR que Pascal valide. "
                    "Pousse ta branche de mission et ouvre une PR."
                )
    sys.exit(0)


if __name__ == "__main__":
    main()
