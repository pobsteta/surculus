/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.controle

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import io.github.pobsteta.surculus.core.dictee.EcouteurDictee
import io.github.pobsteta.surculus.dictee.ModeleVoskImport
import io.github.pobsteta.surculus.dictee.MoteurDicteeVosk
import java.util.concurrent.Executors

/**
 * L'écoute de la dictée, unique dans le processus comme [PocheApp] (mission 012-D2) : l'Activity, qui reçoit les touches, et
 * l'écran du contrôle s'y retrouvent. Le micro de Vosk est ouvert **tant que la touche est tenue** (volume bas en mode
 * poche, bouton micro hors du mode) ; la logique est dans [PilotageEcoute] (testée sans téléphone), ici seulement le câblage
 * Android : fil du micro, fil principal, permission, modèle.
 */
object EcouteApp {
    private val principal = Handler(Looper.getMainLooper())
    /** Un seul fil pour le moteur : charger, ouvrir et fermer s'enchaînent dans l'ordre, jamais sur le fil principal. */
    private val fil = Executors.newSingleThreadExecutor { r -> Thread(r, "ecoute-vosk").apply { isDaemon = true } }

    private var contexte: Context? = null
    private var moteur: MoteurDicteeVosk? = null

    /** Compteur lu par la bande pour se redessiner : l'état est tenu ici, hors de Compose, et traverse la rotation. */
    var version by mutableIntStateOf(0)
        private set

    /** Dernier état connu de la permission du micro (la fenêtre du système n'a pas d'API de lecture du refus définitif). */
    @Volatile var permissionRefusee: EtatPermissionMicro? = null

    @Volatile private var pilote: PilotageEcoute? = null

    private val tic = Runnable { pilote?.avancer() }

    private val micro = object : PortMicro {
        override fun ouvrir(generation: Int) {
            val ctx = contexte ?: return
            fil.execute {
                var echec = false
                val m = moteur ?: MoteurDicteeVosk(ModeleVoskImport.dossier(ctx)).also { moteur = it }
                m.demarrer(object : EcouteurDictee {
                    override fun surPhrase(phrase: String) { principal.post { pilote?.phrase(phrase) } }
                    override fun surErreur(message: String) { echec = true; principal.post { pilote?.erreur() } }
                })
                if (!echec) principal.post { pilote?.moteurPret(generation) }
            }
        }

        override fun fermer() { fil.execute { moteur?.arreter() } }
    }

    /** Posé par l'Activity au démarrage, après [PocheApp.installer]. */
    fun installer(ctx: Context) {
        if (pilote != null) return
        contexte = ctx.applicationContext
        val sorties = PocheApp.sorties ?: return
        pilote = PilotageEcoute(
            micro = micro,
            sorties = sorties,
            horloge = { SystemClock.uptimeMillis() },
            modeleInstalle = { ModeleVoskImport.estInstalle(ctx.applicationContext) },
            permission = { etatPermission(ctx.applicationContext) },
        ).also { p ->
            p.surChangement = {
                version++
                principal.removeCallbacks(tic)
                p.echeance()?.let { principal.postDelayed(tic, (it - SystemClock.uptimeMillis()).coerceAtLeast(0)) }
            }
        }
    }

    /**
     * Pose un pilote construit par un test de rendu (mission 080), horloge à lui ; `null` l'ôte. Câblé comme [installer] : un
     * changement redessine la bande. Aucun effet en production, personne ne l'appelle.
     */
    internal fun poserPilotePourTest(p: PilotageEcoute?) {
        pilote = p
        p?.surChangement = { version++ }
        version++
    }

    /** Lu à chaque appel : l'agent peut accorder le micro dans les réglages entre deux appuis. */
    fun etatPermission(ctx: Context): EtatPermissionMicro {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) return EtatPermissionMicro.ACCORDEE
        return permissionRefusee ?: EtatPermissionMicro.A_DEMANDER
    }

    fun modeleInstalle(ctx: Context): Boolean = ModeleVoskImport.estInstalle(ctx.applicationContext)

    /** Les lignes de la bande (lues par l'écran). */
    val affichage: AffichageEcoute get() = pilote?.affichage ?: AffichageEcoute.RIEN
    val entendu: String? get() = pilote?.entendu
    val compris: String? get() = pilote?.compris
    val comprisErreur: Boolean get() = pilote?.comprisErreur ?: false

    /** Le bandeau est-il montré ? Pendant l'écoute, puis quelques secondes ([DUREE_BANDEAU_MS]) ; lit [version] pour se redessiner. */
    fun bandeauVisible(): Boolean { version; return pilote?.bandeauVisible() ?: false }

    /** L'écran du contrôle prend (ou rend) les phrases. Sans écran, `tenir` ne fait rien. */
    fun brancher(surPhrase: ((String) -> Unit)?) { pilote?.surPhrase = surPhrase }

    fun tenir(): Boolean = pilote?.tenir() ?: false
    fun relacher() { pilote?.relacher() }
    fun resultat(annonce: AnnoncePoche) { pilote?.resultat(annonce) }
    fun effacerLignes() { pilote?.effacerLignes() }
    fun arreter() { pilote?.arreter() }
    fun arreterEtOublier() { pilote?.arreterEtOublier() }
}
