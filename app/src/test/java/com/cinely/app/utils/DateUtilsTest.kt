package com.cinely.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `formatFrenchDay renvoie une date inconnue si l'entree est nulle ou vide`() {
        assertEquals("Date inconnue", DateUtils.formatFrenchDay(null))
        assertEquals("Date inconnue", DateUtils.formatFrenchDay(""))
    }

    @Test
    fun `formatFrenchDay renvoie la date brute en entree si le format ISO est invalide`() {
        assertEquals("pas une date", DateUtils.formatFrenchDay("pas une date"))
    }

    @Test
    fun `formatFrenchDay renvoie une date lointaine passee sans decalage de jour`() {
        // 2020-01-03 est un vendredi (vérifié indépendamment, hors plage Aujourd'hui/Demain)
        val result = DateUtils.formatFrenchDay("2020-01-03")
        assertEquals("Vendredi 3 janvier 2020", result)
    }

    @Test
    fun `formatFrenchDay renvoie une date lointaine future sans decalage de jour`() {
        // 2030-08-07 est un mercredi (vérifié indépendamment, hors plage Aujourd'hui/Demain)
        val result = DateUtils.formatFrenchDay("2030-08-07")
        assertEquals("Mercredi 7 août 2030", result)
    }

    @Test
    fun `formatFrenchDay omet l'annee si la date est dans l'annee courante`() {
        val currentYear = LocalDate.now().year
        // 25 décembre de l'année en cours, en évitant la plage Aujourd'hui/Demain
        val result = DateUtils.formatFrenchDay("$currentYear-12-25")
        assertTrue(
            "attendu sans année pour l'année courante, obtenu: $result",
            !result.contains(currentYear.toString())
        )
    }

    @Test
    fun `daysRemainingLabel renvoie J moins n jours pour une date future lointaine`() {
        val farFuture = LocalDate.now().plusDays(10).toString()
        assertEquals("J-10", DateUtils.daysRemainingLabel(farFuture))
    }

    @Test
    fun `daysRemainingLabel renvoie Sorti pour une date passee`() {
        val past = LocalDate.now().minusDays(5).toString()
        assertEquals("Sorti", DateUtils.daysRemainingLabel(past))
    }

    @Test
    fun `daysRemainingLabel renvoie un tiret si l'entree est invalide`() {
        assertEquals("—", DateUtils.daysRemainingLabel(null))
        assertEquals("—", DateUtils.daysRemainingLabel("invalide"))
    }

    @Test
    fun `isReleased est vrai uniquement strictement avant aujourd'hui`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        assertTrue(DateUtils.isReleased(yesterday))
        assertFalse(DateUtils.isReleased(today))
        assertFalse(DateUtils.isReleased(tomorrow))
    }

    @Test
    fun `isWatchable est vrai le jour meme et avant, faux apres`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        assertTrue(DateUtils.isWatchable(yesterday))
        assertTrue(DateUtils.isWatchable(today))
        assertFalse(DateUtils.isWatchable(tomorrow))
    }
}