/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), fichier android/groma-core/src/main/kotlin/groma/core/Ntrip.kt,
 *           état au commit f12b644 (2026-10-16). Reprise provisoire (ADR 0003, mission 003)
 *           jusqu'à l'extraction en bibliothèque commune (ADR 0008).
 * Modifications : paquet ; agent NTRIP par défaut « Surculus ».
 */
package io.github.pobsteta.surculus.core.gnss

import java.util.Base64
import java.util.Locale
import kotlin.math.abs

/** `STR` entry of an NTRIP source table: a mount point offered by the caster. */
data class PointMontage(
    val nom: String,
    val identifiant: String,
    val format: String,
    /** Position de la base (champs 10 et 11 de l'entrée `STR`), degrés WGS 84 ; null si la table ne la donne pas. */
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/** Caster answer to an NTRIP request. */
enum class StatutNtrip { OK, TABLE_SOURCES, NON_AUTORISE, INCONNU }

/**
 * NTRIP client protocol, pure and tested (taken from Marculus, same author):
 * request, GGA sent back to the caster (VRS / NEAR mount points pick the
 * nearest base from it), source table and answer status. Network I/O lives
 * in the app.
 */
object Ntrip {
    /** NTRIP 2.0 `GET` request with Basic authentication, ended by an empty line. */
    fun requete(pointMontage: String, hote: String, port: Int, utilisateur: String, motDePasse: String, agent: String = "Surculus"): String {
        val mp = if (pointMontage.startsWith("/")) pointMontage else "/$pointMontage"
        val auth = Base64.getEncoder().encodeToString("$utilisateur:$motDePasse".toByteArray(Charsets.UTF_8))
        return "GET $mp HTTP/1.1\r\nHost: $hote:$port\r\nNtrip-Version: Ntrip/2.0\r\nUser-Agent: NTRIP $agent\r\n" +
            "Authorization: Basic $auth\r\n\r\n"
    }

    /** `$GPGGA` giving the rover position to the caster; [heureUtc] is `hhmmss.ss`. */
    fun gga(position: Position, heureUtc: String = "000000.00", qualite: Int = 1, nbSatellites: Int = 10, hdop: Double = 1.0, altitudeM: Double = 0.0): String {
        val (lat, ns) = versNmea(position.latitude, latitude = true)
        val (lon, ew) = versNmea(position.longitude, latitude = false)
        val corps = String.format(Locale.ROOT, "GPGGA,%s,%s,%s,%s,%s,%d,%02d,%.1f,%.1f,M,0.0,M,,", heureUtc, lat, ns, lon, ew, qualite, nbSatellites, hdop, altitudeM)
        return "\$$corps*${Nmea.sommeControle(corps)}"
    }

    /** Mount points (`STR;` lines) of a source table. */
    fun tableSources(texte: String): List<PointMontage> = texte.lineSequence().filter { it.startsWith("STR;") }.mapNotNull { l ->
        val f = l.split(';')
        if (f.size < 4) null else PointMontage(f[1], f[2], f[3], f.getOrNull(9)?.toDoubleOrNull(), f.getOrNull(10)?.toDoubleOrNull())
    }.toList()

    /** Status from the first line of the answer (ICY / HTTP 200, 401, source table). */
    fun statut(premiereLigne: String): StatutNtrip {
        val l = premiereLigne.trim()
        return when {
            l.contains("401") -> StatutNtrip.NON_AUTORISE
            l.startsWith("SOURCETABLE") -> StatutNtrip.TABLE_SOURCES
            l.contains("200") -> StatutNtrip.OK
            else -> StatutNtrip.INCONNU
        }
    }

    private fun versNmea(degres: Double, latitude: Boolean): Pair<String, String> {
        val hemisphere = if (latitude) (if (degres >= 0) "N" else "S") else (if (degres >= 0) "E" else "W")
        val a = abs(degres)
        val d = a.toInt()
        return String.format(Locale.ROOT, "%0${if (latitude) 2 else 3}d%07.4f", d, (a - d) * 60.0) to hemisphere
    }
}
