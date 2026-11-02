/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.messages

/**
 * Les phrases que le forestier lit quand une lecture, un import ou la dictée échoue (mission 082) : du français qui dit
 * quoi faire, jamais le message d'une exception. Le détail technique va au journal Android ([JournalTechnique]).
 * Elles suivent « Carte impossible : » et « Tournée refusée : », donc commencent par une minuscule.
 */
const val TEXTE_TOURNEE_ILLISIBLE = "cette tournée n'a pas pu être lue : l'importer de nouveau depuis sommieR"
const val TEXTE_LIGNE_ILLISIBLE = "tournée illisible : l'importer de nouveau depuis sommieR"
const val TEXTE_IMPORT_ECHOUE = "ce fichier n'a pas pu être importé : vérifier que c'est bien le travaux.gpkg d'une tournée de sommieR, puis réessayer"
const val TEXTE_DICTEE_NON_DEMARREE = "la dictée n'a pas pu démarrer : vérifier le micro et le modèle de voix dans Paramètres"
const val TEXTE_DICTEE_INTERROMPUE = "la dictée s'est interrompue : réessayer"

/** Journal technique : le détail d'une exception, pour le développeur, jamais pour l'écran. Sans effet dans les tests JVM. */
object JournalTechnique {
    fun avertir(etiquette: String, quoi: String, cause: Throwable) {
        runCatching { android.util.Log.w(etiquette, quoi, cause) }
    }
}
