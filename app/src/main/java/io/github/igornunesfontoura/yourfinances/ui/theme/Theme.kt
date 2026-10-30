package io.github.igornunesfontoura.yourfinances.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** Fixed brand palette; light or dark follows the system. */
@Composable
fun YourFinancesTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalFinanceColors provides if (dark) DarkFinanceColors else LightFinanceColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Income, expense and savings colors of the current theme. */
val MaterialTheme.finance: FinanceColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFinanceColors.current
