package com.cinely.app.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cinely.app.data.FollowedItem
import com.cinely.app.data.Repository
import com.cinely.app.ui.components.AppTopBar
import com.cinely.app.ui.components.MediaItemRow
import com.cinely.app.ui.components.SectionTitle
import com.cinely.app.ui.components.toPlanningCardData
import com.cinely.app.ui.theme.CinelyColors
import com.cinely.app.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(repository: Repository, onOpenItem: (String, Int) -> Unit) {
    val allFollowed by repository.observeFollowed().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Le Planning n'a de sens que pour ce qui a une date de sortie connue et pas encore
    // passée : une série "Ended" ou une "Returning Series" sans prochain épisode annoncé
    // a nextAirDate == null et ne doit donc pas apparaître ici (elle atterrit dans
    // Bookmark une fois sortie, si elle n'a pas été vue).
    val followed = remember(allFollowed) {
        allFollowed.filter { it.nextAirDate != null && !DateUtils.isReleased(it.nextAirDate) }
    }

    // Rafraîchissement "au cas où" à l'ouverture de l'écran : throttlé côté Repository
    // (6h normalement, 30min si nextAirDate est encore inconnu pour une série en cours),
    // donc n'entraîne pas systématiquement d'appel réseau.
    LaunchedEffect(allFollowed.map { it.tmdbId }) {
        allFollowed.filter { it.mediaType == "tv" }.forEach { item ->
            repository.refreshFollowedIfStale(item)
        }
    }

    fun refreshAllManually() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            try {
                allFollowed.filter { it.mediaType == "tv" }.forEach { item ->
                    repository.refreshFollowedIfStale(item, force = true)
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Agenda",
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
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (followed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aucune sortie à venir pour l'instant.\nSuis un film ou une série depuis la recherche.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                // Regroupement par date de sortie en conservant l'ordre déjà trié par le DAO
                // (nextAirDate IS NULL puis ASC), pour afficher un seul en-tête "Vendredi 14 août"
                // au-dessus de toutes les sorties de ce jour-là.
                val groups: List<Pair<String?, List<FollowedItem>>> = remember(followed) {
                    val result = mutableListOf<Pair<String?, MutableList<FollowedItem>>>()
                    followed.forEach { item ->
                        val last = result.lastOrNull()
                        if (last != null && last.first == item.nextAirDate) {
                            last.second.add(item)
                        } else {
                            result.add(item.nextAirDate to mutableListOf(item))
                        }
                    }
                    result
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 0.dp,
                        end = 16.dp,
                        bottom = 95.dp
                    ),
                ) {
                    groups.forEach { (date, groupItems) ->
                        item(key = "header_${date ?: "unknown"}") {
                            SectionTitle(
                                if (date.isNullOrBlank()) "Date inconnue" else DateUtils.formatFrenchDay(date),
                            )
                        }
                        items(groupItems, key = { item -> "${item.mediaType}_${item.tmdbId}" }) { followedItem ->
                            Row(Modifier.padding(bottom = 8.dp)) {
                                MediaItemRow(
                                    repository = repository,
                                    item = followedItem.toPlanningCardData(),
                                    onClick = { onOpenItem(followedItem.mediaType, followedItem.tmdbId) },
                                    trailingContent = {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            CinelyColors.colors.navBarSelectedContainerAlt,
                                                            CinelyColors.colors.navBarSelectedContainer
                                                        )
                                                    )
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = DateUtils.daysRemainingLabel(followedItem.nextAirDate),
                                                color = CinelyColors.colors.navBarSelectedIcon,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
