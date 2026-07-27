package com.cinely.app.ui.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinely.app.data.EpisodeInfo
import com.cinely.app.data.Repository
import com.cinely.app.data.SeasonSummary
import com.cinely.app.data.WatchedItem
import com.cinely.app.data.toGenresCsv
import com.cinely.app.ui.components.PosterImage
import com.cinely.app.util.DateUtils
import kotlinx.coroutines.launch

/**
 * Section "Saisons" : un accordéon par saison. Les épisodes ne sont récupérés
 * (repository.getSeasonDetail) qu'au premier dépliage de la saison, jamais au chargement
 * de la fiche. Le statut "vu" par épisode est réactif : il vient de watchedEpisodes,
 * observé en direct depuis la base locale par l'écran parent (TvDetailContent), donc
 * chaque coche se met à jour immédiatement sans re-fetch réseau.
 */
fun LazyListScope.seasonsSection(
    repository: Repository,
    tvId: Int,
    tvTitle: String,
    seasons: List<SeasonSummary>,
    watchedEpisodes: List<WatchedItem>,
    tvGenres: List<com.cinely.app.data.Genre> = emptyList()
) {
    val visibleSeasons = seasons.filter { it.episodeCount > 0 }

    if (visibleSeasons.isEmpty()) {
        item {
            Text(
                text = "Aucune saison disponible.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    items(visibleSeasons, key = { it.seasonNumber }) { season ->
        val watchedInSeason = remember(watchedEpisodes, season.seasonNumber) {
            watchedEpisodes.filter { it.seasonNumber == season.seasonNumber }.mapNotNull { it.episodeNumber }.toSet()
        }
        SeasonCard(
            repository = repository,
            tvId = tvId,
            tvTitle = tvTitle,
            season = season,
            watchedEpisodeNumbers = watchedInSeason,
            tvGenres = tvGenres
        )
    }
}

@Composable
private fun SeasonCard(
    repository: Repository,
    tvId: Int,
    tvTitle: String,
    season: SeasonSummary,
    watchedEpisodeNumbers: Set<Int>,
    tvGenres: List<com.cinely.app.data.Genre>
) {
    var expanded by remember(season.seasonNumber) { mutableStateOf(false) }
    var isLoading by remember(season.seasonNumber) { mutableStateOf(false) }
    var episodes by remember(season.seasonNumber) { mutableStateOf<List<EpisodeInfo>>(emptyList()) }
    var hasFetched by remember(season.seasonNumber) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(season.seasonNumber, expanded) {
        if (expanded && !hasFetched) {
            isLoading = true
            episodes = repository.getSeasonDetail(tvId, season.seasonNumber).episodes
            isLoading = false
            hasFetched = true
        }
    }

    val watchedCount = watchedEpisodeNumbers.size
    val allWatched = season.episodeCount > 0 && watchedCount >= season.episodeCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                PosterImage(
                    model = season.posterPath?.let { "https://image.tmdb.org/t/p/w185$it" },
                    contentDescription = season.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 52.dp, height = 78.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = season.name, style = MaterialTheme.typography.titleMedium)
                        if (allWatched) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Saison vue",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = if (watchedCount > 0) "${season.episodeCount} épisodes · $watchedCount vus"
                        else "${season.episodeCount} épisodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Réduire" else "Développer"
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        if (isLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            episodes.forEach { episode ->
                                val epNum = episode.episodeNumber
                                val isWatched = epNum != null && watchedEpisodeNumbers.contains(epNum)
                                val isWatchable = DateUtils.isWatchable(episode.airDate)
                                EpisodeRow(
                                    episode = episode,
                                    isWatched = isWatched,
                                    isWatchable = isWatchable,
                                    onToggleWatched = {
                                        if (epNum != null) {
                                            scope.launch {
                                                if (isWatched) {
                                                    repository.unmarkWatched(tvId, "tv", season.seasonNumber, epNum)
                                                } else {
                                                    repository.markWatched(
                                                        WatchedItem(
                                                            tmdbId = tvId,
                                                            mediaType = "tv",
                                                            title = tvTitle,
                                                            posterPath = season.posterPath,
                                                            seasonNumber = season.seasonNumber,
                                                            episodeNumber = epNum,
                                                            episodeName = episode.name,
                                                            watchedAt = System.currentTimeMillis(),
                                                            genres = tvGenres.toGenresCsv(),
                                                            durationMinutes = episode.runtime
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            if (episodes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val releasedEpisodes = episodes.filter { DateUtils.isReleased(it.airDate) }
                                            if (allWatched) {
                                                repository.unmarkSeasonWatched(tvId, season.seasonNumber, releasedEpisodes)
                                            } else {
                                                repository.markSeasonWatched(tvId, tvTitle, season.posterPath, season.seasonNumber, releasedEpisodes, tvGenres)
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Text(if (allWatched) "Marquer la saison comme non vue" else "Marquer toute la saison comme vue")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeInfo,
    isWatched: Boolean,
    isWatchable: Boolean,
    onToggleWatched: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isWatchable) it.clickable { onToggleWatched() } else it }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber?.let { "$it. " } ?: ""}${episode.name ?: "Épisode"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            episode.airDate?.let {
                Text(
                    text = DateUtils.formatFrenchDay(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isWatchable) {
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
