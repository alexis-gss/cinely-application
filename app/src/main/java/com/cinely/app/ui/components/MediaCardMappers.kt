package com.cinely.app.ui.components

import com.cinely.app.data.FavoriteItem
import com.cinely.app.data.FollowedItem
import com.cinely.app.data.SearchResult
import com.cinely.app.data.WatchedItem

/**
 * Fonctions d'extension qui convertissent chaque modèle de données de l'app vers le
 * MediaCardData générique consommé par MediaItemRow / MediaCarouselRow. C'est le seul
 * endroit qui doit connaître à la fois les modèles métier (WatchedItem, FollowedItem,
 * SearchResult...) et le modèle d'affichage.
 */

/** Film vu (Account) — une ligne par film. `dateLabel` est déjà formatée par l'écran
 *  (SimpleDateFormat), pour ne pas dupliquer la logique de formatage de date ici. */
fun WatchedItem.toWatchedMovieCardData(dateLabel: String): MediaCardData = MediaCardData(
    key = "movie_$id",
    tmdbId = tmdbId,
    mediaType = mediaType,
    title = title,
    posterPath = posterPath,
    subtitle = "Film",
    trailingText = dateLabel
)

/**
 * Élément suivi affiché dans Bookmark ("à voir"). Pour une série, `watchedEpisodesCount`
 * (nombre d'épisodes déjà sortis et cochés vus, calculé par l'écran à partir de la base
 * locale) permet d'afficher une pastille "reste à voir" sur le poster — même style que la
 * pastille de date vue sur Account (trailingText -> badge en dégradé sur MediaCarouselRow).
 * Si le total d'épisodes sortis n'est pas encore connu (série pas encore rafraîchie côté
 * TMDB), aucune pastille n'est affichée plutôt que d'afficher un chiffre potentiellement faux.
 */
fun FollowedItem.toBookmarkCardData(watchedEpisodesCount: Int = 0): MediaCardData {
    val subtitle = when {
        mediaType == "movie" -> "Film · sorti le ${nextAirDate ?: "?"}"
        nextEpisodeNumber != null ->
            "Dernier épisode sorti : S$nextSeasonNumber E$nextEpisodeNumber" +
                    (nextEpisodeName?.let { " · $it" } ?: "")
        else -> status ?: "Série"
    }
    val remaining = airedEpisodesCount?.let { total -> (total - watchedEpisodesCount).coerceAtLeast(0) }
    val trailingText = remaining?.takeIf { mediaType == "tv" && it > 0 }?.let {
        if (it == 1) "1 ép. restant" else "$it ép. restants"
    }
    return MediaCardData(
        key = "${mediaType}_$tmdbId",
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        posterPath = posterPath,
        subtitle = subtitle,
        trailingText = trailingText
    )
}

/** Élément suivi affiché dans Planning (le chip "jours restants" est géré par l'écran
 *  via `trailingContent`, il ne fait pas partie du modèle d'affichage lui-même). */
fun FollowedItem.toPlanningCardData(): MediaCardData {
    val subtitle = when {
        mediaType == "movie" -> "Film"
        nextEpisodeNumber != null ->
            "S$nextSeasonNumber E$nextEpisodeNumber" + (nextEpisodeName?.let { " · $it" } ?: "")
        else -> status ?: "Série"
    }
    return MediaCardData(
        key = "${mediaType}_$tmdbId",
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        posterPath = posterPath,
        subtitle = subtitle
    )
}

/** Résultat de recherche. La note TMDB (quand disponible et non nulle) alimente le badge
 *  de note coloré affiché sur la carte — pas la peine d'afficher un badge "0.0" pour un
 *  titre qui n'a simplement pas encore de votes. */
fun SearchResult.toCardData(): MediaCardData = MediaCardData(
    key = "${resolvedMediaType}_$id",
    tmdbId = id,
    mediaType = resolvedMediaType,
    title = displayTitle,
    posterPath = posterPath,
    subtitle = listOfNotNull(
        if (resolvedMediaType == "movie") "Film" else "Série",
        year
    ).joinToString(" · "),
    rating = voteAverage?.takeIf { it > 0.0 }
)

/** Élément favori affiché dans Account. */
fun FavoriteItem.toFavoriteCardData(): MediaCardData = MediaCardData(
    key = "${mediaType}_$tmdbId",
    tmdbId = tmdbId,
    mediaType = mediaType,
    title = title,
    posterPath = posterPath,
    subtitle = if (mediaType == "movie") "Film" else "Série"
)