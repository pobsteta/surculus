/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), fichier android/app/src/main/kotlin/groma/app/gnss/SourcePosition.kt,
 *           état au commit f12b644 (2026-10-16). Reprise provisoire (ADR 0003, mission 003)
 *           jusqu'à l'extraction en bibliothèque commune (ADR 0008).
 * Modifications : paquet ; imports ; détection de la position simulée ; horodatage.
 */
package io.github.pobsteta.surculus.gnss

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import io.github.pobsteta.surculus.core.gnss.FixGnss
import io.github.pobsteta.surculus.core.gnss.EvaluationFix
import io.github.pobsteta.surculus.core.gnss.OrigineFix
import io.github.pobsteta.surculus.core.gnss.PolitiqueFix
import io.github.pobsteta.surculus.core.gnss.Position
import io.github.pobsteta.surculus.core.gnss.QualiteFix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * The one position of the app (brief § 6): the RTK receiver's fix when it has
 * one, else the phone's own GNSS (flagged by [OrigineFix.TELEPHONE]).
 */
object SourcePosition {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val telephone = MutableStateFlow<FixGnss?>(null)

    /** Règles de fraîcheur de l'ADR 0003 (fix trop vieux refusé, corrections trop vieilles rétrogradées). */
    val politique = PolitiqueFix()

    private val chaqueSeconde: Flow<Unit> = flow { while (true) { emit(Unit); delay(1000L) } }

    /**
     * Le fix du récepteur (déjà évalué par le service) sinon celui du téléphone, évalué ici ; un fix du
     * téléphone trop vieux disparaît, même si Android n'envoie plus rien.
     */
    val fix: StateFlow<FixGnss?> = combine(ServiceGnssRtk.fix, telephone, chaqueSeconde) { r, t, _ ->
        r ?: t?.let { (politique.evaluer(it, System.currentTimeMillis()) as? EvaluationFix.Accepte)?.fix }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    fun localisationAutorisee(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * Listens to the phone's GNSS every second (only while the map is shown,
     * no background location); close the result to stop. No-op without permission.
     */
    @SuppressLint("MissingPermission")
    fun ecouterTelephone(ctx: Context): AutoCloseable {
        val lm = ctx.getSystemService(LocationManager::class.java)
        if (lm == null || !localisationAutorisee(ctx)) return AutoCloseable {}
        val auditeur = object : LocationListener {
            override fun onLocationChanged(l: Location) { telephone.value = fixDepuis(l) }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) { telephone.value = null }
        }
        runCatching {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.takeIf { System.currentTimeMillis() - it.time < 60_000 }
                ?.let { telephone.value = fixDepuis(it) }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, auditeur, Looper.getMainLooper())
        }
        return AutoCloseable {
            runCatching { lm.removeUpdates(auditeur) }
            telephone.value = null
        }
    }

    /**
     * Rough position for sorting the projects by distance, without listening:
     * the current fix, else the phone's last known location (any provider,
     * less than a day old). Null without the location permission.
     */
    @SuppressLint("MissingPermission")
    fun positionApprochee(ctx: Context): Position? {
        fix.value?.let { return it.position }
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
        if (!localisationAutorisee(ctx)) return null
        return runCatching {
            lm.allProviders.mapNotNull { lm.getLastKnownLocation(it) }
                .filter { System.currentTimeMillis() - it.time < 24 * 3600_000L }
                .maxByOrNull { it.time }?.let { Position(it.latitude, it.longitude) }
        }.getOrNull()
    }

    /** Position simulée par une application tierce (`Location.isMock()` dès l'API 31, `isFromMockProvider` avant). */
    @Suppress("DEPRECATION")
    private fun estSimulee(l: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) l.isMock else l.isFromMockProvider

    private fun fixDepuis(l: Location): FixGnss {
        // Instant de la mesure par l'horloge monotone d'Android, ramené à l'horloge murale.
        val ageMs = (SystemClock.elapsedRealtime() - (l.elapsedRealtimeNanos / 1_000_000L)).coerceAtLeast(0L)
        val precision = if (l.hasAccuracy()) l.accuracy.toDouble() else null
        return FixGnss(
            position = Position(l.latitude, l.longitude),
            // The phone is never RTK, whatever its accuracy claims: autonomous at best.
            qualite = if (precision == null) QualiteFix.AUCUN else QualiteFix.AUTONOME,
            nbSatellites = l.extras?.getInt("satellites") ?: 0,
            hdop = null,
            altitudeM = if (l.hasAltitude()) l.altitude else null,
            ageCorrectionsS = null,
            precisionHorizontaleM = precision,
            horodatageMs = System.currentTimeMillis() - ageMs,
            capDeg = if (l.hasBearing()) l.bearing.toDouble() else null,
            vitesseMs = if (l.hasSpeed()) l.speed.toDouble() else null,
            origine = OrigineFix.TELEPHONE,
            simule = estSimulee(l),
        )
    }
}
