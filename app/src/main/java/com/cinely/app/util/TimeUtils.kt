package com.cinely.app.util

/**
 * TMDB ne fournit qu'une DATE de sortie (pas d'heure précise, les chaînes/plateformes
 * ne la communiquent pas de façon fiable et standardisée). On affiche donc le jour
 * français, sans horaire inventé.
 */
object TimeUtils {
    /**
     * Formate une durée en minutes vers un format lisible "Xh XXmin" ou "XXmin".
     * Exemple : 135 -> "2h 15min", 45 -> "45min", 120 -> "2h"
     */
    fun formatRuntime(runtimeMinutes: Int?): String {
        if (runtimeMinutes == null || runtimeMinutes <= 0) return "Durée inconnue"

        val hours = runtimeMinutes / 60
        val minutes = runtimeMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}min"
            hours > 0 -> "${hours}h"
            else -> "${minutes}min"
        }
    }
}
