/*
 * Surculus — suivi des travaux de plantation en forêt.
 * Copyright (C) 2026 Pascal Obstétar
 *
 * Ce fichier est un logiciel libre : vous pouvez le redistribuer et/ou le modifier selon
 * les termes de la licence publique générale GNU (GPL), version 3 ou ultérieure (voir LICENSE).
 */
package io.github.pobsteta.surculus.core.dictee

/**
 * Échec d'installation du modèle dont le [message] est déjà une phrase française qui dit quoi faire : l'écran la montre
 * telle quelle (mission 081). Toute autre exception n'est jamais montrée : l'écran dit une phrase générique et le détail
 * va au journal technique.
 */
class EchecInstallation(message: String) : IllegalStateException(message)
