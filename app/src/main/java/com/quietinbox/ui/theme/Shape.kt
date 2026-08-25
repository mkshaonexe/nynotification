package com.quietinbox.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape

@Immutable
data class QuietShapes(
    val chip: Shape = RoundedCornerShape(QuietTokens.radiusChip),
    val card: Shape = RoundedCornerShape(QuietTokens.radiusCard),
    val sheet: Shape = RoundedCornerShape(topStart = QuietTokens.radiusSheet, topEnd = QuietTokens.radiusSheet),
    val dialog: Shape = RoundedCornerShape(QuietTokens.radiusSheet),
    val full: Shape = CircleShape,
)

val MaterialShapes = Shapes(
    small = RoundedCornerShape(QuietTokens.radiusChip),
    medium = RoundedCornerShape(QuietTokens.radiusCard),
    large = RoundedCornerShape(QuietTokens.radiusSheet),
    extraLarge = CircleShape
)

val LocalAppShapes = staticCompositionLocalOf { QuietShapes() }
