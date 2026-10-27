/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.sauvegarde

import android.content.Context
import android.os.SystemClock
import io.github.pobsteta.surculus.core.session.EcouteSauvegarde
import io.github.pobsteta.surculus.data.sauvegarde.ServiceSauvegarde
import io.github.pobsteta.surculus.data.sauvegarde.SuiviDeclenchement
import io.github.pobsteta.surculus.data.session.DeclencheurSauvegarde
import io.github.pobsteta.surculus.data.session.JournalDeTournee
import io.github.pobsteta.surculus.data.session.HorlogeSessionAppareil
import io.github.pobsteta.surculus.data.journal.db.HorlogeAppareil
import io.github.pobsteta.surculus.tournee.LigneTournee
import java.util.concurrent.Executors

/**
 * Branche la session de contrôle (mission 012-A) sur la sauvegarde hors de l'appareil (spec § 4.8) : sans écran. Les
 * écrans de la 012-B l'appellent à l'ouverture d'une placette.
 *
 * La sauvegarde est écrite par un fil de fond unique, jamais par le fil principal. Limite connue : sur les API 26 à 28,
 * `WRITE_EXTERNAL_STORAGE` n'est pas encore demandée à l'exécution (voir [StockageMediaStore]) ; l'écriture échoue
 * alors, la session n'en souffre pas et la sauvegarde suivante réessaie.
 */
object SauvegardeApp {
    private val fond = Executors.newSingleThreadExecutor { r -> Thread(r, "sauvegarde-surculus").apply { isDaemon = true } }

    /** Journal de la tournée [ligne] ouverte, pour la session. */
    fun journal(context: Context, ligne: LigneTournee): JournalDeTournee = JournalDeTournee(JournalApp.depot(context), ligne.id)

    /** Horloges de l'appareil pour la session ; [dernierFix] rend le dernier fix GNSS connu, `null` sinon. */
    fun horloge(context: Context, dernierFix: () -> io.github.pobsteta.surculus.data.journal.db.FixGnss?) =
        HorlogeSessionAppareil(HorlogeAppareil(context), dernierFix)

    /**
     * Écoute de la sauvegarde de la tournée [ligne] : chaque ligne écrite est une saisie, « terminer » une fin de
     * placette. La version de sommieR de l'en-tête est `projet.version_sommier`, `null` si la tournée n'en porte pas.
     */
    fun ecoute(context: Context, ligne: LigneTournee): EcouteSauvegarde {
        val depot = JournalApp.depot(context)
        val service = ServiceSauvegarde(
            depot, StockageMediaStore(context), JournalApp.identifiantAppareil(context),
            ligne.tournee?.projet?.versionSommier?.takeIf { it.isNotBlank() },
        )
        return DeclencheurSauvegarde(
            suivi = SuiviDeclenchement(SystemClock.elapsedRealtime()),
            monotoneMs = SystemClock::elapsedRealtime,
            sauvegarder = { service.sauvegarder(ligne.id, System.currentTimeMillis()) },
            executeur = { fond.execute(it) },
        )
    }
}
