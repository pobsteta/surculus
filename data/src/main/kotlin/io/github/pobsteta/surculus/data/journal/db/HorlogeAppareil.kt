package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/**
 * Horloges réelles de l'appareil : heure du téléphone, `SystemClock.elapsedRealtime` (monotone, compte aussi
 * la veille) et compteur de démarrages du système (`Settings.Global.BOOT_COUNT`, API 24).
 */
class HorlogeAppareil(context: Context) : SourceHorloge {
    private val resolver = context.applicationContext.contentResolver

    override fun maintenantUtcMs(): Long = System.currentTimeMillis()

    override fun monotoneMs(): Long = SystemClock.elapsedRealtime()

    override fun demarrage(): Long = Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, 0).toLong()
}
