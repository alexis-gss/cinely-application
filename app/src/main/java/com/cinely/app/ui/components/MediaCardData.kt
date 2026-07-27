package com.cinely.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinely.app.data.Repository
import com.cinely.app.ui.theme.CinelyColors
import com.cinely.app.ui.theme.colorForRating
import java.util.Locale

/**
 * Modèle d'affichage générique pour un élément média (film ou série), indépendant de la
 * source de données (WatchedItem, FollowedItem, SearchResult, ...).
 *
 * Chaque écran ne connaît que ce type-là pour le rendu : il mappe son propre modèle de
 * données vers un MediaCardData via les fonctions de MediaCardMappers.kt, puis passe la
 * liste obtenue aux composants ci-dessous. Résultat : pour changer le style d'une ligne ou
 * d'un poster dans toute l'app (Account, Bookmark, Planning, Search...), il n'y a plus
 * qu'un seul fichier à modifier : celui-ci.
 */
data class MediaCardData(
    val key: String,
    val tmdbId: Int,
    val mediaType: String, // "movie" ou "tv"
    val title: String,
    val posterPath: String?,
    val subtitle: String? = null,
    val trailingText: String? = null,
    /** Note TMDB /10, quand elle est connue (ex : résultats de recherche/populaire).
     *  Affichée sous forme de badge coloré (vert/ambre/rouge) façon TV Time. */
    val rating: Double? = null
)

private val PosterShape = RoundedCornerShape(12.dp)
private val DefaultRowPosterSize: Pair<Dp, Dp> = 64.dp to 92.dp
private val DefaultCarouselPosterSize: Pair<Dp, Dp> = 130.dp to 192.dp

/**
 * Poster seul (image + coins arrondis). Brique de base réutilisée par MediaItemRow et
 * MediaCarouselRow, à modifier ici si tu changes le style des posters partout.
 */
@Composable
fun MediaPoster(
    repository: Repository,
    posterPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    PosterImage(
        model = repository.posterUrl(posterPath),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

/**
 * AFFICHAGE 1 — une ligne par élément : poster + titre + sous-titre, dans une card flottante
 * avec ombre douce et fin liseré (façon TV Time). `trailingContent` permet d'ajouter un slot
 * à droite (ex : l'AssistChip "jours restants" du Planning) ; s'il n'est pas fourni, la note
 * colorée (si connue) ou `trailingText` sont affichés à la place.
 *
 * Utilisé par Account, Bookmark, Planning et Search : toute évolution de style ici
 * s'applique automatiquement partout.
 */
@Composable
fun MediaItemRow(
    repository: Repository,
    item: MediaCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterSize: Pair<Dp, Dp> = DefaultRowPosterSize,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val extended = CinelyColors.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = PosterShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.25f))
            .clip(PosterShape)
            .background(extended.cardSurface)
            .border(1.dp, extended.cardBorder, PosterShape)
            .clickable(onClick = onClick)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            MediaPoster(
                repository = repository,
                posterPath = item.posterPath,
                contentDescription = item.title,
                modifier = Modifier
                    .size(posterSize.first, posterSize.second)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.subtitle?.let { subtitle ->
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item.rating?.let { rating ->
                RatingBadge(rating = rating)
            }
        }
        when {
            trailingContent != null -> {
                Spacer(Modifier.width(10.dp))
                trailingContent()
            }
            item.trailingText != null -> {
                Spacer(Modifier.width(10.dp))
                MediaBadge(text = item.trailingText)
            }
        }
    }
}

/**
 * Petit badge pilule (façon statut/date) réutilisé sur les lignes et posters de carousel.
 */
@Composable
fun MediaBadge(text: String, modifier: Modifier = Modifier) {
    val extended = CinelyColors.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(extended.badgeBackground)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = extended.badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Badge de note coloré façon TV Time : vert si bien noté, ambre si moyen, rouge si faible.
 * C'est volontairement une couleur à part (jamais l'accent doré de la marque) pour rester
 * un signal de lecture rapide, distinct du reste de l'UI.
 */
@Composable
fun RatingBadge(rating: Double) {
    val extended = CinelyColors.colors
    val color = extended.colorForRating(rating)
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            text = String.format(Locale.US, "%.1f", rating), // point décimal forcé (lint: DefaultLocale)
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

/**
 * AFFICHAGE 2 — carousel horizontal de posters façon TV Time : titre en overlay dégradé
 * en bas du poster, note colorée en haut à gauche, badge optionnel (trailingText) en haut
 * à droite, ombre portée marquée. Prêt à l'emploi pour une section compacte ("À la une",
 * recommandations, etc.) : il suffit de lui passer une List<MediaCardData> et un callback
 * de clic, où que ce soit dans l'app.
 */
@Composable
fun MediaCarouselRow(
    repository: Repository,
    items: List<MediaCardData>,
    onItemClick: (MediaCardData) -> Unit,
    modifier: Modifier = Modifier,
    posterSize: Pair<Dp, Dp> = DefaultCarouselPosterSize,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp),
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = contentPadding,
    ) {
        items(items, key = { it.key }) { item ->
            MediaPosterCard(
                repository = repository,
                item = item,
                posterSize = posterSize,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun MediaPosterCard(
    repository: Repository,
    item: MediaCardData,
    posterSize: Pair<Dp, Dp>,
    onClick: () -> Unit,
) {
    val extended = CinelyColors.colors
    Box(
        modifier = Modifier
            .size(posterSize.first, posterSize.second)
            .shadow(elevation = 10.dp, shape = PosterShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(PosterShape)
            .clickable(onClick = onClick)
    ) {
        MediaPoster(
            repository = repository,
            posterPath = item.posterPath,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize()
        )
        item.trailingText?.let { badge ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(extended.posterOverlayBottom, extended.posterOverlayTop)
                        )
                    )
            )
            MediaBadge(
                text = badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}

/**
 * Factorise le pattern "en-tête de section + liste, ou message si vide" répété dans
 * Account et Bookmark (ex : "Films vus" / "Aucun film vu pour l'instant."). Utilise
 * MediaItemRow pour chaque élément, donc bénéficie automatiquement de ses évolutions.
 */
fun LazyListScope.mediaRowSection(
    repository: Repository,
    title: String,
    items: List<MediaCardData>,
    emptyLabel: String,
    onItemClick: (MediaCardData) -> Unit,
    trailingContent: (@Composable (MediaCardData) -> Unit)? = null
) {
    item {
        SectionTitle(title = title)
    }
    if (items.isEmpty()) {
        item {
            EmptySectionHint(emptyLabel)
        }
    } else {
        items(items, key = { it.key }) { data ->
            MediaItemRow(
                repository = repository,
                item = data,
                onClick = { onItemClick(data) },
                trailingContent = trailingContent?.let { render -> { render(data) } }
            )
        }
    }
}

/**
 * Équivalent de [mediaRowSection] mais avec l'AFFICHAGE 2 : un en-tête de section suivi
 * d'un carousel horizontal de posters (ou du message vide). Même principe : un seul appel
 * dans le LazyColumn de l'écran, le style du carousel se modifie uniquement dans
 * [MediaCarouselRow].
 */
fun LazyListScope.mediaCarouselSection(
    repository: Repository,
    title: String,
    items: List<MediaCardData>,
    emptyLabel: String,
    onItemClick: (MediaCardData) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    posterSize: Pair<Dp, Dp> = DefaultCarouselPosterSize,
    sectionHorizontalPadding: Dp = 4.dp,
) {
    item {
        SectionTitle(
            title = title,
            modifier = Modifier.padding(horizontal = sectionHorizontalPadding),
            onSeeAllClick = onSeeAllClick,
        )
    }
    if (items.isEmpty()) {
        item {
            EmptySectionHint(emptyLabel, modifier = Modifier.padding(horizontal = sectionHorizontalPadding))
        }
    } else {
        item {
            MediaCarouselRow(
                repository = repository,
                items = items,
                onItemClick = onItemClick,
                posterSize = posterSize,
                contentPadding = PaddingValues(horizontal = sectionHorizontalPadding)
            )
        }
    }
}

/** Message "section vide" harmonisé, dans une carte discrète plutôt qu'un texte nu perdu
 *  dans la page (plus lisible visuellement, cohérent avec le reste des cartes de l'app). */
@Composable
private fun EmptySectionHint(text: String, modifier: Modifier = Modifier) {
    val extended = CinelyColors.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(extended.chipSurface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
