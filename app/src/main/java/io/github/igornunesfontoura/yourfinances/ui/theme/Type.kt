package io.github.igornunesfontoura.yourfinances.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tabular figures keep amounts aligned in lists and stop the big number from jumping as it changes.
private fun TextStyle.numbers(weight: FontWeight? = fontWeight, tracking: Float? = null) = copy(
    fontWeight = weight,
    fontFeatureSettings = "tnum",
    letterSpacing = tracking?.sp ?: letterSpacing,
)

internal val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.numbers(FontWeight.SemiBold, -1.5f),
        displayMedium = displayMedium.numbers(FontWeight.SemiBold, -1f),
        displaySmall = displaySmall.numbers(FontWeight.SemiBold, -0.5f),
        headlineLarge = headlineLarge.numbers(FontWeight.SemiBold, -0.5f),
        headlineMedium = headlineMedium.numbers(FontWeight.SemiBold, -0.25f),
        headlineSmall = headlineSmall.numbers(FontWeight.SemiBold),
        titleLarge = titleLarge.numbers(FontWeight.SemiBold),
        titleMedium = titleMedium.numbers(FontWeight.SemiBold),
        titleSmall = titleSmall.numbers(FontWeight.SemiBold),
        bodyLarge = bodyLarge.numbers(),
        bodyMedium = bodyMedium.numbers(),
        bodySmall = bodySmall.numbers(),
        labelLarge = labelLarge.numbers(FontWeight.SemiBold),
        labelMedium = labelMedium.numbers(FontWeight.SemiBold),
        labelSmall = labelSmall.numbers(FontWeight.Medium),
    )
}

internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
