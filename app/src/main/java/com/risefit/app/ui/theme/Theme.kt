package com.risefit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Fixed palette: black surfaces with neon accents. */
object RF {
    val Bg = Color(0xFF0A0A0F)
    val Surface = Color(0xFF110F19)
    val Card = Color(0xFF181423)
    val CardHigh = Color(0xFF211B31)
    val Outline = Color(0xFF3A3150)
    val TextPrimary = Color(0xFFF3EEFF)
    val TextDim = Color(0xFFA79FBC)
    val Success = Color(0xFF39E58C)
    val Warning = Color(0xFFFFC857)
    val Danger = Color(0xFFFF5C7A)
}

val accentColors = listOf(
    Color(0xFFB26BFF), // neon purple
    Color(0xFF4DA3FF), // neon blue
    Color(0xFF39E58C), // neon green
    Color(0xFFFF5FA2)  // neon pink
)

val accentNames = listOf("بنفش نئونی", "آبی نئونی", "سبز نئونی", "صورتی نئونی")

@Composable
fun RiseFitTheme(accentIndex: Int, content: @Composable () -> Unit) {
    val accent = accentColors[accentIndex.coerceIn(0, accentColors.lastIndex)]
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color(0xFF12001F),
        primaryContainer = accent.copy(alpha = 0.22f),
        onPrimaryContainer = RF.TextPrimary,
        secondary = accent,
        onSecondary = Color(0xFF12001F),
        secondaryContainer = accent.copy(alpha = 0.20f),
        onSecondaryContainer = RF.TextPrimary,
        background = RF.Bg,
        onBackground = RF.TextPrimary,
        surface = RF.Surface,
        onSurface = RF.TextPrimary,
        surfaceVariant = RF.Card,
        onSurfaceVariant = RF.TextDim,
        surfaceContainer = RF.Card,
        surfaceContainerHigh = RF.CardHigh,
        surfaceContainerHighest = RF.CardHigh,
        outline = RF.Outline,
        outlineVariant = RF.Outline,
        error = RF.Danger
    )
    // The whole app is Persian: force right-to-left layout regardless of device language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
