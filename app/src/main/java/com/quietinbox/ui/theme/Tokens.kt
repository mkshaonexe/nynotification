package com.quietinbox.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object QuietTokens {

    // Spacing
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val space48 = 48.dp

    // Layout Specific
    val screenHorizontalPadding = 16.dp
    val rowVerticalPadding = 12.dp
    val minTouchTarget = 48.dp

    // Corner Radius
    val radiusChip = 8.dp
    val radiusCard = 12.dp
    val radiusSheet = 20.dp

    // Dark Colors
    object Dark {
        val background = Color(0xFF0F1012)
        val surface = Color(0xFF17181B)
        val surfaceVariant = Color(0xFF1F2125)
        val outline = Color(0xFF2C2F34)
        val onSurface = Color(0xFFE8EAED)
        val onSurfaceVariant = Color(0xFF9AA0A8)
        val primary = Color(0xFF7B93FF)
        val positive = Color(0xFF4ADE80)
        val muted = Color(0xFF9AA0A8)
        val warning = Color(0xFFFBBF24)
        val danger = Color(0xFFF87171)

        val heatmapRamp = listOf(
            Color(0xFF1F2125),
            Color(0xFF2A3565),
            Color(0xFF3F51A8),
            Color(0xFF5A72DB),
            Color(0xFF7B93FF)
        )
    }

    // Light Colors
    object Light {
        val background = Color(0xFFFBFBFC)
        val surface = Color(0xFFFFFFFF)
        val surfaceVariant = Color(0xFFF1F2F4)
        val outline = Color(0xFFE2E4E8)
        val onSurface = Color(0xFF16181B)
        val onSurfaceVariant = Color(0xFF5E646C)
        val primary = Color(0xFF3B5BDB)
        val positive = Color(0xFF16A34A)
        val muted = Color(0xFF6B7280)
        val warning = Color(0xFFB45309)
        val danger = Color(0xFFDC2626)

        val heatmapRamp = listOf(
            Color(0xFFF1F2F4),
            Color(0xFFD0EBFF),
            Color(0xFF748FFC),
            Color(0xFF4C6EF5),
            Color(0xFF3B5BDB)
        )
    }
}
