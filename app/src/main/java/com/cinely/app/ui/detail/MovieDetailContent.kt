package com.cinely.app.ui.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cinely.app.data.GalleryImage
import com.cinely.app.data.MediaGallery
import com.cinely.app.data.MovieDetail
import com.cinely.app.data.Repository
import com.cinely.app.ui.components.PosterGalleryDialog
import com.cinely.app.ui.detail.components.ActionButtons
import com.cinely.app.ui.detail.components.CastAndCrewSection
import com.cinely.app.ui.detail.components.DetailHeader
import com.cinely.app.ui.detail.components.FinancialInfoSection
import com.cinely.app.ui.detail.components.MediaTabContent
import com.cinely.app.ui.detail.components.SynopsisSection
import com.cinely.app.ui.detail.components.StreamingProvidersRow
import com.cinely.app.ui.detail.components.recommendationsSection
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.cinely.app.util.DateUtils

private val MOVIE_TABS = listOf("Détails", "Médias")

@Composable
fun MovieDetailContent(
    repository: Repository,
    movie: MovieDetail,
    isFollowed: Boolean = false,
    onFollowToggle: () -> Unit = {},
    isWatched: Boolean = false,
    onWatchedToggle: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {},
    onSelectMedia: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> },
    listState: LazyListState = rememberLazyListState(),
) {
    var selectedTabIndex by remember(movie.id) { mutableIntStateOf(0) }

    // Galerie (posters/bannières HD) chargée à la demande, uniquement au premier passage
    // sur l'onglet Médias — pas au chargement initial de la fiche.
    var isGalleryLoading by remember(movie.id) { mutableStateOf(false) }
    var gallery by remember(movie.id) { mutableStateOf<MediaGallery?>(null) }
    var hasFetchedGallery by remember(movie.id) { mutableStateOf(false) }

    val isMovieReleased = remember(movie.id) {
        DateUtils.isWatchable(movie.releaseDate)
    }

    LaunchedEffect(movie.id, selectedTabIndex) {
        if (selectedTabIndex == 1 && !hasFetchedGallery) {
            isGalleryLoading = true
            gallery = repository.getMovieGallery(movie.id, movie.posterPath, movie.backdropPath)
            isGalleryLoading = false
            hasFetchedGallery = true
        }
    }

    // Visionneuse plein écran, ouverte au clic sur une image de l'onglet Médias.
    var galleryViewerImages by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var galleryViewerIndex by remember { mutableIntStateOf(0) } // évite l'autoboxing (lint: AutoboxingStateCreation)
    if (galleryViewerImages.isNotEmpty()) {
        PosterGalleryDialog(
            posters = galleryViewerImages.map { it.fullQualityUrl },
            initialIndex = galleryViewerIndex,
            onDismiss = { galleryViewerImages = emptyList() }
        )
    }

    val trailer = remember(movie.id) {
        movie.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }
            ?: movie.videos?.results?.firstOrNull { it.site == "YouTube" }
    }

    val infoLine = remember(movie.id) {
        listOfNotNull(
            movie.releaseDate?.take(4),
            movie.runtime?.takeIf { it > 0 }?.let { "${it / 60}h${(it % 60).toString().padStart(2, '0')}" }
        ).joinToString(" · ")
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            DetailHeader(
                backdropPath = movie.backdropPath,
                posterPath = movie.posterPath,
                title = movie.title,
                tagline = movie.tagline,
                voteAverage = movie.voteAverage,
                infoLine = infoLine.ifBlank { null },
                genres = movie.genres.map { it.name }
            )
        }

        item {
            ActionButtons(
                isFollowed = isFollowed,
                followDisabled = isWatched || isFavorite,
                onFollowToggle = onFollowToggle,
                isWatched = isWatched,
                onWatchedToggle = onWatchedToggle,
                watchedDisabled = !isMovieReleased,
                isFavorite = isFavorite,
                onFavoriteToggle = onFavoriteToggle,
                favoriteDisabled = !isMovieReleased,
                trailer = trailer,
            )
        }

        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                MOVIE_TABS.forEachIndexed { index, tabLabel ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(text = tabLabel) }
                    )
                }
            }
        }

        when (selectedTabIndex) {
            0 -> {
                item {
                    SynopsisSection(
                        tagline = null,
                        overview = movie.overview,
                    )
                }
                item {
                    CastAndCrewSection(
                        cast = movie.credits?.cast.orEmpty(),
                        crew = movie.credits?.crew.orEmpty()
                    )
                }
                item {
                    FinancialInfoSection(
                        budget = movie.budget,
                        revenue = movie.revenue
                    )
                }
                val providers = movie.watchProviders?.results?.get("FR")?.flatrate.orEmpty()
                if (providers.isNotEmpty()) {
                    item {
                        StreamingProvidersRow(
                            providers = providers,
                            posterUrlProvider = { path -> repository.posterUrl(path) }
                        )
                    }
                }
                val recommendations = movie.recommendations?.results.orEmpty()
                if (recommendations.isNotEmpty()) {
                    recommendationsSection(
                        repository = repository,
                        recommendations = recommendations,
                        onSelectMedia = onSelectMedia,
                    )
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            1 -> {
                item {
                    MediaTabContent(
                        videos = movie.videos?.results.orEmpty(),
                        isLoadingImages = isGalleryLoading,
                        gallery = gallery,
                        onImageClick = { images, index ->
                            galleryViewerImages = images
                            galleryViewerIndex = index
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
