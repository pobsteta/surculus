/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), fichier android/app/src/main/kotlin/groma/app/gnss/ServiceGnssRtk.kt,
 *           état au commit f12b644 (2026-10-16). Reprise provisoire (ADR 0003, mission 003)
 *           jusqu'à l'extraction en bibliothèque commune (ADR 0008).
 * Modifications : paquet ; imports ; horodatage, évaluation de l'âge du fix et des corrections.
 */
package io.github.pobsteta.surculus.gnss

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.pobsteta.surculus.R
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.LecteurNmea
import io.github.pobsteta.surculus.core.gnss.Ntrip
import io.github.pobsteta.surculus.core.gnss.EvaluationFix
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import io.github.pobsteta.surculus.core.gnss.RaisonRefus
import io.github.pobsteta.surculus.core.gnss.StatutNtrip
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Link state, for the badge and the diagnostic of the settings. */
data class EtatRtk(
    val enMarche: Boolean = false,
    val connecte: Boolean = false,
    val octetsRecus: Long = 0,
    val tramesRecues: Long = 0,
    val derniereTrame: String? = null,
    val rtcmEnvoye: Long = 0,
    val ggaEnvoye: Long = 0,
    val erreur: String? = null,
    val ntripStatut: StatutNtrip? = null,
    val ntripErreur: String? = null,
    /** Raison pour laquelle le dernier fix du récepteur est refusé (trop vieux…), sinon null (ADR 0003). */
    val fixRefuse: RaisonRefus? = null,
    /** Qualité annoncée par le récepteur quand elle a été rétrogradée (corrections trop vieilles), sinon null. */
    val qualiteRetrogradee: QualiteFix? = null,
)

/**
 * Foreground service (type connectedDevice) holding the receiver link while
 * the map is open or the phone is in a pocket (brief § 6). Three concurrent
 * flows, as in Marculus: NMEA in, RTCM from the caster to the receiver, GGA
 * back to the caster. Reconnects after a cut until it is stopped; the
 * notification shows the fix and offers « Libérer le récepteur ».
 */
class ServiceGnssRtk : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
        _etat.update { it.copy(erreur = e.message ?: e.toString()) }
    })
    private var travail: Job? = null

    /** Dernier fix reçu du récepteur, avant évaluation ; réévalué chaque seconde pour que le temps compte. */
    @Volatile private var fixBrut: FixGnss? = null

    /** Applique la politique de fraîcheur (ADR 0003) au dernier fix brut et publie le résultat. */
    private fun publier() {
        val brut = fixBrut
        val verdict = brut?.let { SourcePosition.politique.evaluer(it, System.currentTimeMillis()) }
        val retenu = (verdict as? EvaluationFix.Accepte)?.fix
        val ancien = _fix.value
        _fix.value = retenu
        _etat.update {
            it.copy(
                fixRefuse = (verdict as? EvaluationFix.Refuse)?.raison,
                qualiteRetrogradee = (verdict as? EvaluationFix.Accepte)?.qualiteAnnoncee,
            )
        }
        // Notification seulement quand le texte change (qualité ou centimètres).
        if (retenu == null) {
            // Fix refusé ou absent : la notification ne garde pas l'ancien texte.
            if (ancien != null) premierPlan(null)
        } else if (ancien == null || ancien.qualite != retenu.qualite || texte(ancien) != texte(retenu)) {
            premierPlan(retenu)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CANAL, getString(R.string.rtk_canal), NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ARRET) {
            arreterInterne()
            return START_NOT_STICKY
        }
        // startForeground first: a service started in the foreground that stops without it crashes.
        val premier = premierPlan(null)
        val config = ConfigRtk.lire(this)
        val transport = transport(this, config)
        if (transport == null || !premier) {
            _etat.value = EtatRtk(erreur = getString(R.string.rtk_config_incomplete))
            arreterInterne()
            return START_NOT_STICKY
        }
        _etat.value = EtatRtk(enMarche = true)
        travail?.cancel()
        travail = scope.launch {
            while (isActive) {
                runCatching { relier(transport, config) }
                    .onFailure { e -> _etat.update { it.copy(erreur = e.message ?: e.toString()) } }
                if (!isActive) break
                fixBrut = null
                _fix.value = null
                _etat.update { it.copy(connecte = false) }
                premierPlan(null)
                delay(RECONNEXION_MS)
            }
        }
        return START_NOT_STICKY
    }

    /** One connection to the receiver (and the caster), until either closes. */
    private suspend fun relier(transport: Transport, config: ConfigRtk) = kotlinx.coroutines.coroutineScope {
        val lecteur = LecteurNmea(horlogeMonotone = android.os.SystemClock::elapsedRealtime)
        val ntrip = if (config.ntrip && config.pointMontage.isNotBlank()) {
            ClientNtrip(config.casterHote, config.casterPort, config.pointMontage, config.utilisateur, config.motDePasse)
        } else null
        val enfants = mutableListOf<Job>()
        // Un récepteur qui se tait ne laisse pas un vieux fix affiché : réévaluation chaque seconde.
        enfants += launch { while (isActive) { delay(1000L); publier() } }
        if (ntrip != null) {
            // A caster failure (unreachable, 401) never cuts the receiver link: it is reported only.
            enfants += launch(Dispatchers.IO) {
                while (isActive) {
                    runCatching {
                        ntrip.corrections { s -> _etat.update { it.copy(ntripStatut = s, ntripErreur = null) } }.collect { rtcm ->
                            transport.ecrire(rtcm)
                            _etat.update { it.copy(rtcmEnvoye = it.rtcmEnvoye + rtcm.size) }
                        }
                    }.onFailure { e -> _etat.update { it.copy(ntripErreur = e.message ?: e.toString()) } }
                    delay(RECONNEXION_MS * 2)
                }
            }
            // GGA to the caster as soon as connected (receiver's own, else the phone's last
            // position), fast until the first one, then every 10 s.
            enfants += launch {
                var amorce = false
                while (isActive) {
                    if (ntrip.connecte) {
                        val gga = lecteur.derniereGga ?: positionTelephone()?.let { Ntrip.gga(it, heureUtc()) }
                        if (gga != null) {
                            val n = ntrip.envoyerGga(gga)
                            if (n > 0) { amorce = true; _etat.update { it.copy(ggaEnvoye = it.ggaEnvoye + n) } }
                        }
                    }
                    delay(if (amorce) 10_000L else 500L)
                }
            }
        }
        try {
            transport.lire().collect { octets ->
                val (trames, fixes) = lecteur.pousser(octets.toString(Charsets.US_ASCII))
                _etat.update {
                    it.copy(connecte = true, erreur = null, octetsRecus = it.octetsRecus + octets.size,
                        tramesRecues = it.tramesRecues + trames.size, derniereTrame = trames.lastOrNull() ?: it.derniereTrame)
                }
                fixes.lastOrNull()?.let { f ->
                    fixBrut = f
                    publier()
                }
            }
        } finally {
            enfants.forEach { it.cancel() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun positionTelephone(): Position? = runCatching {
        val lm = getSystemService(LocationManager::class.java)
        (lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
            ?.let { Position(it.latitude, it.longitude) }
    }.getOrNull()

    private fun texte(f: FixGnss?): String = f?.let { libelleFix(it) } ?: getString(R.string.rtk_connexion)

    /** Foreground notification; false if the system refuses it. */
    private fun premierPlan(fix: FixGnss?): Boolean {
        val liberer = PendingIntent.getService(
            this, 0, Intent(this, ServiceGnssRtk::class.java).setAction(ACTION_ARRET),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif: Notification = NotificationCompat.Builder(this, CANAL)
            .setContentTitle(getString(R.string.rtk_notif_titre))
            .setContentText(texte(fix))
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.rtk_liberer), liberer)
            .build()
        return runCatching {
            ServiceCompat.startForeground(this, ID_NOTIF, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        }.isSuccess
    }

    private fun arreterInterne() {
        travail?.cancel()
        fixBrut = null
        _fix.value = null
        // Stopped after running: clean state; refused at start: keep the reason for the settings.
        _etat.update { if (it.enMarche) EtatRtk() else it }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        fixBrut = null
        _fix.value = null
        _etat.update { it.copy(enMarche = false, connecte = false) }
        super.onDestroy()
    }

    companion object {
        private const val CANAL = "gnss_rtk"
        private const val ID_NOTIF = 1001
        private const val RECONNEXION_MS = 2000L
        private const val ACTION_ARRET = "io.github.pobsteta.surculus.gnss.LIBERER"

        private val _fix = MutableStateFlow<FixGnss?>(null)

        /** Dernier fix du récepteur **après** la politique de fraîcheur ; null si arrêté, coupé, sans position ou trop vieux. */
        val fix: StateFlow<FixGnss?> = _fix.asStateFlow()

        private val _etat = MutableStateFlow(EtatRtk())
        val etat: StateFlow<EtatRtk> = _etat.asStateFlow()

        /** Starts (or restarts with the current settings) the receiver link. */
        fun demarrer(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, ServiceGnssRtk::class.java))
        }

        /** Releases the receiver (for QField, or on leaving the project). */
        fun arreter(ctx: Context) {
            if (_etat.value.enMarche) ctx.startService(Intent(ctx, ServiceGnssRtk::class.java).setAction(ACTION_ARRET))
        }

        @SuppressLint("MissingPermission")
        private fun transport(ctx: Context, c: ConfigRtk): Transport? = when (c.transport) {
            TransportRtk.BLUETOOTH -> {
                val adresse = c.appareilBt ?: return null
                val adaptateur = ctx.getSystemService(BluetoothManager::class.java)?.adapter
                runCatching { adaptateur?.getRemoteDevice(adresse) }.getOrNull()?.let(::TransportBluetooth)
            }
            TransportRtk.TCP -> if (c.hoteTcp.isBlank()) null else TransportTcp(c.hoteTcp, c.portTcp)
        }

        private fun heureUtc(): String = LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HHmmss.00", Locale.ROOT))
    }
}

/** "RTK fixe · 0,02 m", as in the notification and the badge. */
fun libelleFix(f: FixGnss): String =
    f.qualite.libelle + (f.precisionHorizontaleM?.let { " · " + String.format(Locale.FRANCE, "%.2f m", it) } ?: "")
