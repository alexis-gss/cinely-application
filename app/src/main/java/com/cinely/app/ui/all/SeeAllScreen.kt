package com.cinely.app.ui.all

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cinely.app.data.Repository
import com.cinely.app.ui.components.MediaCardData
import com.cinely.app.ui.components.MediaPoster

private const val PAGE_SIZE = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeeAllScreen(
    title: String,
    allItems: List<MediaCardData>, // liste complète déjà en mémoire (vient de Room, pas du réseau)
    repository: Repository,
    onOpenItem: (MediaCardData) -> Unit,
    onBack: () -> Unit
) {
    var loadedCount by remember(allItems) { mutableIntStateOf(minOf(PAGE_SIZE, allItems.size)) }
    val visibleItems = remember(allItems, loadedCount) { allItems.take(loadedCount) }
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    // Détecte qu'on approche de la fin de la liste visible pour charger la page suivante.
    LaunchedEffect(gridState, allItems) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= loadedCount - 6 &&
                    loadedCount < allItems.size
                ) {
                    loadedCount = minOf(loadedCount + PAGE_SIZE, allItems.size)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visibleItems, key = { it.key }) { item ->
                MediaPoster(
                    repository = repository,
                    posterPath = item.posterPath,
                    contentDescription = item.title,
                    modifier = Modifier
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenItem(item) }
                )
            }
            if (loadedCount < allItems.size) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}
