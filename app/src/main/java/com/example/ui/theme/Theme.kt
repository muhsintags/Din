package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.viewmodel.AppThemeSetting

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    background = BackgroundParchment,
    onBackground = TextCharcoal,
    surface = CardLight,
    onSurface = TextCharcoal,
    surfaceVariant = BackgroundParchment,
    onSurfaceVariant = TextSecondary,
    outline = SecondaryParchment,
    secondary = SacredGold,
    onSecondary = Color.White,
    secondaryContainer = SecondaryParchment,
    onSecondaryContainer = TextSecondary,
    error = ErrorRed,
    onError = Color.White
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaText,
    onPrimary = SepiaBackground,
    background = SepiaBackground,
    onBackground = SepiaText,
    surface = SepiaBackground,
    onSurface = SepiaText,
    surfaceVariant = SepiaBackground,
    onSurfaceVariant = SepiaText,
    outline = SepiaBorder,
    secondary = SacredGold,
    onSecondary = SepiaBackground,
    secondaryContainer = SepiaBorder,
    onSecondaryContainer = SepiaText,
    error = ErrorRed,
    onError = Color.White
)

private val GoldPrimaryDark = Color(0xFFD4AF37)
private val DarkOutlineColor = Color(0xFF484E4A)
private val DarkSurfaceVariantColor = Color(0xFF282C2A)

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimaryDark,
    onPrimary = Color(0xFF121413),
    primaryContainer = Color(0xFF332D1B),
    onPrimaryContainer = GoldPrimaryDark,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariantColor,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutlineColor,
    outlineVariant = Color(0xFF3B403D),
    secondary = SacredGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2E3330),
    onSecondaryContainer = Color(0xFFF0ECE1),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ScriptoriumTheme(
    themeSetting: AppThemeSetting = AppThemeSetting.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeSetting) {
        AppThemeSetting.LIGHT -> LightColorScheme
        AppThemeSetting.DARK -> DarkColorScheme
        AppThemeSetting.SEPIA -> SepiaColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ScriptoriumTypography,
        content = content
    )
}
