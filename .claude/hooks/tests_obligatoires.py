#!/usr/bin/env python3
"""Hook Stop / SubagentStop : pas de « terminé » sans tests verts.

Méthode de l'Impératrice, fiche n° 15 : « avec l'IA, et surtout avec elle, les tests
sont obligatoires ». Si du code a changé (hors docs/ et .claude/) et que la commande
de tests du projet est définie dans .claude/test-command, on la lance ; si elle
échoue, l'agent ne peut pas s'arrêter et reçoit la sortie des tests.
Tant que la stack n'est pas choisie (pas de .claude/test-command), le hook ne fait rien.
"""
import json
import os
import subprocess
import sys

DELAI = 600  # secondes


def main():
    try:
        entree = json.load(sys.stdin)
    except Exception:
        entree = {}
    # Évite la boucle infinie : si l'agent repart déjà à cause de ce hook, on le laisse finir.
    if entree.get("stop_hook_active"):
        sys.exit(0)

    racine = os.environ.get("CLAUDE_PROJECT_DIR") or entree.get("cwd") or os.getcwd()
    cwd = entree.get("cwd") or racine
    fichier_commande = os.path.join(racine, ".claude", "test-command")
    if not os.path.isfile(fichier_commande):
        sys.exit(0)
    with open(fichier_commande, encoding="utf-8") as f:
        commande = f.read().strip()
    if not commande:
        sys.exit(0)

    try:
        statut = subprocess.run(
            ["git", "status", "--porcelain"], cwd=cwd,
            capture_output=True, text=True, timeout=10,
        ).stdout.splitlines()
    except Exception:
        sys.exit(0)
    code_modifie = [
        l for l in statut
        if not l[3:].startswith(("docs/", ".claude/", "README", "CLAUDE.md"))
    ]
    if not code_modifie:
        sys.exit(0)

    # Un worktree d'agent n'a pas de local.properties : sans ANDROID_HOME, le build
    # Android échoue (« SDK location not found »). Repli sur l'emplacement standard
    # du SDK s'il existe sur ce poste ; rien n'est écrit dans le dépôt.
    env = dict(os.environ)
    if not env.get("ANDROID_HOME") and not os.path.isfile(os.path.join(cwd, "local.properties")):
        for candidat in (os.path.expanduser("~/Android/Sdk"), os.path.expanduser("~/Library/Android/sdk")):
            if os.path.isdir(candidat):
                env["ANDROID_HOME"] = candidat
                break

    # Deux builds Gradle dans le même dossier se gênent (fichiers de build tronqués) :
    # un verrou par dossier de travail fait attendre le second (incident du 2026-10-16).
    verrou = None
    try:
        dossier_git = subprocess.run(
            ["git", "rev-parse", "--absolute-git-dir"], cwd=cwd,
            capture_output=True, text=True, timeout=10,
        ).stdout.strip()
        if dossier_git:
            import fcntl
            verrou = open(os.path.join(dossier_git, "surculus-tests.lock"), "w")
            fcntl.flock(verrou, fcntl.LOCK_EX)
    except Exception:
        verrou = None

    try:
        resultat = subprocess.run(
            commande, shell=True, cwd=cwd, env=env,
            capture_output=True, text=True, timeout=DELAI,
        )
    except subprocess.TimeoutExpired:
        print(f"Tests trop longs (> {DELAI} s) : impossible de vérifier. "
              "Dis-le à l'orchestrateur, ne déclare pas la mission terminée.", file=sys.stderr)
        sys.exit(2)

    if resultat.returncode != 0:
        sortie = (resultat.stdout + "\n" + resultat.stderr)[-3000:]
        print(
            "Mission non terminée : les tests échouent (« un code qui a l'air juste "
            "n'est pas un code vérifié »). Corrige ou dis ce qui bloque.\n\n" + sortie,
            file=sys.stderr,
        )
        sys.exit(2)
    sys.exit(0)


if __name__ == "__main__":
    main()
