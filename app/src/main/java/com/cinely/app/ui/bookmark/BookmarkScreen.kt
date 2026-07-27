package com.cinely.app.ui.bookmark

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cinely.app.data.Repository
import com.cinely.app.ui.components.AppTopBar
import com.cinely.app.ui.components.MediaCardData
import com.cinely.app.ui.components.mediaCarouselSection
import com.cinely.app.ui.components.toBookmarkCardData
import com.cinely.app.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(repository: Repository, onOpenItem: (String, Int) -> Unit, onSeeAll: (String, List<MediaCardData>) -> Unit) {
    val followed by repository.observeFollowed().collectAsState(initial = emptyList())
    val watched by repository.observeWatched().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // 1. Séries marquées "entièrement vues" (seasonNumber == null)
    val fullyWatchedShowIds = remember(watched) {
        watched.filter { it.mediaType == "tv" && it.seasonNumber == null }
            .map { it.tmdbId }
            .toSet()
    }

    // 2. Épisodes vus au format "tmdbId_saison_episode"
    val watchedEpisodesSet = remember(watched) {
        watched.filter { it.mediaType == "tv" && it.seasonNumber != null && it.episodeNumber != null }
            .map { "${it.tmdbId}_${it.seasonNumber}_${it.episodeNumber}" }
            .toSet()
    }

    // 2bis. Nombre d'épisodes vus par série (pour la pastille "reste à voir" des posters).
    val watchedEpisodesCountByShow = remember(watched) {
        watched.filter { it.mediaType == "tv" && it.seasonNumber != null && it.episodeNumber != null }
            .groupingBy { it.tmdbId }
            .eachCount()
    }

    val movies = remember(followed) {
        followed.filter {
            it.mediaType == "movie" && DateUtils.isReleased(it.releaseDate)
        }.map { it.toBookmarkCardData() }
    }

    val tvToWatch = remember(followed, fullyWatchedShowIds, watchedEpisodesSet) {
        followed.filter { item ->
            if (item.mediaType != "tv") return@filter false

            // Si la série globale est cochée vue -> On masque direct !
            if (item.tmdbId in fullyWatchedShowIds) return@filter false

            // Dernier épisode "officiellement" diffusé selon TMDB
            val lastSeason = item.lastAiredSeasonNumber
            val lastEpisode = item.lastAiredEpisodeNumber

            // TMDB peut publier next_episode_to_air avec un léger retard après sa sortie
            // réelle : si sa date est déjà passée ou aujourd'hui, on le traite comme sorti.
            val nextIsActuallyReleased = item.nextAirDate != null &&
                    DateUtils.isWatchable(item.nextAirDate)

            val effectiveSeason = if (nextIsActuallyReleased) item.nextSeasonNumber else lastSeason
            val effectiveEpisode = if (nextIsActuallyReleased) item.nextEpisodeNumber else lastEpisode

            if (effectiveSeason != null && effectiveEpisode != null) {
                val isEffectiveEpisodeWatched =
                    "${item.tmdbId}_${effectiveSeason}_${effectiveEpisode}" in watchedEpisodesSet
                !isEffectiveEpisodeWatched
            } else if (lastSeason != null && lastEpisode != null) {
                val isLastEpisodeWatched = "${item.tmdbId}_${lastSeason}_${lastEpisode}" in watchedEpisodesSet
                !isLastEpisodeWatched
            } else {
                val hasWatchedEpisodes = watchedEpisodesSet.any { it.startsWith("${item.tmdbId}_") }
                !hasWatchedEpisodes
            }
        }
    }

    // Séries dans tvToWatch qu'on a déjà commencé à regarder (au moins 1 épisode vu)
    val inProgressSeries = remember(tvToWatch, watchedEpisodesSet, watchedEpisodesCountByShow) {
        tvToWatch.filter { item ->
            watchedEpisodesSet.any { it.startsWith("${item.tmdbId}_") }
        }.map { it.toBookmarkCardData(watchedEpisodesCountByShow[it.tmdbId] ?: 0) }
    }
    // Séries dans tvToWatch qu'on n'a jamais commencées
    val notStartedSeries = remember(tvToWatch, watchedEpisodesSet, watchedEpisodesCountByShow) {
        tvToWatch.filter { item ->
            watchedEpisodesSet.none { it.startsWith("${item.tmdbId}_") }
        }.map { it.toBookmarkCardData(watchedEpisodesCountByShow[it.tmdbId] ?: 0) }
    }

    val isEmpty = movies.isEmpty() && inProgressSeries.isEmpty() && notStartedSeries.isEmpty()

    // Refresh "au cas où" à l'ouverture, throttlé à 6h côté Repository comme Planning.
    LaunchedEffect(followed.map { it.tmdbId }) {
        followed.forEach { item ->
            repository.refreshFollowedIfStale(item)
        }
    }

    fun refreshAllManually() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                followed.forEach { item ->
                    repository.refreshFollowedIfStale(item, force = true)
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Éléments suivis",
                actions = {
                    IconButton(onClick = { refreshAllManually() }, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                        }
                    }
                }
            )
        }) { padding ->
        if (isEmpty) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Rien à voir pour l'instant.\nTout ce que tu suis est soit à venir, soit déjà vu.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = 95.dp
                ),
            ) {
                mediaCarouselSection(
                    repository = repository,
                    title = "Films",
                    items = movies.take(15),
                    emptyLabel = "Aucun film à voir.",
                    onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                    onSeeAllClick = { onSeeAll("Films", movies) },
                    sectionHorizontalPadding = 16.dp,
                )
                item { Spacer(modifier = Modifier.height(8.dp)) }
                mediaCarouselSection(
                    repository = repository,
                    title = "Séries en cours",
                    items = inProgressSeries.take(15),
                    emptyLabel = "Aucune série en cours à voir.",
                    onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                    onSeeAllClick = { onSeeAll("Séries en cours", inProgressSeries) },
                    sectionHorizontalPadding = 16.dp,
                )
                item { Spacer(modifier = Modifier.height(8.dp)) }
                mediaCarouselSection(
                    repository = repository,
                    title = "Séries non commencées",
                    items = notStartedSeries.take(15),
                    emptyLabel = "Aucune série non commencée à voir.",
                    onItemClick = { onOpenItem(it.mediaType, it.tmdbId) },
                    onSeeAllClick = { onSeeAll("Séries non commencées", notStartedSeries) },
                    sectionHorizontalPadding = 16.dp,
                )
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}