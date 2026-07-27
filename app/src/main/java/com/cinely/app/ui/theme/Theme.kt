package com.cinely.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ===================================================================================
// Palette "streaming app" façon TV Time : fond quasi noir / anthracite très saturé en
// dark (mode principal de ce type d'appli), jaune-or de la marque comme accent unique
// et fort (jamais dilué dans plein de couleurs), notes de films colorées séparément
// (vert / ambre / rouge) pour rester un signal à part entière.
// ===================================================================================

// ---------------------------------------------------------------------------------
// Palette claire — fond crème chaud, noir du logo en accent fort, jaune en highlight
// ---------------------------------------------------------------------------------
private val LightColors = lightColorScheme(
    primary = Color(0xFF17171A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFC93C),
    onPrimaryContainer = Color(0xFF231A00),
    secondary = Color(0xFF6B6355),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E7D3),
    onSecondaryContainer = Color(0xFF241D0D),
    tertiary = Color(0xFF8A5D00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA6),
    onTertiaryContainer = Color(0xFF2A1B00),
    error = Color(0xFFE0453A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF1A1918),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1918),
    surfaceVariant = Color(0xFFECE5D6),
    onSurfaceVariant = Color(0xFF4C463A),
    outline = Color(0xFF7D7669),
    outlineVariant = Color(0xFFE1DACB),
    inverseSurface = Color(0xFF201F1C),
    inverseOnSurface = Color(0xFFF7F5F0),
    inversePrimary = Color(0xFFFFC93C),
)

// ---------------------------------------------------------------------------------
// Palette sombre — quasi-noir profond façon appli de streaming, jaune-or dominant,
// surfaces légèrement bleutées pour éviter le "gris sale".
// ---------------------------------------------------------------------------------
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC93C),
    onPrimary = Color(0xFF241A00),
    primaryContainer = Color(0xFF4A3600),
    onPrimaryContainer = Color(0xFFFFE49B),
    secondary = Color(0xFFCBC3B0),
    onSecondary = Color(0xFF353024),
    secondaryContainer = Color(0xFF2B271E),
    onSecondaryContainer = Color(0xFFEDE4CE),
    tertiary = Color(0xFFF4A94A),
    onTertiary = Color(0xFF402C00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFDF99),
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A0A0D),
    onBackground = Color(0xFFEFECE4),
    surface = Color(0xFF131317),
    onSurface = Color(0xFFEFECE4),
    surfaceVariant = Color(0xFF232228),
    onSurfaceVariant = Color(0xFFC9C4B8),
    outline = Color(0xFF7D7A72),
    outlineVariant = Color(0xFF2E2D33),
    inverseSurface = Color(0xFFEFECE4),
    inverseOnSurface = Color(0xFF1A1918),
    inversePrimary = Color(0xFF8A5D00),
)

/**
 * Couleurs "maison" non couvertes par le ColorScheme Material3 standard : dégradés
 * sur posters, badges de note colorés (le "signe distinctif" façon TV Time), fond de
 * la nav flottante en verre dépoli, dégradé héros des écrans d'accueil.
 * Accessible partout via `CinelyColors.colors`.
 */
data class CinelyExtendedColors(
    val posterOverlayTop: Color,
    val posterOverlayBottom: Color,
    val posterOverlayText: Color,
    val badgeBackground: Color,
    val badgeText: Color,
    val navBarContainer: Color,
    val navBarBorder: Color,
    val navBarSelectedContainer: Color,
    val navBarSelectedContainerAlt: Color,
    val navBarIcon: Color,
    val navBarSelectedIcon: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val ratingGood: Color,
    val ratingAverage: Color,
    val ratingLow: Color,
    val chipSurface: Color,
)

private val LightExtendedColors = CinelyExtendedColors(
    posterOverlayTop = Color(0x00000000),
    posterOverlayBottom = Color(0xE6141312),
    posterOverlayText = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFFFFC93C),
    badgeText = Color(0xFF231A00),
    navBarContainer = Color(0xFF181817),
    navBarBorder = Color(0x1FFFFFFF),
    navBarSelectedContainer = Color(0xFFFFC93C),
    navBarSelectedContainerAlt = Color(0xFFFFDB74),
    navBarIcon = Color(0xB3FFFFFF),
    navBarSelectedIcon = Color(0xFF231A00),
    cardSurface = Color(0xFFFFFFFF),
    cardBorder = Color(0x14000000),
    heroGradientStart = Color(0xFFFFC93C),
    heroGradientEnd = Color(0xFFFF9D3C),
    ratingGood = Color(0xFF1FB565),
    ratingAverage = Color(0xFFE8A400),
    ratingLow = Color(0xFFE0453A),
    chipSurface = Color(0xFFEFE8D9),
)

private val DarkExtendedColors = CinelyExtendedColors(
    posterOverlayTop = Color(0x00000000),
    posterOverlayBottom = Color(0xF2000000),
    posterOverlayText = Color(0xFFFFFFFF),
    badgeBackground = Color(0xFFFFC93C),
    badgeText = Color(0xFF241A00),
    navBarContainer = Color(0xFF17171C),
    navBarBorder = Color(0x1FFFFFFF),
    navBarSelectedContainer = Color(0xFFFFC93C),
    navBarSelectedContainerAlt = Color(0xFFFFAE42),
    navBarIcon = Color(0x99EFECE4),
    navBarSelectedIcon = Color(0xFF241A00),
    cardSurface = Color(0xFF17171B),
    cardBorder = Color(0x14FFFFFF),
    heroGradientStart = Color(0xFF2A2410),
    heroGradientEnd = Color(0xFF0A0A0D),
    ratingGood = Color(0xFF3ADC85),
    ratingAverage = Color(0xFFFFC93C),
    ratingLow = Color(0xFFFF6B5E),
    chipSurface = Color(0xFF232228),
)

val LocalCinelyColors = staticCompositionLocalOf { LightExtendedColors }

/** Accès pratique : `CinelyColors.colors.badgeBackground` depuis n'importe quel composable. */
object CinelyColors {
    val colors: CinelyExtendedColors
        @Composable get() = LocalCinelyColors.current
}

/** Dégradé "héros" (bandeau de bienvenue, en-têtes de Planning/Compte). */
val CinelyExtendedColors.heroBrush: Brush
    @Composable get() = Brush.verticalGradient(listOf(heroGradientStart, heroGradientEnd))

/** Couleur associée à une note /10 façon TV Time : vert / ambre / rouge. */
fun CinelyExtendedColors.colorForRating(rating: Double): Color = when {
    rating >= 7.0 -> ratingGood
    rating >= 5.0 -> ratingAverage
    else -> ratingLow
}

@Composable
fun CinelyTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        if (activity != null) {
            androidx.compose.runtime.SideEffect {
                WindowCompat.getInsetsController(activity.window, view)
                    .isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(activity.window, view)
                    .isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalCinelyColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CinelyTypography,
            content = content
        )
    }
}
