/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 *
 * Origine : dépôt Groma (https://github.com/pobsteta/groma), fichier android/app/src/main/kotlin/groma/app/gnss/ConfigRtk.kt,
 *           état au commit f12b644 (2026-10-16). Reprise provisoire (ADR 0003, mission 003)
 *           jusqu'à l'extraction en bibliothèque commune (ADR 0008).
 * Modifications : paquet ; fichier de préférences « surculus ».
 */
package io.github.pobsteta.surculus.gnss

import android.content.Context

enum class TransportRtk { BLUETOOTH, TCP }

/**
 * External RTK receiver and NTRIP caster. Sources : le brief « carte RTK » du 2026-10-14, § 6, et
 * `docs/terrain.md` (« Corrections RTK Centipède »), tous deux dans le dépôt Groma (Surculus n'en
 * a pas de copie) ; côté Surculus, ADR 0003.
 * [ntrip] = true: Surculus pulls RTCM from the caster and forwards it to the
 * receiver; false: the receiver gets its corrections by itself. Centipède by
 * default (Groma docs/terrain.md, « Corrections RTK Centipède »); the mount point
 * is the nearest base, picked with « Charger ».
 */
data class ConfigRtk(
    val actif: Boolean = false,
    val transport: TransportRtk = TransportRtk.BLUETOOTH,
    val appareilBt: String? = null,
    val appareilBtNom: String? = null,
    val hoteTcp: String = "192.168.4.1",
    val portTcp: Int = 2947,
    val ntrip: Boolean = true,
    val casterHote: String = "caster.centipede.fr",
    val casterPort: Int = 2101,
    val pointMontage: String = "",
    // Compte public de Centipède : voir la documentation du réseau, https://docs.centipede.fr/
    val utilisateur: String = "centipede",
    val motDePasse: String = "centipede",
    /** Length of « Mesurer le centre » (s), brief § 5.4. */
    val dureeCentreS: Int = 10,
) {
    companion object {
        private fun prefs(ctx: Context) = ctx.getSharedPreferences("surculus", Context.MODE_PRIVATE)

        fun lire(ctx: Context): ConfigRtk = prefs(ctx).let { p ->
            val d = ConfigRtk()
            ConfigRtk(
                actif = p.getBoolean("rtk_actif", d.actif),
                transport = runCatching { TransportRtk.valueOf(p.getString("rtk_transport", null)!!) }.getOrDefault(d.transport),
                appareilBt = p.getString("rtk_appareil", null),
                appareilBtNom = p.getString("rtk_appareil_nom", null),
                hoteTcp = p.getString("rtk_hote_tcp", d.hoteTcp)!!,
                portTcp = p.getInt("rtk_port_tcp", d.portTcp),
                ntrip = p.getBoolean("rtk_ntrip", d.ntrip),
                casterHote = p.getString("rtk_caster", d.casterHote)!!,
                casterPort = p.getInt("rtk_caster_port", d.casterPort),
                pointMontage = p.getString("rtk_point_montage", d.pointMontage)!!,
                utilisateur = p.getString("rtk_utilisateur", d.utilisateur)!!,
                motDePasse = p.getString("rtk_mot_de_passe", d.motDePasse)!!,
                dureeCentreS = p.getInt("rtk_duree_centre", d.dureeCentreS),
            )
        }

        fun memoriser(ctx: Context, c: ConfigRtk) = prefs(ctx).edit()
            .putBoolean("rtk_actif", c.actif).putString("rtk_transport", c.transport.name)
            .putString("rtk_appareil", c.appareilBt).putString("rtk_appareil_nom", c.appareilBtNom)
            .putString("rtk_hote_tcp", c.hoteTcp).putInt("rtk_port_tcp", c.portTcp)
            .putBoolean("rtk_ntrip", c.ntrip).putString("rtk_caster", c.casterHote).putInt("rtk_caster_port", c.casterPort)
            .putString("rtk_point_montage", c.pointMontage).putString("rtk_utilisateur", c.utilisateur)
            .putString("rtk_mot_de_passe", c.motDePasse).putInt("rtk_duree_centre", c.dureeCentreS)
            .apply()
    }
}
