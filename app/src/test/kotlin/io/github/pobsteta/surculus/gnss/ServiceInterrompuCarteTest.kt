/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.gnss

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mission 082 : la carte reconnaît la phrase que le service pose quand il s'interrompt, et dit la même (celle de l'Essai
 * GNSS). La phrase vit dans deux ressources (service et carte) : ce test les garde identiques.
 */
class ServiceInterrompuCarteTest {
    private fun phrase(fichier: String, nom: String): String {
        val dossier = File("src/main/res/values").takeIf { it.isDirectory } ?: File("app/src/main/res/values")
        return Regex("<string name=\"$nom\">(.*?)</string>").find(File(dossier, fichier).readText())!!.groupValues[1].replace("\\'", "'")
    }

    @Test fun `la carte dit la phrase du service, mot pour mot`() {
        assertEquals(phrase("strings.xml", "rtk_service_interrompu"), phrase("strings_tournees.xml", "carte_recepteur_service_interrompu"))
    }

    @Test fun `le debut reconnu par la carte est celui de la phrase du service`() {
        val p = phrase("strings.xml", "rtk_service_interrompu")
        assertTrue(p.startsWith(ErreurRecepteurCarte.DEBUT_SERVICE_INTERROMPU), "« $p » ne commence plus par le début reconnu")
    }

    @Test fun `service interrompu seulement si le service est en marche`() {
        val p = ErreurRecepteurCarte.DEBUT_SERVICE_INTERROMPU + " : x"
        assertEquals(ErreurRecepteurCarte.SERVICE_INTERROMPU, ErreurRecepteurCarte.pour(true, true, p, null))
        assertEquals(ErreurRecepteurCarte.NON_DEMARRE, ErreurRecepteurCarte.pour(true, false, p, null))
    }
}
