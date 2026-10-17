package io.github.pobsteta.surculus.data.journal.db

/** Horloge pilotée par le test : heure du téléphone, horloge monotone et démarrage réglables. */
class HorlogeFixe(
    var utcMs: Long = 1_760_000_000_000L,
    var monoMs: Long = 1_000_000L,
    var boot: Long = 1,
) : SourceHorloge {
    override fun maintenantUtcMs() = utcMs
    override fun monotoneMs() = monoMs
    override fun demarrage() = boot

    /** Fait passer `ms` millisecondes : l'heure et l'horloge monotone avancent ensemble. */
    fun avancer(ms: Long) {
        utcMs += ms
        monoMs += ms
    }
}
