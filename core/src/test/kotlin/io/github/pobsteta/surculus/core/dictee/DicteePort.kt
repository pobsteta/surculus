package io.github.pobsteta.surculus.core.dictee

import com.google.gson.JsonObject

/**
 * Port de test de la mission 013 (dictée à grammaire fermée), décrit depuis `fixtures/dictee/` et
 * `fixtures/README.md` (section Dictée), sans lire de code de production.
 *
 * Branché par la mission 013 sur le code de production via [ImplementationDictee.dictee] (adaptateur en bas de ce
 * fichier). Les cas de `fixtures/dictee/` ne changent pas.
 *
 * Résultats (JSON, comparés par [io.github.pobsteta.surculus.core.fixtures.Comparaison]) :
 * `{"accepte": true, "commande": {"type": ...}}` ou `{"accepte": false}` (libellé du refus libre).
 */
interface Dictee {
    /** Analyse la phrase rendue par le moteur (minuscules, sans ponctuation), sans contexte. */
    fun analyser(phrase: String): JsonObject

    /** Même phrase confrontée à l'état courant du plant (`sain`, `atteint`, `mort`, `absent`, `non_observe`, ou null : aucun état). */
    fun appliquer(phrase: String, etatCourant: String?): JsonObject

    /** Tous les mots de la grammaire, tels que le moteur doit les connaître. */
    fun motsDeLaGrammaire(): Set<String>

    /** Vérifie nom et empreinte SHA-256 d'un modèle ; `contenu` null = fichier absent. Résultat : `{"accepte": bool}`. */
    fun verifierModele(nomModele: String, contenu: ByteArray?, sha256Attendu: String): JsonObject
}

/** Branchement de la mission 013 : adaptateur du port [Dictee] vers le code de production (`AnalyseurDictee`, `VerificationModele`). */
object ImplementationDictee {
    fun dictee(): Dictee = AdaptateurDictee
}

private object AdaptateurDictee : Dictee {
    override fun analyser(phrase: String) = enJson(AnalyseurDictee.analyser(phrase))

    override fun appliquer(phrase: String, etatCourant: String?): JsonObject {
        val etat = EtatDicte.deCode(etatCourant)
        require(etatCourant == null || etat != null) { "état inconnu : $etatCourant" }
        return enJson(AnalyseurDictee.appliquer(phrase, etat))
    }

    override fun motsDeLaGrammaire(): Set<String> = object : MoteurDictee {
        override fun demarrer(ecouteur: EcouteurDictee) = Unit
        override fun arreter() = Unit
    }.motsDeLaGrammaire()

    override fun verifierModele(nomModele: String, contenu: ByteArray?, sha256Attendu: String): JsonObject =
        JsonObject().apply { addProperty("accepte", VerificationModele.verifier(nomModele, contenu?.inputStream(), sha256Attendu)) }

    private fun enJson(r: ResultatDictee): JsonObject = JsonObject().apply {
        when (r) {
            is ResultatDictee.Refus -> { addProperty("accepte", false); addProperty("motif", r.motif) }
            is ResultatDictee.Accepte -> {
                addProperty("accepte", true)
                add("commande", commandeEnJson(r.commande))
                addProperty("correction", r.correction)
                addProperty("annonce_vocale", r.annonceVocale)
                addProperty("ligne_ajoutee", r.ligneAjoutee)
                addProperty("confirmation_vocale", r.confirmationVocale)
                r.mesuresConservees?.let { addProperty("mesures_conservees", it) }
            }
        }
    }

    private fun commandeEnJson(c: Commande) = JsonObject().apply {
        when (c) {
            is Commande.Etat -> { addProperty("type", "etat"); addProperty("etat", c.etat.code) }
            is Commande.Cause -> { addProperty("type", "cause"); addProperty("cause_groupe", c.causeGroupe) }
            is Commande.Detail -> { addProperty("type", "detail"); addProperty("cause_detail", c.causeDetail) }
            is Commande.Hauteur -> { addProperty("type", "hauteur"); addProperty("hauteur_cm", c.hauteurCm) }
            Commande.Suivant -> addProperty("type", "suivant")
            Commande.Regarni -> addProperty("type", "regarni")
            Commande.VideDebut -> addProperty("type", "vide_debut")
            Commande.VideFin -> addProperty("type", "vide_fin")
            Commande.Annule -> addProperty("type", "annule")
        }
    }
}
