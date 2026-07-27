package com.cinely.app.ui.detail.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cinely.app.data.VideoItem

@Composable
fun ActionButtons(
    isFollowed: Boolean,
    followDisabled: Boolean = false,
    onFollowToggle: () -> Unit,
    isWatched: Boolean? = null,
    onWatchedToggle: (() -> Unit)? = null,
    watchedDisabled: Boolean = false,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    favoriteDisabled: Boolean = false,
    trailer: VideoItem? = null,
) {
    val context = LocalContext.current
    val minChipWidth = 85.dp // Largeur minimale garantie par bouton

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()) // Permet de défiler si l'écran est très étroit
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Bouton Suivi / Suivre
        FilterChip(
            selected = isFollowed,
            enabled = !followDisabled,
            onClick = onFollowToggle,
            modifier = Modifier
                .weight(1f) // Prend une part égale de la largeur
                .defaultMinSize(minWidth = minChipWidth), // Minimum de largeur
            shape = RoundedCornerShape(12.dp),
            label = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isFollowed) Icons.Filled.Bookmark else Icons.Filled.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isFollowed) "Suivi" else "Suivre")
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
        )

        // Bouton Vu / Non vu
        if (isWatched != null && onWatchedToggle != null) {
            FilterChip(
                selected = isWatched,
                enabled = !watchedDisabled,
                onClick = onWatchedToggle,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = minChipWidth),
                shape = RoundedCornerShape(12.dp),
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isWatched) "Vu" else "Non vu")
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
            )
        }

        // Bouton Favoris / Favori
        if (onFavoriteToggle != null) {
            FilterChip(
                selected = isFavorite,
                enabled = !favoriteDisabled,
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = minChipWidth),
                shape = RoundedCornerShape(12.dp),
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isFavorite) "Favori" else "Favoris")
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
            )
        }

        // Bouton Bande-annonce (Bouton d'action compact)
        trailer?.let { tr ->
            FilledTonalIconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${tr.key}"))
                    context.startActivity(intent)
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Bande-annonce",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}