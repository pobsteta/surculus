#!/usr/bin/env python3
"""Extrait la liste des symboles de vosk-model-small-fr-0.22 (mission 025, test du successeur).

Le modèle « small » n'a pas de graph/words.txt : la table des mots est incorporée à
graph/Gr.fst (en-tête OpenFst, table de symboles). Ce script la lit en octets (taille,
texte, clé) et l'écrit triée en ordre d'octets, un symbole par ligne.

Usage (bibliothèque standard seule) :

    python3 -I fixtures/dictee/outils/extraire_vocabulaire.py \
        vosk-model-small-fr-0.22.zip > fixtures/dictee/vocabulaire-vosk-model-small-fr-0.22.txt

Le premier argument est l'archive du modèle (ou le fichier Gr.fst seul). Le décompte et le
SHA-256 de l'archive sont écrits sur la sortie d'erreur : 135 774 symboles attendus, dont
six ne sont pas des mots (<eps>, #0, <s>, </s>, !SIL, [unk]).
"""
import hashlib
import struct
import sys
import zipfile

MAGIC_SYMTAB = struct.pack("<i", 2125658996)  # 0x7eb2fdf4, magie de SymbolTable (OpenFst)


def lire_gr_fst(chemin):
    if zipfile.is_zipfile(chemin):
        with zipfile.ZipFile(chemin) as z:
            nom = next(n for n in z.namelist() if n.endswith("graph/Gr.fst"))
            return z.read(nom)
    with open(chemin, "rb") as f:
        return f.read()


def symboles(donnees):
    debut = donnees.find(MAGIC_SYMTAB)
    if debut < 0:
        raise SystemExit("table de symboles OpenFst introuvable")

    def chaine(p):
        n = struct.unpack_from("<i", donnees, p)[0]
        return donnees[p + 4 : p + 4 + n], p + 4 + n

    p = debut + 4
    _nom, p = chaine(p)
    _dispo, taille = struct.unpack_from("<qq", donnees, p)
    p += 16
    sortie = []
    for _ in range(taille):
        mot, p = chaine(p)
        p += 8  # clé du symbole
        sortie.append(mot)
    return sortie


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    donnees = lire_gr_fst(sys.argv[1])
    mots = sorted(symboles(donnees))
    sys.stdout.buffer.write(b"\n".join(mots) + b"\n")
    sys.stderr.write(f"{len(mots)} symboles\n")
    if zipfile.is_zipfile(sys.argv[1]):
        h = hashlib.sha256()
        with open(sys.argv[1], "rb") as f:
            for bloc in iter(lambda: f.read(1 << 20), b""):
                h.update(bloc)
        sys.stderr.write(f"sha256 de l'archive : {h.hexdigest()}\n")


if __name__ == "__main__":
    main()
