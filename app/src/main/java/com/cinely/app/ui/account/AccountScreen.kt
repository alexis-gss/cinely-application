package com.cinely.app.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinely.app.data.Repository
import com.cinely.app.data.WatchedItem
import com.cinely.app.ui.components.AppTopBar
import com.cinely.app.ui.components.MediaCardData
import com.cinely.app.ui.components.SectionTitle
import com.cinely.app.ui.components.mediaCarouselSection
import com.cinely.app.ui.components.toFavoriteCardData
import com.cinely.app.ui.components.toWatchedMovieCardData
import com.cinely.app.ui.theme.CinelyColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    repository: Repository,
    onOpenItem: (String, Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onSeeAll: (String, List<MediaCardData>) -> Unit
) {
    val watched by repository.observeWatched().collectAsState(initial = emptyList())
    val favorites by repository.observeFavorites().collectAsState(initial = emptyList())
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE) }

    val favoriteItems = remember(favorites) {
        favorites.map { it.toFavoriteCardData() }
    }

    val movies = remember(watched) {
        watched.filter { it.mediaType == "movie" }
            .sortedByDescending { it.watchedAt }
            .map { it.toWatchedMovieCardData(dateFmt.format(Date(it.watchedAt))) }
    }

    // Résumé d'une série vue : une ligne par série (pas une par épisode), avec le nombre
    // d'épisodes cochés. Le regroupement reste ici car il est spécifique à cet écran ;
    // seul le résultat (MediaCardData) est partagé avec le reste de l'app.
    val tv = remember(watched) {
        watched.filter { it.mediaType == "tv" }
            .groupBy { it.tmdbId }
            .values
            .map { items -> items.maxBy { it.watchedAt } to items.size }
            .sortedByDescending { (latest, _) -> latest.watchedAt }
            .map { (latest, episodesWatched) ->
                MediaCardData(
                    key = "tv_${latest.tmdbId}",
                    tmdbId = latest.tmdbId,
                    mediaType = "tv",
                    title = latest.title,
                    posterPath = latest.posterPath,
                    subtitle = "$episodesWatched épisode(s) vu(s)",
                    trailingText = dateFmt.format(Date(latest.watchedAt))
                )
            }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Mon compte",
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Statistiques")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Paramètres")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            StatsSection(watched, onOpenStats = onOpenStats)
            if (movies.isEmpty() && tv.isEmpty() && favoriteItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Rien de coché pour l'instant.\nMarque un film ou un épisode comme vu depuis sa fiche.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        top = 0.dp,
                        end = 0.dp,
                        bottom = 95.dp
                    ),
                ) {
                    mediaCarouselSection(
                        repository = repository,
                        title = "Favoris",
                        items = favoriteItems.take(15),
                        emptyLabel = "Aucun favori pour l'instant.",
                        onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                        onSeeAllClick = { onSeeAll("Favoris", favoriteItems) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    mediaCarouselSection(
                        repository = repository,
                        title = "Films vus",
                        items = movies.take(15),
                        emptyLabel = "Aucun film vu pour l'instant.",
                        onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                        onSeeAllClick = { onSeeAll("Films vus", movies) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    mediaCarouselSection(
                        repository = repository,
                        title = "Séries vues",
                        items = tv.take(15),
                        emptyLabel = "Aucune série vue pour l'instant.",
                        onItemClick = { onOpenItem("tv", it.tmdbId) },
                        onSeeAllClick = { onSeeAll("Séries vues", tv) },
                        sectionHorizontalPadding = 16.dp,
                    )
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

// ---------------- Statistiques (spécifique à cet écran) ----------------
// Purement dérivées de la liste déjà observée en mémoire (watched_items) :
// aucun appel réseau ni requête supplémentaire, juste du filtrage/comptage.

private data class StatItem(val value: Int, val label: String, val accent: Boolean = false)

@Composable
private fun StatsSection(watched: List<WatchedItem>, onOpenStats: () -> Unit) {
    val movies = remember(watched) { watched.count { it.seasonNumber == null } }
    val episodes = remember(watched) { watched.count { it.seasonNumber != null } }
    val thisMonth = remember(watched) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        watched.count {
            cal.timeInMillis = it.watchedAt
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
    }

    val stats = listOf(
        StatItem(movies, "Films"),
        StatItem(episodes, "Épisodes"),
        StatItem(thisMonth, "Ce mois-ci", accent = true)
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Statistiques")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenStats),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            stats.forEach { stat ->
                StatCard(stat, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatCard(stat: StatItem, modifier: Modifier = Modifier) {
    val extended = CinelyColors.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (stat.accent) extended.badgeBackground else extended.cardSurface)
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stat.value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (stat.accent) extended.badgeText else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stat.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (stat.accent) extended.badgeText.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
