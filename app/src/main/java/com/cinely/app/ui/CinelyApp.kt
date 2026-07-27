package com.cinely.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.cinely.app.data.ApiKeyStore
import com.cinely.app.data.LanguagePreferenceStore
import com.cinely.app.data.Repository
import com.cinely.app.data.ThemePreferenceStore
import com.cinely.app.ui.account.AccountScreen
import com.cinely.app.ui.detail.DetailScreen
import com.cinely.app.ui.gate.ApiKeyGateScreen
import com.cinely.app.ui.planning.PlanningScreen
import com.cinely.app.ui.bookmark.BookmarkScreen
import com.cinely.app.ui.search.SearchScreen
import com.cinely.app.ui.settings.SettingsScreen
import com.cinely.app.ui.stats.StatsScreen
import com.cinely.app.ui.all.SeeAllScreen
import com.cinely.app.ui.components.MediaCardData
import com.cinely.app.ui.theme.CinelyColors
import com.cinely.app.ui.theme.CinelyExtendedColors

private sealed class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Planning : Tab("agenda", "Agenda", Icons.Filled.CalendarMonth)
    data object Bookmark : Tab("suivi", "Suivi", Icons.Filled.Bookmark)
    data object Search : Tab("recherche", "Recherche", Icons.Filled.Search)
    data object Account : Tab("compte", "Compte", Icons.Filled.AccountCircle)
}

private val tabs = listOf(Tab.Planning, Tab.Bookmark, Tab.Search, Tab.Account)

// Durée du fade des transitions de page, réutilisée telle quelle par la nav bar
// flottante pour que son apparition/disparition reste synchronisée visuellement.
private const val PAGE_FADE_DURATION_MS = 300

private const val DETAIL_ROUTE = "detail/{mediaType}/{tmdbId}"
private const val SETTINGS_ROUTE = "settings"
private const val STATS_ROUTE = "stats"
private const val SEE_ALL_ROUTE = "all"
fun detailRoute(mediaType: String, tmdbId: Int) = "detail/$mediaType/$tmdbId"

/**
 * Point d'entrée de l'UI. Tant qu'aucune clé API TMDB valide n'est enregistrée
 * dans apiKeyStore, seul ApiKeyGateScreen est affiché : aucun onglet, aucune
 * navigation possible vers le reste de l'appli. Dès que la clé est validée,
 * apiKeyStore.apiKey change d'état et cette fonction recompose automatiquement
 * pour basculer vers MainScaffold.
 */
@Composable
fun CinelyApp(
    repository: Repository,
    apiKeyStore: ApiKeyStore,
    themeStore: ThemePreferenceStore,
    languageStore: LanguagePreferenceStore
) {
    val apiKey by apiKeyStore.apiKey.collectAsState()

    if (apiKey.isNullOrBlank()) {
        ApiKeyGateScreen(repository = repository, apiKeyStore = apiKeyStore)
    } else {
        MainScaffold(repository = repository, apiKeyStore = apiKeyStore, themeStore = themeStore, languageStore = languageStore)
    }
}

@Composable
private fun MainScaffold(
    repository: Repository,
    apiKeyStore: ApiKeyStore,
    themeStore: ThemePreferenceStore,
    languageStore: LanguagePreferenceStore
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val hideBottomBar = currentDestination?.route == DETAIL_ROUTE ||
            currentDestination?.route == SETTINGS_ROUTE ||
            currentDestination?.route == STATS_ROUTE
    var seeAllPayload by remember { mutableStateOf<Pair<String, List<MediaCardData>>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = Tab.Planning.route,
            modifier = Modifier.fillMaxSize(), // plus de innerPadding : plein écran
            enterTransition = { fadeIn(animationSpec = tween(PAGE_FADE_DURATION_MS)) },
            exitTransition = { fadeOut(animationSpec = tween(PAGE_FADE_DURATION_MS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(PAGE_FADE_DURATION_MS)) },
            popExitTransition = { fadeOut(animationSpec = tween(PAGE_FADE_DURATION_MS)) }
        ) {
            composable(Tab.Planning.route) {
                PlanningScreen(repository = repository, onOpenItem = { mediaType, id ->
                    navController.navigate(detailRoute(mediaType, id))
                })
            }
            composable(Tab.Bookmark.route) {
                BookmarkScreen(
                    repository = repository,
                    onOpenItem = { mediaType, id ->
                        navController.navigate(detailRoute(mediaType, id))
                    },
                    onSeeAll = { title, items ->
                        seeAllPayload = title to items
                        navController.navigate(SEE_ALL_ROUTE)
                    }
                )
            }
            composable(Tab.Search.route) {
                SearchScreen(repository = repository, onOpenItem = { mediaType, id ->
                    navController.navigate(detailRoute(mediaType, id))
                })
            }
            composable(Tab.Account.route) {
                AccountScreen(
                    repository = repository,
                    onOpenItem = { mediaType, id -> navController.navigate(detailRoute(mediaType, id)) },
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                    onOpenStats = { navController.navigate(STATS_ROUTE) },
                    onSeeAll = { title, items ->
                        seeAllPayload = title to items
                        navController.navigate(SEE_ALL_ROUTE)
                    }
                )
            }
            composable(DETAIL_ROUTE) { backStackEntry ->
                val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
                val tmdbId = backStackEntry.arguments?.getString("tmdbId")?.toIntOrNull() ?: 0
                DetailScreen(
                    repository = repository,
                    mediaType = mediaType,
                    tmdbId = tmdbId,
                    onBack = { navController.popBackStack() },
                    onOpenItem = { newMediaType, newId ->
                        navController.navigate(detailRoute(newMediaType, newId))
                    }
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    repository = repository,
                    apiKeyStore = apiKeyStore,
                    themeStore = themeStore,
                    languageStore = languageStore,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(STATS_ROUTE) {
                StatsScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(SEE_ALL_ROUTE) {
                seeAllPayload?.let { (sectionTitle, items) ->
                    SeeAllScreen(
                        title = sectionTitle,
                        allItems = items,
                        repository = repository,
                        onOpenItem = { navController.navigate(detailRoute(it.mediaType, it.tmdbId)) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = !hideBottomBar,
            enter = fadeIn(animationSpec = tween(PAGE_FADE_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(PAGE_FADE_DURATION_MS)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingGlassNavBar(
                tabs = tabs,
                currentDestination = currentDestination,
                onTabSelected = { tab ->
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

/**
 * Barre de navigation flottante "verre dépoli" façon TV Time : capsule sombre
 * semi-transparente détachée des bords, liseré lumineux du haut pour simuler un
 * reflet de verre, onglet actif matérialisé par une pastille dorée qui s'étire/se
 * rétracte en douceur (spring) quand on change d'onglet, icône qui "pop" légèrement
 * à la sélection.
 */
@Composable
private fun FloatingGlassNavBar(
    tabs: List<Tab>,
    currentDestination: androidx.navigation.NavDestination?,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = CinelyColors.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(33.dp), clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(33.dp))
                .background(extended.navBarContainer)
                .border(1.dp, extended.navBarBorder, RoundedCornerShape(33.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                PillNavItem(
                    tab = tab,
                    selected = selected,
                    extended = extended,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.PillNavItem(
    tab: Tab,
    selected: Boolean,
    extended: CinelyExtendedColors,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val weight by animateFloatAsState(
        targetValue = if (selected) 1.7f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navItemWeight"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) extended.navBarSelectedContainer else Color.Transparent,
        label = "navItemColor"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navIconScale"
    )
    Row(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(
                    Brush.horizontalGradient(
                        listOf(extended.navBarSelectedContainerAlt, extended.navBarSelectedContainer)
                    )
                ) else Modifier.background(containerColor)
            )
            .clickable(
                interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (selected) extended.navBarSelectedIcon else extended.navBarIcon,
            modifier = Modifier.size(21.dp).scale(iconScale)
        )
        if (selected) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            Text(
                text = tab.label,
                color = extended.navBarSelectedIcon,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}
