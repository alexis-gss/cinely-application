package com.cinely.app.ui.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cinely.app.data.GalleryImage
import com.cinely.app.data.MediaGallery
import com.cinely.app.data.Repository
import com.cinely.app.data.TvDetail
import com.cinely.app.ui.components.PosterGalleryDialog
import com.cinely.app.ui.detail.components.ActionButtons
import com.cinely.app.ui.detail.components.CastAndCrewSection
import com.cinely.app.ui.detail.components.DetailHeader
import com.cinely.app.ui.detail.components.MediaTabContent
import com.cinely.app.ui.detail.components.StreamingProvidersRow
import com.cinely.app.ui.detail.components.SynopsisSection
import com.cinely.app.ui.detail.components.recommendationsSection
import com.cinely.app.ui.detail.components.seasonsSection

private val TV_TABS = listOf("Détails", "Saisons", "Médias")

@Composable
fun TvDetailContent(
    repository: Repository,
    tvShow: TvDetail,
    isFollowed: Boolean = false,
    onFollowToggle: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {},
    onSelectMedia: (mediaType: String, tmdbId: Int) -> Unit = { _, _ -> },
    listState: LazyListState = rememberLazyListState(),
) {
    var selectedTabIndex by remember(tvShow.id) { mutableIntStateOf(0) }

    var isGalleryLoading by remember(tvShow.id) { mutableStateOf(false) }
    var gallery by remember(tvShow.id) { mutableStateOf<MediaGallery?>(null) }
    var hasFetchedGallery by remember(tvShow.id) { mutableStateOf(false) }

    LaunchedEffect(tvShow.id, selectedTabIndex) {
        if (selectedTabIndex == 2 && !hasFetchedGallery) {
            isGalleryLoading = true
            gallery = repository.getTvGallery(tvShow.id, tvShow.posterPath, tvShow.backdropPath)
            isGalleryLoading = false
            hasFetchedGallery = true
        }
    }

    var galleryViewerImages by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var galleryViewerIndex by remember { mutableIntStateOf(0) } // évite l'autoboxing (lint: AutoboxingStateCreation)
    if (galleryViewerImages.isNotEmpty()) {
        PosterGalleryDialog(
            posters = galleryViewerImages.map { it.fullQualityUrl },
            initialIndex = galleryViewerIndex,
            onDismiss = { galleryViewerImages = emptyList() }
        )
    }

    val watchedEpisodes by repository.observeWatched().collectAsState(initial = emptyList())
    val watchedForThisShow = remember(watchedEpisodes, tvShow.id) {
        watchedEpisodes.filter { it.tmdbId == tvShow.id && it.mediaType == "tv" }
    }

    val trailer = remember(tvShow.id) {
        tvShow.videos?.results?.firstOrNull { it.site == "YouTube" && it.type == "Trailer" }
            ?: tvShow.videos?.results?.firstOrNull { it.site == "YouTube" }
    }

    val statusLabel = remember(tvShow.status) {
        when (tvShow.status) {
            "Returning Series" -> "En cours"
            "Ended" -> "Terminée"
            "Canceled" -> "Annulée"
            "In Production" -> "En production"
            "Planned" -> "Prévue"
            "Pilot" -> "Pilote"
            else -> tvShow.status
        }
    }

    val infoLine = remember(tvShow.id, statusLabel) {
        listOfNotNull(
            tvShow.firstAirDate?.take(4),
            tvShow.numberOfSeasons?.takeIf { it > 0 }?.let { "$it saison" + if (it > 1) "s" else "" },
            tvShow.numberOfEpisodes?.takeIf { it > 0 }?.let { "$it épisodes" },
            statusLabel,
        ).joinToString(" · ")
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            DetailHeader(
                backdropPath = tvShow.backdropPath,
                posterPath = tvShow.posterPath,
                title = tvShow.name,
                tagline = tvShow.tagline,
                voteAverage = tvShow.voteAverage,
                infoLine = infoLine.ifBlank { null },
                genres = tvShow.genres.map { it.name }
            )
        }

        item {
            ActionButtons(
                isFollowed = isFollowed,
                followDisabled = false,
                onFollowToggle = onFollowToggle,
                isFavorite = isFavorite,
                onFavoriteToggle = onFavoriteToggle,
                favoriteDisabled = false,
                trailer = trailer,
            )
        }

        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                TV_TABS.forEachIndexed { index, tabLabel ->
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
                        overview = tvShow.overview,
                    )
                }
                item {
                    CastAndCrewSection(
                        cast = tvShow.credits?.cast.orEmpty(),
                        crew = tvShow.credits?.crew.orEmpty()
                    )
                }
                val providers = tvShow.watchProviders?.results?.get("FR")?.flatrate.orEmpty()
                if (providers.isNotEmpty()) {
                    item {
                        StreamingProvidersRow(
                            providers = providers,
                            posterUrlProvider = { path -> repository.posterUrl(path) }
                        )
                    }
                }
                val recommendations = tvShow.recommendations?.results.orEmpty()
                if (recommendations.isNotEmpty()) {
                    recommendationsSection(
                        repository = repository,
                        recommendations = recommendations,
                        onSelectMedia = onSelectMedia
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
                seasonsSection(
                    repository = repository,
                    tvId = tvShow.id,
                    tvTitle = tvShow.name,
                    seasons = tvShow.seasons,
                    watchedEpisodes = watchedForThisShow,
                    tvGenres = tvShow.genres,
                )
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            2 -> {
                item {
                    MediaTabContent(
                        videos = tvShow.videos?.results.orEmpty(),
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