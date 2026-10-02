package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IsaacColorScheme = darkColorScheme(
    primary = IsaacTearBlue,
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004C70),
    onPrimaryContainer = Color(0xFFC7E7FF),

    secondary = IsaacGold,
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5B4300),
    onSecondaryContainer = Color(0xFFFFE088),

    tertiary = IsaacBrimstone,
    onTertiary = Color(0xFF68000A),
    tertiaryContainer = Color(0xFF930013),
    onTertiaryContainer = Color(0xFFFFDAD7),

    background = IsaacBackground,
    onBackground = IsaacBoneWhite,

    surface = IsaacSurface,
    onSurface = IsaacBoneWhite,
    surfaceVariant = IsaacSurfaceVariant,
    onSurfaceVariant = IsaacTextMuted,

    outline = IsaacBorder,
    outlineVariant = Color(0xFF3A332E),

    error = IsaacBloodRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Isaac theme is best enjoyed in atmospheric dark mode
    dynamicColor: Boolean = false, // Keep thematic Isaac colors by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IsaacColorScheme,
        typography = Typography,
        content = content
    )
}
