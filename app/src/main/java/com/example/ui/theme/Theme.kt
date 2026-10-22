package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RoseAccent,
    onPrimary = Color.White,
    primaryContainer = RosePrimaryDark,
    onPrimaryContainer = RoseLight,
    secondary = CupidGold,
    onSecondary = Color.Black,
    secondaryContainer = NightCardBorder,
    onSecondaryContainer = Color.White,
    background = NightBackground,
    onBackground = TextPrimaryDark,
    surface = NightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NightCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = NightCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = RoseLight,
    onPrimaryContainer = RosePrimaryDark,
    secondary = RoseSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightCard,
    onSecondaryContainer = RosePrimaryDark,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to romantic luxury dark theme
    dynamicColor: Boolean = false, // Keep brand aesthetic
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
