package com.cinely.app.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinely.app.data.Repository
import com.cinely.app.data.WatchedItem
import com.cinely.app.ui.components.SectionTitle
import com.cinely.app.ui.theme.CinelyColors
import com.cinely.app.ui.theme.CinelyExtendedColors
import com.cinely.app.ui.theme.StatNumberStyle
import com.cinely.app.ui.theme.heroBrush
import java.util.Calendar
import java.util.Locale

/**
 * Écran dédié aux statistiques détaillées de visionnage. Deux onglets (Films / Séries) pour
 * éviter de mélanger un film (1 visionnage) avec un épisode (1 visionnage) dans les mêmes
 * agrégats — sinon une série de 20 épisodes écrase totalement les films dans les stats.
 * Purement dérivé de watched_items déjà observé via Repository.observeWatched() : aucun
 * appel réseau, aucune requête supplémentaire.
 *
 * Disposition : une carte "hero" en dégradé doré avec le chiffre phare de l'onglet, une
 * grille de mini-cartes de stats rapides, puis des sections détaillées (série la plus
 * regardée, genres, temps de visionnage, activité récente, jours préférés) présentées en
 * cartes plutôt qu'en texte nu perdu dans la page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(repository: Repository, onBack: () -> Unit) {
    val watched by repository.observeWatched().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    val movies = remember(watched) { watched.filter { it.mediaType == "movie" } }
    val episodes = remember(watched) { watched.filter { it.mediaType == "tv" } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Statistiques", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (watched.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Rien à analyser pour l'instant.\nMarque un film ou un épisode comme vu depuis sa fiche.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            return@Scaffold
        }

        Column(Modifier
            .padding(padding)
            .fillMaxSize()) {
            StatsSegmentedTabs(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
            )

            AnimatedContent(
                targetState = selectedTab,
                label = "stats-tab",
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { tab ->
                val data = if (tab == 0) movies else episodes
                if (data.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (tab == 0) "Aucun film vu pour l'instant." else "Aucun épisode vu pour l'instant.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { HeroStatCard(data, isMovieTab = tab == 0) }
                        item { QuickStatsGrid(data) }
                        item { GenreBreakdownCard(data) }
                        item { WatchTimeCard(data) }
                        item { MonthlyActivityCard(data) }
                        item { WeekdayBreakdownCard(data) }
                    }
                }
            }
        }
    }
}

/** Sélecteur Films/Séries en pilule, cohérent avec le style de la nav flottante plutôt
 *  qu'un TabRow Material générique avec soulignement. */
@Composable
private fun StatsSegmentedTabs(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = CinelyColors.colors
    val labels = listOf("Films", "Séries")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(extended.chipSurface)
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) extended.badgeBackground else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) extended.badgeText else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Fond de carte harmonisé (surface + liseré fin), réutilisé par toutes les sections
 *  détaillées de cet écran pour remplacer le texte nu de l'ancienne version.
 *  Extension de Modifier (et non fonction top-level) pour permettre le chaînage fluide
 *  et suivre la convention Compose (lint: ModifierFactoryExtensionFunction). */
private fun Modifier.cardBackground(extended: CinelyExtendedColors, radius: Int = 16): Modifier = this
    .fillMaxWidth()
    .clip(RoundedCornerShape(radius.dp))
    .background(extended.cardSurface)
    .border(1.dp, extended.cardBorder, RoundedCornerShape(radius.dp))
    .padding(16.dp)

// ---------------- Carte "hero" : le chiffre phare de l'onglet en avant ----------------
// Films -> nombre total de films vus. Séries -> nombre d'épisodes vus, avec en complément
// le nombre de séries distinctes concernées et, si pertinent, le record de jours d'affilée
// avec au moins un visionnage (nouvelle info, dérivée des dates déjà stockées).

@Composable
private fun HeroStatCard(items: List<WatchedItem>, isMovieTab: Boolean) {
    val extended = CinelyColors.colors
    val distinctShows = remember(items, isMovieTab) {
        if (isMovieTab) 0 else items.map { it.tmdbId }.distinct().size
    }
    val streak = remember(items) { longestDailyStreak(items) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(extended.heroBrush)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = items.size.toString(),
                style = StatNumberStyle.copy(fontSize = 44.sp),
                color = extended.badgeText
            )
            Text(
                text = if (isMovieTab) "film(s) vu(s) au total" else "épisode(s) vu(s) au total",
                style = MaterialTheme.typography.titleSmall,
                color = extended.badgeText.copy(alpha = 0.85f)
            )
            if (!isMovieTab || streak >= 2) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isMovieTab) {
                        HeroChip(
                            icon = Icons.Filled.Tv,
                            text = "$distinctShows série(s)",
                            textColor = extended.badgeText
                        )
                    }
                    if (streak >= 2) {
                        HeroChip(
                            icon = Icons.Filled.LocalFireDepartment,
                            text = "$streak jours d'affilée (record)",
                            textColor = extended.badgeText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, textColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.12f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------- Grille de mini-stats rapides ----------------
// Nouvelles infos par rapport à l'ancienne version : moyenne mensuelle et meilleur mois,
// en plus du compteur "ce mois-ci" déjà présent sur Account.

private data class QuickStat(val value: String, val label: String)

@Composable
private fun QuickStatsGrid(items: List<WatchedItem>) {
    val thisMonth = remember(items) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        items.count {
            cal.timeInMillis = it.watchedAt
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }
    val byMonth = remember(items) {
        items.groupBy { w ->
            val c = Calendar.getInstance().apply { timeInMillis = w.watchedAt }
            c.get(Calendar.YEAR) * 12 + c.get(Calendar.MONTH)
        }
    }
    val average = remember(byMonth, items) {
        val monthsSpanned = byMonth.keys.size.coerceAtLeast(1)
        String.format(Locale.US, "%.1f", items.size.toFloat() / monthsSpanned) // lint: DefaultLocale
    }
    val bestMonthCount = remember(byMonth) {
        byMonth.maxOfOrNull { it.value.size } ?: 0
    }

    val stats = listOf(
        QuickStat(thisMonth.toString(), "Ce mois-ci"),
        QuickStat(average, "Moyenne / mois"),
        QuickStat(bestMonthCount.toString(), "Meilleur mois"),
    )

    Column {
        SectionTitle("En bref")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.forEach { stat ->
                QuickStatCard(stat, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickStatCard(stat: QuickStat, modifier: Modifier = Modifier) {
    val extended = CinelyColors.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(extended.cardSurface)
            .border(1.dp, extended.cardBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stat.value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stat.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------- Répartition par genre ----------------
// Un même film/épisode peut avoir plusieurs genres (CSV), donc un visionnage
// peut compter dans plusieurs barres ; c'est voulu, c'est une répartition
// d'intérêt, pas une partition stricte du volume regardé.

@Composable
private fun GenreBreakdownCard(items: List<WatchedItem>) {
    val extended = CinelyColors.colors
    val genreCounts = remember(items) {
        items.asSequence()
            .flatMap { it.genres?.split(",").orEmpty().asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)
    }

    Column {
        SectionTitle("Genres préférés")
        Box(modifier = Modifier.cardBackground(extended)) {
            if (genreCounts.isEmpty()) {
                Text(
                    "Pas encore de données de genre. Elles se rempliront au fur et à mesure que tu marques de nouveaux éléments comme vus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val max = genreCounts.first().value
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    genreCounts.forEachIndexed { index, entry ->
                        GenreBar(genre = entry.key, count = entry.value, max = max, rank = index)
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreBar(genre: String, count: Int, max: Int, rank: Int) {
    val extended = CinelyColors.colors
    val barColor = if (rank == 0) extended.badgeBackground
        else MaterialTheme.colorScheme.primary.copy(alpha = (0.85f - rank * 0.1f).coerceAtLeast(0.35f))
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = genre,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (count.toFloat() / max).coerceIn(0.05f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }
    }
}

// ---------------- Temps de visionnage estimé ----------------
// Basé sur WatchedItem.durationMinutes (runtime TMDB stocké à l'écriture, comme les genres).
// Les éléments marqués vus avant l'ajout de ce champ (ou dont TMDB n'a pas fourni de durée,
// ex. un épisode pas encore diffusé/documenté) n'ont pas de durée connue : on les compte
// à part plutôt que de fausser le total avec une estimation arbitraire.
// Nouvelle info : équivalent en jours non-stop, pour donner une idée plus parlante du total.

@Composable
private fun WatchTimeCard(items: List<WatchedItem>) {
    val (totalMinutes, missingCount) = remember(items) {
        val known = items.mapNotNull { it.durationMinutes }
        known.sum() to (items.size - known.size)
    }

    Column {
        SectionTitle("Temps de visionnage estimé")
        Column(modifier = Modifier.cardBackground(CinelyColors.colors)) {
            if (totalMinutes <= 0) {
                Text(
                    "Pas encore de durée connue. Elle se remplira au fur et à mesure que tu marques de nouveaux éléments comme vus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val totalHours = totalMinutes / 60
                val fullDays = totalHours / 24
                val minutesRemainder = totalMinutes % 60
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (totalHours > 0) "${totalHours}h" else "${minutesRemainder}min",
                        style = StatNumberStyle,
                    )
                    if (totalHours > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${minutesRemainder.toString().padStart(2, '0')}min",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                if (fullDays >= 1) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "soit environ $fullDays jour(s) non-stop",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (missingCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Durée inconnue pour $missingCount élément(s), non comptabilisé(s) ici.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------- Activité des 6 derniers mois ----------------

@Composable
private fun MonthlyActivityCard(items: List<WatchedItem>) {
    val extended = CinelyColors.colors
    val monthLabels = listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc")

    val last6Months = remember(items) {
        val now = Calendar.getInstance()
        val buckets = (5 downTo 0).map { offset ->
            val c = now.clone() as Calendar
            c.add(Calendar.MONTH, -offset)
            c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
        }

        val counters = IntArray(buckets.size)
        items.forEach { w ->
            val c = Calendar.getInstance().apply { timeInMillis = w.watchedAt }
            val key = c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
            val idx = buckets.indexOf(key)
            if (idx >= 0) counters[idx]++
        }

        buckets.mapIndexed { idx, pair -> monthLabels[pair.second] to counters[idx] }
    }

    Column {
        SectionTitle("Activité (6 derniers mois)")
        Box(modifier = Modifier.cardBackground(extended)) {
            val max = (last6Months.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                last6Months.forEachIndexed { idx, pair ->
                    val (label, count) = pair
                    val isLast = idx == last6Months.lastIndex
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(text = count.toString(), style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((90 * count.toFloat() / max).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isLast) extended.badgeBackground else MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.55f
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isLast) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Jours de la semaine préférés ----------------
// Nouvelle info : sur quel(s) jour(s) de la semaine tu regardes le plus, pour repérer
// tes habitudes (ex : "soirée série le dimanche").

@Composable
private fun WeekdayBreakdownCard(items: List<WatchedItem>) {
    val extended = CinelyColors.colors
    val dayLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    val counts = remember(items) {
        // Calendar.DAY_OF_WEEK : Dimanche=1 ... Samedi=7 -> on remappe vers Lundi=0..Dimanche=6
        val counters = IntArray(7)
        items.forEach { w ->
            val c = Calendar.getInstance().apply { timeInMillis = w.watchedAt }
            val dow = c.get(Calendar.DAY_OF_WEEK) // 1..7, dimanche=1
            val index = (dow + 5) % 7 // dimanche(1)->6, lundi(2)->0, ...
            counters[index]++
        }
        counters.toList()
    }

    Column {
        SectionTitle("Jours préférés")
        Box(modifier = Modifier.cardBackground(extended)) {
            val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
            val topIndex = counts.indices.maxByOrNull { counts[it] } ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                dayLabels.forEachIndexed { idx, label ->
                    val count = counts[idx]
                    val isTop = idx == topIndex && count > 0
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((90 * count.toFloat() / max).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (isTop) extended.badgeBackground else MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.45f
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isTop) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isTop) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Plus longue série de jours consécutifs avec au moins un visionnage (film ou épisode).
 * Purement dérivé des timestamps déjà en mémoire, pas de champ supplémentaire nécessaire.
 */
private fun longestDailyStreak(items: List<WatchedItem>): Int {
    if (items.isEmpty()) return 0
    val days = items.map { w ->
        val c = Calendar.getInstance().apply { timeInMillis = w.watchedAt }
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.timeInMillis
    }.distinct().sorted()

    var longest = 1
    var current = 1
    val dayMillis = 24 * 60 * 60 * 1000L
    for (i in 1 until days.size) {
        if (days[i] - days[i - 1] == dayMillis) {
            current += 1
            longest = maxOf(longest, current)
        } else {
            current = 1
        }
    }
    return longest
}
