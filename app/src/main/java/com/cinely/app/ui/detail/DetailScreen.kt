package com.cinely.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cinely.app.data.FavoriteItem
import com.cinely.app.data.FollowedItem
import com.cinely.app.data.Repository
import com.cinely.app.data.WatchedItem
import com.cinely.app.data.toGenresCsv
import com.cinely.app.ui.detail.components.DetailScreenState
import com.cinely.app.ui.detail.components.FloatingTopBar
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState

/**
 * Point d'entrée de la fiche détail (film ou série). Charge la fiche TMDB (un seul appel,
 * credits/videos/watch-providers/recommendations déjà inclus via append_to_response), puis
 * délègue l'affichage à MovieDetailContent / TvDetailContent selon `mediaType`. Les données
 * plus lourdes (galerie médias HD, épisodes d'une saison) ne sont chargées qu'au clic sur
 * l'onglet correspondant — voir Repository.getMovieGallery/getTvGallery/getSeasonDetail.
 */
@Composable
fun DetailScreen(
    repository: Repository,
    mediaType: String,
    tmdbId: Int,
    onBack: () -> Unit,
    onOpenItem: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> }
) {
    var state by remember(mediaType, tmdbId) { mutableStateOf<DetailScreenState>(DetailScreenState.Loading) }
    // Incrémenté pour forcer un rechargement manuel (bouton "Réessayer").
    var reloadKey by remember(mediaType, tmdbId) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaType, tmdbId, reloadKey) {
        state = DetailScreenState.Loading
        state = try {
            if (mediaType == "tv") {
                DetailScreenState.TvSuccess(repository.getTvDetail(tmdbId))
            } else {
                DetailScreenState.MovieSuccess(repository.getMovieDetail(tmdbId))
            }
        } catch (e: Exception) {
            DetailScreenState.Error(e.message ?: "Impossible de charger cette fiche.")
        }
    }

    // Suivi / favoris / vus : observés en direct depuis la base locale (comme partout
    // ailleurs dans l'app) pour que les boutons d'action réagissent immédiatement.
    val followedList by repository.observeFollowed().collectAsState(initial = emptyList())
    val favoriteList by repository.observeFavorites().collectAsState(initial = emptyList())
    val watchedList by repository.observeWatched().collectAsState(initial = emptyList())

    val isFollowed = followedList.any { it.tmdbId == tmdbId && it.mediaType == mediaType }
    val isFavorite = favoriteList.any { it.tmdbId == tmdbId && it.mediaType == mediaType }
    val isMovieWatched = mediaType == "movie" &&
            watchedList.any { it.tmdbId == tmdbId && it.mediaType == mediaType }

    val listState = rememberLazyListState()
    val currentTitle = when (val current = state) {
        is DetailScreenState.MovieSuccess -> current.movie.title
        is DetailScreenState.TvSuccess -> current.tvShow.name
        else -> null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is DetailScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is DetailScreenState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = current.message, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { reloadKey++ }) {
                        Text("Réessayer")
                    }
                }
            }

            is DetailScreenState.MovieSuccess -> {
                val movie = current.movie
                MovieDetailContent(
                    repository = repository,
                    movie = movie,
                    listState = listState,
                    isFollowed = isFollowed,
                    onFollowToggle = {
                        scope.launch {
                            if (isFollowed) {
                                repository.unfollow(tmdbId, mediaType)
                            } else {
                                repository.follow(
                                    FollowedItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = movie.title,
                                        posterPath = movie.posterPath,
                                        releaseDate = movie.releaseDate,
                                        nextAirDate = movie.releaseDate,
                                    )
                                )
                            }
                        }
                    },
                    isWatched = isMovieWatched,
                    onWatchedToggle = {
                        scope.launch {
                            if (isMovieWatched) {
                                repository.unmarkWatched(tmdbId, mediaType, null, null)
                            } else {
                                repository.markWatched(
                                    WatchedItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = movie.title,
                                        posterPath = movie.posterPath,
                                        watchedAt = System.currentTimeMillis(),
                                        genres = movie.genres.toGenresCsv(),
                                        durationMinutes = movie.runtime
                                    )
                                )
                                if (isFollowed) {
                                    repository.unfollow(tmdbId, mediaType)
                                }
                            }
                        }
                    },
                    isFavorite = isFavorite,
                    onFavoriteToggle = {
                        scope.launch {
                            if (isFavorite) {
                                repository.removeFavorite(tmdbId, mediaType)
                            } else {
                                repository.addFavorite(
                                    FavoriteItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = movie.title,
                                        posterPath = movie.posterPath,
                                        addedAt = System.currentTimeMillis()
                                    )
                                )
                                if (!isMovieWatched) {
                                    repository.markWatched(
                                        WatchedItem(
                                            tmdbId = tmdbId,
                                            mediaType = mediaType,
                                            title = movie.title,
                                            posterPath = movie.posterPath,
                                            watchedAt = System.currentTimeMillis(),
                                            genres = movie.genres.toGenresCsv(),
                                            durationMinutes = movie.runtime
                                        )
                                    )
                                }
                                if (isFollowed) {
                                    repository.unfollow(tmdbId, mediaType)
                                }
                            }
                        }
                    },
                    onSelectMedia = onOpenItem
                )
            }

            is DetailScreenState.TvSuccess -> {
                val tvShow = current.tvShow
                TvDetailContent(
                    repository = repository,
                    tvShow = tvShow,
                    listState = listState,
                    isFollowed = isFollowed,
                    onFollowToggle = {
                        scope.launch {
                            if (isFollowed) {
                                repository.unfollow(tmdbId, mediaType)
                            } else {
                                repository.follow(
                                    FollowedItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = tvShow.name,
                                        posterPath = tvShow.posterPath,
                                        nextAirDate = tvShow.nextEpisodeToAir?.airDate,
                                        nextEpisodeName = tvShow.nextEpisodeToAir?.name,
                                        nextSeasonNumber = tvShow.nextEpisodeToAir?.seasonNumber,
                                        nextEpisodeNumber = tvShow.nextEpisodeToAir?.episodeNumber,
                                        status = tvShow.status,
                                        lastAiredSeasonNumber = tvShow.lastEpisodeToAir?.seasonNumber,
                                        lastAiredEpisodeNumber = tvShow.lastEpisodeToAir?.episodeNumber,
                                        lastCheckedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    isFavorite = isFavorite,
                    onFavoriteToggle = {
                        scope.launch {
                            if (isFavorite) {
                                repository.removeFavorite(tmdbId, mediaType)
                            } else {
                                repository.addFavorite(
                                    FavoriteItem(
                                        tmdbId = tmdbId,
                                        mediaType = mediaType,
                                        title = tvShow.name,
                                        posterPath = tvShow.posterPath,
                                        addedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    onSelectMedia = onOpenItem
                )
            }
        }

        // Barre flottante : bouton retour + titre qui apparaît au scroll
        FloatingTopBar(
            listState = listState,
            currentTitle = currentTitle,
            onBack = onBack,
        )
    }
}
