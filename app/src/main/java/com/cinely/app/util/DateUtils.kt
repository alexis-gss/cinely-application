package com.cinely.app.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * TMDB ne fournit qu'une DATE de sortie (pas d'heure précise, les chaînes/plateformes
 * ne la communiquent pas de façon fiable et standardisée). On affiche donc le jour
 * français, sans horaire inventé.
 */
object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val frenchZone: ZoneId = ZoneId.of("Europe/Paris")

    fun formatFrenchDay(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "Date inconnue"
        return try {
            val date = LocalDate.parse(isoDate, isoFormatter)
            val today = LocalDate.now(frenchZone)
            when {
                date.isEqual(today) -> "Aujourd'hui"
                date.isEqual(today.plusDays(1)) -> "Demain"
                else -> {
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.FRENCH)
                        .replaceFirstChar { it.uppercase() }
                    val monthName = date.month
                        .getDisplayName(TextStyle.FULL, Locale.FRENCH)
                    if (date.year == today.year) {
                        "$dayName ${date.dayOfMonth} $monthName"
                    } else {
                        "$dayName ${date.dayOfMonth} $monthName ${date.year}"
                    }
                }
            }
        } catch (e: Exception) {
            isoDate
        }
    }

    /**
     * Libellé court du nombre de jours restants avant la sortie, affiché à droite
     * de chaque ligne du Planning (le jour complet est lui affiché une seule fois
     * en en-tête de groupe par PlanningScreen).
     */
    fun daysRemainingLabel(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "—"
        return try {
            val date = LocalDate.parse(isoDate, isoFormatter)
            val today = LocalDate.now(frenchZone)
            val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
            when {
                days < 0 -> "Sorti"
                days == 0L -> "Aujourd'hui"
                days == 1L -> "J-1"
                else -> "J-$days"
            }
        } catch (e: Exception) {
            "—"
        }
    }

    /**
     * Check if element is already released.
     */
    fun isReleased(isoDate: String?): Boolean {
        if (isoDate.isNullOrBlank()) return false
        return try {
            val date = LocalDate.parse(isoDate, isoFormatter)
            date.isBefore(LocalDate.now(frenchZone))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if an episode can be marked as watched: already aired or airing today.
     * Unlike isReleased (strictly before today), this includes the air date itself.
     */
    fun isWatchable(isoDate: String?): Boolean {
        if (isoDate.isNullOrBlank()) return false
        return try {
            val date = LocalDate.parse(isoDate, isoFormatter)
            !date.isAfter(LocalDate.now(frenchZone))
        } catch (e: Exception) {
            false
        }
    }
}