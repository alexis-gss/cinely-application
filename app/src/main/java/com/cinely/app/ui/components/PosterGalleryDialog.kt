package com.cinely.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Visionneuse plein écran d'une liste de posters : swipe horizontal (HorizontalPager),
 * flèches précédent/suivant, compteur "i / total". Transition d'ouverture/fermeture en
 * fondu + léger zoom (pas un vrai "shared element" depuis la vignette d'origine, mais
 * une transition progressive fluide sans dépendance expérimentale de Compose).
 */
@Composable
fun PosterGalleryDialog(
    posters: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (posters.isEmpty()) return

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true } // déclenche l'animation d'entrée dès l'ouverture

    Dialog(
        onDismissRequest = { visible = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, posters.lastIndex)
        ) { posters.size }
        LaunchedEffect(visible) {
            if (!visible) {
                delay(200)
                onDismiss()
            }
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(tween(220), initialScale = 0.85f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.85f)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    PosterImage(
                        model = posters[page],
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(24.dp)
                    )
                }
                IconButton(
                    onClick = { visible = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Color.White)
                }
                if (posters.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                val prev = (pagerState.currentPage - 1 + posters.size) % posters.size
                                pagerState.animateScrollToPage(prev)
                            }
                        }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Précédent", tint = Color.White)
                        }
                        Text(
                            "${pagerState.currentPage + 1} / ${posters.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        IconButton(onClick = {
                            scope.launch {
                                val next = (pagerState.currentPage + 1) % posters.size
                                pagerState.animateScrollToPage(next)
                            }
                        }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Suivant", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}