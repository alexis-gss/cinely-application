package com.cinely.app.data

import com.cinely.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

/** Une image de galerie, avec une URL miniature (grille) et une URL qualité originale (visionneuse plein écran). */
data class GalleryImage(
    val path: String,
    val thumbnailUrl: String,
    val fullQualityUrl: String
)

/** Résultat groupé posters + bannières pour l'onglet Médias. */
data class MediaGallery(
    val posters: List<GalleryImage> = emptyList(),
    val backdrops: List<GalleryImage> = emptyList()
) {
    val isEmpty: Boolean get() = posters.isEmpty() && backdrops.isEmpty()
}

/** Sérialise une liste de genres TMDB vers le format CSV stocké sur WatchedItem.genres. */
fun List<Genre>.toGenresCsv(): String? = joinToString(",") { it.name }.ifBlank { null }

class Repository(
    private val api: TmdbApi,
    private val db: AppDatabase,
    val imageBaseUrl: String = "https://image.tmdb.org/t/p/w342"
) {

    // Test d'une clé spécifique sans la sauvegarder au préalable
    suspend fun testApiKey(keyToTest: String) {
        api.testApiKeyDirect(keyToTest)
    }

    suspend fun testApiKey() {
        api.getConfiguration()
    }

    // ----- Recherche -----
    suspend fun search(query: String) = api.searchMulti(query)
        .results
        .filter { it.resolvedMediaType == "movie" || it.resolvedMediaType == "tv" }

    // ----- Populaire / Tendances -----
    suspend fun getPopular() = api.getTrending()
        .results
        .filter { it.resolvedMediaType == "movie" || it.resolvedMediaType == "tv" }

    // ----- Détails -----
    suspend fun getMovieDetail(id: Int) = api.getMovieDetail(id)
    suspend fun getTvDetail(id: Int) = api.getTvDetail(id)
    suspend fun getSeasonDetail(tvId: Int, seasonNumber: Int) = api.getSeasonDetail(tvId, seasonNumber)

    fun posterUrl(path: String?): String? = path?.let { "$imageBaseUrl$it" }

    // ----- Cinely (planning) -----
    fun observeFollowed(): Flow<List<FollowedItem>> = db.followedItemDao().observeAll()

    suspend fun isFollowed(tmdbId: Int, mediaType: String): Boolean =
        db.followedItemDao().find(tmdbId, mediaType) != null

    suspend fun follow(item: FollowedItem) = db.followedItemDao().upsert(item)

    suspend fun unfollow(tmdbId: Int, mediaType: String) = db.followedItemDao().remove(tmdbId, mediaType)

    /**
     * Rafraîchit les infos de la série suivie (prochain épisode, statut) si les données
     * sont périmées. Deux cas de figure pour la durée de péremption :
     *  - Si on n'a pas encore de date de prochain épisode et que la série n'est ni
     *    terminée ni annulée, on retente plus souvent (30 min) car TMDB peut publier
     *    next_episode_to_air avec un léger délai après la diffusion précédente.
     *  - Sinon, on utilise le throttle normal (6h) pour ne pas spammer l'API.
     *
     * @param force si true, ignore complètement le throttle et force l'appel réseau
     *              (utilisé par le refresh manuel du Planning).
     */
    suspend fun refreshFollowedIfStale(
        item: FollowedItem,
        staleAfterMillis: Long = 6 * 60 * 60 * 1000L,
        force: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!force) {
            val effectiveStale = if (item.mediaType == "tv" && item.nextAirDate == null &&
                item.status != "Ended" && item.status != "Canceled"
            ) {
                30 * 60 * 1000L
            } else {
                staleAfterMillis
            }
            if (now - item.lastCheckedAt < effectiveStale) return
        }

        if (item.mediaType == "tv") {
            val detail = api.getTvDetail(item.tmdbId)
            val next = detail.nextEpisodeToAir
            val last = detail.lastEpisodeToAir
            val airedEpisodes = if (last?.seasonNumber != null && last.episodeNumber != null) {
                detail.seasons
                    .filter { it.seasonNumber in 1 until last.seasonNumber }
                    .sumOf { it.episodeCount } + last.episodeNumber
            } else {
                detail.seasons
                    .filter { DateUtils.isReleased(it.airDate) || DateUtils.isWatchable(it.airDate) }
                    .sumOf { it.episodeCount }
            }
            db.followedItemDao().upsert(
                item.copy(
                    title = detail.name,
                    nextAirDate = next?.airDate,
                    nextEpisodeName = next?.name,
                    nextSeasonNumber = next?.seasonNumber,
                    nextEpisodeNumber = next?.episodeNumber,
                    status = detail.status,
                    lastAiredSeasonNumber = last?.seasonNumber,
                    lastAiredEpisodeNumber = last?.episodeNumber,
                    airedEpisodesCount = airedEpisodes.takeIf { it > 0 },
                    lastCheckedAt = now
                )
            )
        } else if (item.mediaType == "movie") {
            val detail = api.getMovieDetail(item.tmdbId)
            db.followedItemDao().upsert(
                item.copy(
                    title = detail.title,
                    releaseDate = detail.releaseDate,
                    // Même mapping qu'au moment du follow (DetailScreen) : nextAirDate est le
                    // champ lu par PlanningScreen, il doit rester synchronisé avec releaseDate
                    // pour un film sous peine de le faire disparaître du Planning au prochain
                    // rafraîchissement.
                    nextAirDate = detail.releaseDate,
                    lastCheckedAt = now
                )
            )
        }
    }

    // ----- Favoris -----
    fun observeFavorites(): Flow<List<FavoriteItem>> = db.favoriteItemDao().observeAll()

    suspend fun isFavorite(tmdbId: Int, mediaType: String): Boolean =
        db.favoriteItemDao().find(tmdbId, mediaType) != null

    suspend fun addFavorite(item: FavoriteItem) = db.favoriteItemDao().upsert(item)

    suspend fun removeFavorite(tmdbId: Int, mediaType: String) = db.favoriteItemDao().remove(tmdbId, mediaType)

    // ----- Vus -----

    fun observeWatched(): Flow<List<WatchedItem>> = db.watchedItemDao().observeAll()

    /**
     * Check if element is watched.
     */
    suspend fun isWatched(tmdbId: Int, mediaType: String, seasonNumber: Int?, episodeNumber: Int?): Boolean =
        db.watchedItemDao().countMatching(tmdbId, mediaType, seasonNumber, episodeNumber) > 0

    /**
     * Mark element watched.
     */
    suspend fun markWatched(item: WatchedItem) = db.watchedItemDao().insert(item)

    /**
     * Unmark element watched.
     */
    suspend fun unmarkWatched(tmdbId: Int, mediaType: String, seasonNumber: Int?, episodeNumber: Int?) =
        db.watchedItemDao().deleteMatching(tmdbId, mediaType, seasonNumber, episodeNumber)

    suspend fun markSeasonWatched(
        tmdbId: Int,
        title: String,
        posterPath: String?,
        seasonNumber: Int,
        episodes: List<EpisodeInfo>,
        genres: List<Genre> = emptyList()
    ) {
        val genresCsv = genres.toGenresCsv()
        episodes.forEach { ep ->
            val epNum = ep.episodeNumber ?: return@forEach
            if (!isWatched(tmdbId, "tv", seasonNumber, epNum)) {
                markWatched(
                    WatchedItem(
                        tmdbId = tmdbId, mediaType = "tv", title = title, posterPath = posterPath,
                        seasonNumber = seasonNumber, episodeNumber = epNum, episodeName = ep.name,
                        watchedAt = System.currentTimeMillis(), genres = genresCsv, durationMinutes = ep.runtime
                    )
                )
            }
        }
    }

    suspend fun unmarkSeasonWatched(tmdbId: Int, seasonNumber: Int, episodes: List<EpisodeInfo>) {
        episodes.forEach { ep ->
            val epNum = ep.episodeNumber ?: return@forEach
            unmarkWatched(tmdbId, "tv", seasonNumber, epNum)
        }
    }

    // ----- Galerie média (onglet "Médias") -----
    // Chargée uniquement au clic sur l'onglet Médias (voir MovieDetailContent/TvDetailContent),
    // jamais lors du chargement initial de la fiche, pour ne pas alourdir le premier appel.

    /** Galerie (posters + bannières) d'un film, en qualité originale. */
    suspend fun getMovieGallery(tmdbId: Int, currentPosterPath: String?, currentBackdropPath: String?): MediaGallery =
        api.getMovieImages(tmdbId).toGallery(currentPosterPath, currentBackdropPath)

    /** Galerie (posters + bannières) d'une série, en qualité originale. */
    suspend fun getTvGallery(tmdbId: Int, currentPosterPath: String?, currentBackdropPath: String?): MediaGallery =
        api.getTvImages(tmdbId).toGallery(currentPosterPath, currentBackdropPath)

    /** Affiches disponibles pour une saison précise d'une série. */
    suspend fun getSeasonPosters(tmdbId: Int, seasonNumber: Int, currentPosterPath: String?): List<GalleryImage> =
        api.getSeasonImages(tmdbId, seasonNumber).posters
            .map { it.filePath }
            .let { reorderWithCurrentFirst(it, currentPosterPath) }
            .map { it.toGalleryImage() }

    private fun ImagesResponse.toGallery(currentPoster: String?, currentBackdrop: String?): MediaGallery = MediaGallery(
        posters = reorderWithCurrentFirst(posters.map { it.filePath }, currentPoster).map { it.toGalleryImage() },
        backdrops = reorderWithCurrentFirst(backdrops.map { it.filePath }, currentBackdrop).map { it.toGalleryImage() }
    )

    private fun String.toGalleryImage(): GalleryImage = GalleryImage(
        path = this,
        thumbnailUrl = "$TMDB_IMAGE_CDN/w500$this",
        fullQualityUrl = "$TMDB_IMAGE_CDN/original$this"
    )

    private fun reorderWithCurrentFirst(paths: List<String>, current: String?): List<String> {
        if (current == null) return paths
        val distinct = paths.toMutableList()
        distinct.remove(current)
        distinct.add(0, current)
        return distinct
    }

    /**
     * Export compressed file.
     */
    suspend fun exportSnapshot(): ExportBundle = ExportBundle(
        followed = db.followedItemDao().getAllOnce(),
        watched = db.watchedItemDao().getAllOnce()
    )

    /**
     * Import compressed file.
     */
    suspend fun importSnapshot(bundle: ExportBundle, replaceExisting: Boolean) {
        if (replaceExisting) {
            db.watchedItemDao().clearAllWatchedItems()
        }
        db.followedItemDao().upsertAll(bundle.followed)
        db.watchedItemDao().insertAll(bundle.watched)
    }

    suspend fun clearAllData() {
        db.followedItemDao().clearAllFollowedItems()
        db.watchedItemDao().clearAllWatchedItems()
        db.favoriteItemDao().clearAllFavoriteItems()
    }

    companion object {
        private const val TMDB_IMAGE_CDN = "https://image.tmdb.org/t/p"
    }
}