#!/usr/bin/env python3
"""Hook PreToolUse (Bash) : main est protégée.

Méthode de l'Impératrice : tout passe par une PR que Pascal valide.
Refuse : un commit fait directement sur main/master, un push vers main/master,
et tout push forcé.
Code de sortie 2 = l'action est bloquée ; le message part vers l'agent.
"""
import json
import os
import re
import shlex
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


def est_branche(cwd, nom):
    try:
        return subprocess.run(
            ["git", "rev-parse", "--verify", "--quiet", f"refs/heads/{nom}"],
            cwd=cwd, capture_output=True, timeout=5,
        ).returncode == 0
    except Exception:
        return False


def resoudre(base, chemin):
    """Chemin absolu de `chemin`, lu depuis `base` (~ et variables développés)."""
    chemin = os.path.expandvars(os.path.expanduser(chemin))
    return os.path.normpath(os.path.join(base or os.getcwd(), chemin))


def mots_de(morceau):
    try:
        return shlex.split(morceau)
    except ValueError:
        return morceau.split()


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

    # Une ligne de commande peut enchaîner plusieurs commandes. On suit, morceau par
    # morceau, le dossier courant (cd, git -C) et la branche quand elle vient d'être
    # créée ou changée dans la même ligne : un agent dans son worktree, ou qui crée sa
    # branche puis commite, n'est pas sur main.
    branche_connue = {}  # dossier -> branche posée plus tôt dans la ligne
    for morceau in re.split(r"&&|\|\||;|\n", commande):
        mots = mots_de(morceau.strip())
        if not mots:
            continue
        if mots[0] == "cd":
            cwd = resoudre(cwd, mots[1] if len(mots) > 1 else "~")
            continue
        if len(mots) < 2 or mots[0] != "git":
            continue
        dossier = cwd
        # git -C chemin commit ...
        while len(mots) > 3 and mots[1] == "-C":
            dossier, mots = resoudre(dossier, mots[2]), ["git"] + mots[3:]
        sous_commande = mots[1]

        def branche():
            return branche_connue.get(dossier) or branche_courante(dossier)

        if sous_commande in ("checkout", "switch"):
            arguments = [a for a in mots[2:] if a != "--"]
            for drapeau in ("-b", "-B", "-c", "-C", "--create", "--force-create"):
                if drapeau in arguments:
                    i = arguments.index(drapeau)
                    if i + 1 < len(arguments):
                        branche_connue[dossier] = arguments[i + 1]
                    break
            else:
                cibles = [a for a in arguments if not a.startswith("-")]
                if len(cibles) == 1 and est_branche(dossier, cibles[0]):
                    branche_connue[dossier] = cibles[0]
            continue

        if sous_commande == "commit" and branche() in PROTEGEES:
            bloquer(
                "Commit refusé : tu es sur main. Une mission = une branche = une PR. "
                "Crée une branche (git switch -c mission/NNN-titre) puis recommence."
            )

        if sous_commande == "push":
            arguments = mots[2:]
            if any(a in ("-f", "--force", "--force-with-lease") or a.startswith("--force")
                   or (a.startswith("+") and len(a) > 1)
                   for a in arguments):
                bloquer("Push forcé refusé : on n'efface pas l'historique (règle du sommier).")
            cibles = [a for a in arguments if not a.startswith("-")]
            refspecs = cibles[1:]  # cibles[0] est le dépôt distant
            vise_main = any(
                r.split(":")[-1].removeprefix("refs/heads/") in PROTEGEES for r in refspecs
            )
            if not refspecs and branche() in PROTEGEES:
                vise_main = True
            if vise_main:
                bloquer(
                    "Push vers main refusé : tout passe par une PR que Pascal valide. "
                    "Pousse ta branche de mission et ouvre une PR."
                )
    sys.exit(0)


if __name__ == "__main__":
    main()
