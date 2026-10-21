package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkTerracottaPrimary,
    onPrimary = DarkTerracottaOnPrimary,
    primaryContainer = DarkTerracottaContainer,
    onPrimaryContainer = DarkTerracottaOnContainer,
    secondary = DarkSageSecondary,
    onSecondary = DarkSageOnSecondary,
    secondaryContainer = DarkSageContainer,
    onSecondaryContainer = DarkSageOnContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = DarkAmberOnTertiary,
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = DarkAmberOnContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TerracottaOnPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TerracottaOnContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageContainer,
    onSecondaryContainer = SageOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = LinenBackground,
    onBackground = LinenOnBackground,
    surface = LinenSurface,
    onSurface = LinenOnSurface,
    surfaceVariant = LinenSurfaceVariant,
    onSurfaceVariant = LinenOnSurfaceVariant,
    outline = CraftOutline
)

@Composable
fun CraftStashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Prefer warm craft palette by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
