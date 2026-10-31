package io.github.igornunesfontoura.yourfinances.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand: the emerald of the launcher icon (#1B6E4F). Cards sit on surfaceContainerLow, a step
// above the background, so the screen reads as white cards on a soft neutral page.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF1B6E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEFDF),
    onPrimaryContainer = Color(0xFF002115),
    inversePrimary = Color(0xFF6EDBA8),
    secondary = Color(0xFF4A5A54),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2EAE6),
    onSecondaryContainer = Color(0xFF111D18),
    tertiary = Color(0xFF4F5BD5),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E3FF),
    onTertiaryContainer = Color(0xFF0B1255),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4F6F5),
    onBackground = Color(0xFF151C19),
    surface = Color(0xFFF4F6F5),
    onSurface = Color(0xFF151C19),
    surfaceVariant = Color(0xFFE3E9E6),
    onSurfaceVariant = Color(0xFF5C6863),
    surfaceTint = Color(0xFF1B6E4F),
    inverseSurface = Color(0xFF2A322E),
    inverseOnSurface = Color(0xFFECF2EE),
    outline = Color(0xFF8A9690),
    outlineVariant = Color(0xFFDCE3DF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDDE3E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF2F0),
    surfaceContainerHigh = Color(0xFFE8EDEA),
    surfaceContainerHighest = Color(0xFFE2E8E5),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF6EDBA8),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF0E5A3F),
    onPrimaryContainer = Color(0xFFB8F2D4),
    inversePrimary = Color(0xFF1B6E4F),
    secondary = Color(0xFFB6C8C0),
    onSecondary = Color(0xFF21312B),
    secondaryContainer = Color(0xFF2E3D37),
    onSecondaryContainer = Color(0xFFD2E4DB),
    tertiary = Color(0xFFB9C0FF),
    onTertiary = Color(0xFF1A237E),
    tertiaryContainer = Color(0xFF343E9E),
    onTertiaryContainer = Color(0xFFE0E3FF),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1412),
    onBackground = Color(0xFFE0E6E2),
    surface = Color(0xFF0E1412),
    onSurface = Color(0xFFE0E6E2),
    surfaceVariant = Color(0xFF2A3430),
    onSurfaceVariant = Color(0xFF9DABA4),
    surfaceTint = Color(0xFF6EDBA8),
    inverseSurface = Color(0xFFE0E6E2),
    inverseOnSurface = Color(0xFF2A322E),
    outline = Color(0xFF66736D),
    outlineVariant = Color(0xFF26302C),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF333B37),
    surfaceDim = Color(0xFF0E1412),
    surfaceContainerLowest = Color(0xFF090D0C),
    surfaceContainerLow = Color(0xFF161D1A),
    surfaceContainer = Color(0xFF1B2320),
    surfaceContainerHigh = Color(0xFF212A26),
    surfaceContainerHighest = Color(0xFF28322E),
)

/** Meaning colors for money, which the Material roles do not cover. */
@Immutable
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val savings: Color,
    /** The "disponível" card: brand color, or alarm when the month is overspent. */
    val hero: Color,
    val onHero: Color,
    val heroOverspent: Color,
    val onHeroOverspent: Color,
)

internal val LightFinanceColors = FinanceColors(
    income = Color(0xFF1B7A4F),
    expense = Color(0xFFD13B3B),
    savings = Color(0xFF4F5BD5),
    hero = Color(0xFF1B6E4F),
    onHero = Color(0xFFFFFFFF),
    heroOverspent = Color(0xFFB3261E),
    onHeroOverspent = Color(0xFFFFFFFF),
)

internal val DarkFinanceColors = FinanceColors(
    income = Color(0xFF6EDBA8),
    expense = Color(0xFFFF8A80),
    savings = Color(0xFFB9C0FF),
    hero = Color(0xFF0E5A3F),
    onHero = Color(0xFFE6FFF2),
    heroOverspent = Color(0xFF8C1D18),
    onHeroOverspent = Color(0xFFFFEDEA),
)

internal val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }
