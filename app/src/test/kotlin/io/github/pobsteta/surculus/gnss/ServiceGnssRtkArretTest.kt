/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gnss

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Mission 052. Sans Android (ni Robolectric), le plus proche : le service **définit** `onTaskRemoved`. Ce test n'exécute
 * pas l'arrêt ; celui-ci se vérifie au téléphone (retirer la tâche : plus de notification du récepteur).
 */
class ServiceGnssRtkArretTest {
    @Test fun `le service gere le retrait de la tache`() {
        val methodes = ServiceGnssRtk::class.java.declaredMethods.map { it.name }
        assertTrue("onTaskRemoved" in methodes, "ServiceGnssRtk doit surcharger onTaskRemoved pour s'arrêter proprement")
    }
}
