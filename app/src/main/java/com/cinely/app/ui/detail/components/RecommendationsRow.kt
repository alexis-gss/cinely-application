package com.cinely.app.ui.detail.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.dp
import com.cinely.app.data.Repository
import com.cinely.app.data.SearchResult
import com.cinely.app.ui.components.MediaCardData
import com.cinely.app.ui.components.mediaCarouselSection

/**
 * Mappe les SearchResult du detail vers MediaCardData et délègue
 * le rendu à mediaCarouselSection pour conserver le même carousel que sur AccountScreen.
 */
fun LazyListScope.recommendationsSection(
    repository: Repository,
    recommendations: List<SearchResult>,
    onSelectMedia: (String, Int) -> Unit
) {
    val items = recommendations.map { item ->
        MediaCardData(
            key = "${item.resolvedMediaType}_${item.id}",
            tmdbId = item.id,
            mediaType = item.resolvedMediaType,
            title = item.displayTitle,
            posterPath = item.posterPath
        )
    }

    mediaCarouselSection(
        repository = repository,
        title = "Recommandations",
        items = items,
        emptyLabel = "",
        onItemClick = { data ->
            onSelectMedia(data.mediaType, data.tmdbId)
        },
        sectionHorizontalPadding = 16.dp,
    )
}
