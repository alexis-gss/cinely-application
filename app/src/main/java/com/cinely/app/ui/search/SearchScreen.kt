package com.cinely.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cinely.app.data.Repository
import com.cinely.app.data.SearchResult
import com.cinely.app.ui.components.AppTopBar
import com.cinely.app.ui.components.MediaItemRow
import com.cinely.app.ui.components.SectionTitle
import com.cinely.app.ui.components.toCardData
import com.cinely.app.ui.theme.CinelyColors
import kotlinx.coroutines.delay

private const val DEBOUNCE_MS = 500L
private const val POPULAR_LIMIT = 15

private enum class MediaFilter(val label: String) {
    ALL("Tout"),
    MOVIE("Films"),
    TV("Séries")
}

private enum class SortOption(val label: String) {
    RELEVANCE("Pertinence"),
    RATING("Mieux notés"),
    RECENT("Plus récents")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(repository: Repository, onOpenItem: (String, Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var rawResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var mediaFilter by remember { mutableStateOf(MediaFilter.ALL) }
    var sortOption by remember { mutableStateOf(SortOption.RELEVANCE) }

    var popularItems by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isPopularLoading by remember { mutableStateOf(true) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            popularItems = repository.getPopular()
        } catch (e: Exception) {
            // silencieux
        } finally {
            isPopularLoading = false
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            rawResults = emptyList()
            errorMessage = null
            return@LaunchedEffect
        }
        delay(DEBOUNCE_MS)
        isLoading = true
        errorMessage = null
        try {
            rawResults = repository.search(query)
        } catch (e: Exception) {
            errorMessage = "Recherche impossible. Vérifie ta connexion ou ta clé API TMDB."
        } finally {
            isLoading = false
        }
    }

    val filteredResults = remember(rawResults, mediaFilter, sortOption) {
        rawResults
            .filter { r ->
                when (mediaFilter) {
                    MediaFilter.ALL -> true
                    MediaFilter.MOVIE -> r.resolvedMediaType == "movie"
                    MediaFilter.TV -> r.resolvedMediaType == "tv"
                }
            }
            .let { list ->
                when (sortOption) {
                    SortOption.RELEVANCE -> list
                    SortOption.RATING -> list.sortedByDescending { it.voteAverage ?: 0.0 }
                    SortOption.RECENT -> list.sortedByDescending {
                        it.releaseDate ?: it.firstAirDate ?: ""
                    }
                }
            }
    }

    val extended = CinelyColors.colors

    Scaffold(
        topBar = {
            AppTopBar(title = "Recherche")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Zone fixe (ne scrolle pas) ---
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(50))
                    .focusRequester(focusRequester),
                placeholder = { Text("Film, série, manga…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Effacer la recherche")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = extended.chipSurface,
                    focusedContainerColor = extended.chipSurface,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (query.isNotBlank()) {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(MediaFilter.entries) { filter ->
                        FilterChip(
                            selected = mediaFilter == filter,
                            onClick = { mediaFilter = filter },
                            label = { Text(filter.label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extended.badgeBackground,
                                selectedLabelColor = extended.badgeText,
                                containerColor = extended.chipSurface
                            )
                        )
                    }
                    item {
                        VerticalDivider(
                            modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)
                        )
                    }
                    items(SortOption.entries) { sort ->
                        FilterChip(
                            selected = sortOption == sort,
                            onClick = { sortOption = sort },
                            label = { Text(sort.label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extended.badgeBackground,
                                selectedLabelColor = extended.badgeText,
                                containerColor = extended.chipSurface
                            )
                        )
                    }
                }
            }

            // --- Zone scrollable ---
            Box(modifier = Modifier.weight(1f)) {
                when {
                    query.isBlank() -> {
                        if (isPopularLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 0.dp,
                                    end = 16.dp,
                                    bottom = 95.dp
                                ),
                            ) {
                                item { SectionTitle("Populaire en ce moment") }
                                itemsIndexed(
                                    popularItems.take(POPULAR_LIMIT),
                                    key = { _, item -> "${item.resolvedMediaType}_${item.id}" }
                                ) { index, popularItem ->
                                    Row(
                                        Modifier.padding(
                                            bottom = if (index == popularItems.take(POPULAR_LIMIT).lastIndex) 0.dp else 12.dp
                                        )
                                    ) {
                                        MediaItemRow(
                                            repository = repository,
                                            item = popularItem.toCardData(),
                                            onClick = { onOpenItem(popularItem.resolvedMediaType, popularItem.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    errorMessage != null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(errorMessage!!, modifier = Modifier.padding(24.dp))
                        }
                    }

                    filteredResults.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucun résultat")
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                top = 8.dp,
                                end = 12.dp,
                                bottom = 110.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredResults, key = { "${it.resolvedMediaType}_${it.id}" }) { r ->
                                MediaItemRow(
                                    repository = repository,
                                    item = r.toCardData(),
                                    onClick = { onOpenItem(r.resolvedMediaType, r.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
