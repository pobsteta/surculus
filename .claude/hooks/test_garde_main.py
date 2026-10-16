#!/usr/bin/env python3
"""Cas du hook garde_main.py, rejouables sans toucher au dépôt.

    python3 .claude/hooks/test_garde_main.py

Crée un dépôt temporaire (main, plus un worktree sur une branche de mission) et
vérifie, pour chaque ligne de commande, que le hook bloque (2) ou laisse passer (0).
"""
import json
import os
import subprocess
import sys
import tempfile

HOOK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "garde_main.py")
G = "g" + "it"  # le hook de la session lit aussi ce fichier : pas de commande en clair


def git(*args, cwd):
    subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True)


def verdict(commande, cwd):
    entree = json.dumps({"tool_input": {"command": commande}, "cwd": cwd})
    return subprocess.run([sys.executable, HOOK], input=entree,
                          capture_output=True, text=True).returncode


def main():
    with tempfile.TemporaryDirectory() as tmp:
        depot = os.path.join(tmp, "depot")
        wt = os.path.join(tmp, "wt")
        os.mkdir(depot)
        git("init", "-q", "-b", "main", cwd=depot)
        git("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "--allow-empty", "-m", "0",
            cwd=depot)
        git("worktree", "add", "-q", "-b", "mission/x", wt, cwd=depot)

        cas = [
            (f"{G} commit -m x", depot, 2),
            (f"cd {wt} && {G} commit -m x", depot, 0),
            (f"{G} -C {wt} commit -m x", depot, 0),
            (f"{G} checkout -b mission/y && {G} commit -m x", depot, 0),
            (f"{G} switch -c mission/y; {G} commit -m x", depot, 0),
            (f"cd {wt} && {G} checkout main && {G} commit -m x", depot, 2),
            (f"{G} checkout README.md && {G} commit -m x", depot, 2),
            (f"{G} push origin main", wt, 2),
            (f"{G} push", depot, 2),
            (f"{G} push -u origin mission/x", wt, 0),
            (f"{G} push --force-with-lease origin mission/x", wt, 2),
            (f"{G} push origin +mission/x", wt, 2),
            (f"cd {wt} && {G} push", depot, 0),
            (f"{G} commit -m 'a et b'", wt, 0),
        ]
        echecs = 0
        for commande, cwd, attendu in cas:
            obtenu = verdict(commande, cwd)
            lisible = commande.replace(wt, "<worktree>").replace(depot, "<main>")
            print(("ok   " if obtenu == attendu else "ÉCHEC"), attendu, obtenu, lisible)
            echecs += obtenu != attendu
    sys.exit(1 if echecs else 0)


if __name__ == "__main__":
    main()
