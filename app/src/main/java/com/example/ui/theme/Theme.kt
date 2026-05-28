package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FuelDarkColorScheme = darkColorScheme(
    primary = FuelAmber,
    onPrimary = Color.Black,
    primaryContainer = FuelAmberDark,
    onPrimaryContainer = Color.White,
    secondary = AlertBlue,
    onSecondary = Color.White,
    background = CharcoalBlack,
    onBackground = Color.White,
    surface = CarbonCard,
    onSurface = Color.White,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = SteelBlueText,
    outline = CarbonBorder,
    error = AlertRed
)

private val FuelLightColorScheme = lightColorScheme(
    primary = FuelAmber,
    onPrimary = Color.Black,
    primaryContainer = FuelAmberLight,
    onPrimaryContainer = Color.Black,
    secondary = AlertBlue,
    onSecondary = Color.White,
    background = LightGrayBg,
    onBackground = DarkSteelText,
    surface = LightCard,
    onSurface = DarkSteelText,
    surfaceVariant = LightCard,
    onSurfaceVariant = DarkSteelText,
    outline = LightBorder,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FuelDarkColorScheme else FuelLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
