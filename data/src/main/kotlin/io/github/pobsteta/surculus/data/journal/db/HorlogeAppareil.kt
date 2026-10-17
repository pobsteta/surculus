package io.github.pobsteta.surculus.data.journal.db

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

/**
 * Horloges réelles de l'appareil : heure du téléphone, `SystemClock.elapsedRealtime` (monotone, compte aussi
 * la veille) et numéro de démarrage.
 *
 * **Numéro de démarrage.** `Settings.Global.BOOT_COUNT` (API 24) quand le système le fournit. S'il est absent
 * (certains constructeurs), renvoyer une constante ferait comparer, après un redémarrage, deux relevés de
 * l'horloge monotone de deux démarrages différents : un **faux saut d'horloge** noté à chaque redémarrage.
 * Repli : un compteur propre à Surculus (préférences privées) qui augmente dès que l'horloge monotone **recule**
 * par rapport au dernier relevé, ce qui n'arrive que si l'appareil a redémarré, jamais lors d'un changement
 * d'heure du téléphone (elle ne dépend pas de l'heure civile). Limite : un redémarrage dont l'horloge monotone
 * dépasserait déjà le dernier relevé au premier appel n'est pas vu ; il ne produit alors au pire qu'un saut
 * manqué, pas un faux saut.
 */
class HorlogeAppareil(
    context: Context,
    private val monotone: () -> Long = SystemClock::elapsedRealtime,
) : SourceHorloge {
    private val appli = context.applicationContext
    private val prefs = appli.getSharedPreferences("horloge_appareil", Context.MODE_PRIVATE)

    override fun maintenantUtcMs(): Long = System.currentTimeMillis()

    override fun monotoneMs(): Long = monotone()

    @Synchronized
    override fun demarrage(): Long {
        try {
            return Settings.Global.getInt(appli.contentResolver, Settings.Global.BOOT_COUNT).toLong()
        } catch (_: Settings.SettingNotFoundException) {
            // repli ci-dessous
        }
        val maintenant = monotone()
        var compteur = prefs.getLong(CLE_COMPTEUR, 0L)
        if (maintenant < prefs.getLong(CLE_DERNIER, 0L)) compteur++
        // commit() : le relevé est durable avant que la saisie qui s'en sert soit écrite.
        prefs.edit().putLong(CLE_COMPTEUR, compteur).putLong(CLE_DERNIER, maintenant).commit()
        return compteur
    }

    private companion object {
        const val CLE_COMPTEUR = "compteur_demarrage"
        const val CLE_DERNIER = "dernier_monotone_ms"
    }
}
