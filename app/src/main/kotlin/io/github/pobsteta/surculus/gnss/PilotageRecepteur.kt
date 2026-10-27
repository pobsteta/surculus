/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gnss

import android.content.Context

/** Le récepteur externe vu du pilotage : testable sans Android par une source injectée. */
interface Recepteur {
    fun enMarche(): Boolean
    fun demarrer()
    fun arreter()
}

/** Le vrai récepteur : [ServiceGnssRtk]. */
class RecepteurService(private val ctx: Context) : Recepteur {
    override fun enMarche() = ServiceGnssRtk.etat.value.enMarche
    override fun demarrer() = ServiceGnssRtk.demarrer(ctx)
    override fun arreter() = ServiceGnssRtk.arreter(ctx)
}

/**
 * Mission 047, comme Groma : le récepteur est « démarré à l'ouverture de la carte, arrêté à la sortie du projet ».
 * [actif] relit le réglage des Paramètres à chaque entrée ; inactif, le récepteur ne démarre jamais.
 * Aucune règle GNSS ici : la politique du fix reste celle de l'ADR 0003.
 */
class PilotageRecepteur(private val recepteur: Recepteur, private val actif: () -> Boolean) {
    /** À l'ouverture de la carte (ou de l'essai) : démarre si le réglage est actif et que le récepteur ne tourne pas déjà. */
    fun entrer() {
        if (actif() && !recepteur.enMarche()) recepteur.demarrer()
    }

    /** À la sortie : libère le récepteur. */
    fun sortir() = recepteur.arreter()
}
