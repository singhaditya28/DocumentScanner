package com.kaushalvasava.apps.documentscanner.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Named themes — values mirror the web's CSS theme strings ─────────────────
enum class AppTheme(val webKey: String, val label: String, val emoji: String) {
    Default("light",               "☀️ Light",          "☀️"),
    Dark("dark",                   "🌙 Dark",           "🌙"),
    MiracleOrange("theme-miracle-orange", "🧡 Miracle Orange", "🧡"),
    OceanBlue("theme-ocean-blue",  "🌊 Ocean Blue",     "🌊"),
    ForestGreen("theme-forest-green", "🌲 Forest Green", "🌲");

    companion object {
        fun fromWebKey(key: String?): AppTheme =
            entries.firstOrNull { it.webKey == key } ?: Default
    }
}

// ─── Color schemes ────────────────────────────────────────────────────────────

private val DefaultLight = lightColorScheme(
    primary = DefaultPrimary,
    onPrimary = DefaultOnPrimary,
    primaryContainer = DefaultPrimaryContainer,
    onPrimaryContainer = DefaultOnPrimaryContainer,
    secondary = DefaultSecondary,
    onSecondary = DefaultOnSecondary,
    secondaryContainer = DefaultSecondaryContainer,
    onSecondaryContainer = DefaultOnSecondaryContainer,
    tertiary = DefaultTertiary,
    onTertiary = DefaultOnTertiary,
    tertiaryContainer = DefaultTertiaryContainer,
    onTertiaryContainer = DefaultOnTertiaryContainer,
    background = DefaultBackground,
    onBackground = DefaultOnBackground,
    surface = DefaultSurface,
    onSurface = DefaultOnSurface,
    surfaceVariant = DefaultSurfaceVariant,
    onSurfaceVariant = DefaultOnSurfaceVariant,
    outline = DefaultOutline,
    error = ErrorRed,
)

private val DefaultDark = darkColorScheme(
    primary = DefaultDarkPrimary,
    onPrimary = DefaultDarkOnPrimary,
    primaryContainer = DefaultDarkPrimaryContainer,
    onPrimaryContainer = DefaultDarkOnPrimaryContainer,
    secondary = DefaultDarkSecondary,
    onSecondary = DefaultDarkOnSecondary,
    secondaryContainer = DefaultDarkSecondaryContainer,
    onSecondaryContainer = DefaultDarkOnSecondaryContainer,
    tertiary = DefaultDarkTertiary,
    onTertiary = DefaultDarkOnTertiary,
    tertiaryContainer = DefaultDarkTertiaryContainer,
    onTertiaryContainer = DefaultDarkOnTertiaryContainer,
    background = DefaultDarkBackground,
    onBackground = DefaultDarkOnBackground,
    surface = DefaultDarkSurface,
    onSurface = DefaultDarkOnSurface,
    surfaceVariant = DefaultDarkSurfaceVariant,
    onSurfaceVariant = DefaultDarkOnSurfaceVariant,
    outline = DefaultDarkOutline,
    error = ErrorRedDark,
)

private val OrangeLight = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = OrangeOnPrimary,
    primaryContainer = OrangePrimaryContainer,
    onPrimaryContainer = OrangeOnPrimaryContainer,
    secondary = OrangeSecondary,
    onSecondary = OrangeOnSecondary,
    secondaryContainer = OrangeSecondaryContainer,
    onSecondaryContainer = OrangeOnSecondaryContainer,
    tertiary = OrangeTertiary,
    onTertiary = OrangeOnTertiary,
    tertiaryContainer = OrangeTertiaryContainer,
    onTertiaryContainer = OrangeOnTertiaryContainer,
    background = OrangeBackground,
    onBackground = OrangeOnBackground,
    surface = OrangeSurface,
    onSurface = OrangeOnSurface,
    surfaceVariant = OrangeSurfaceVariant,
    onSurfaceVariant = OrangeOnSurfaceVariant,
    outline = OrangeOutline,
    error = ErrorRed,
)

private val OrangeDark = darkColorScheme(
    primary = OrangeDarkPrimary,
    onPrimary = OrangeDarkOnPrimary,
    primaryContainer = OrangeDarkPrimaryContainer,
    onPrimaryContainer = OrangeOnPrimaryContainer,
    secondary = OrangeSecondary,
    onSecondary = OrangeOnSecondary,
    secondaryContainer = OrangeSecondaryContainer,
    onSecondaryContainer = OrangeOnSecondaryContainer,
    background = OrangeDarkBackground,
    onBackground = OrangeDarkOnBackground,
    surface = OrangeDarkSurface,
    onSurface = OrangeDarkOnSurface,
    error = ErrorRedDark,
)

private val BlueLight = lightColorScheme(
    primary = BluePrimary,
    onPrimary = BlueOnPrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    tertiary = BlueTertiary,
    onTertiary = BlueOnTertiary,
    tertiaryContainer = BlueTertiaryContainer,
    onTertiaryContainer = BlueOnTertiaryContainer,
    background = BlueBackground,
    onBackground = BlueOnBackground,
    surface = BlueSurface,
    onSurface = BlueOnSurface,
    surfaceVariant = BlueSurfaceVariant,
    onSurfaceVariant = BlueOnSurfaceVariant,
    outline = BlueOutline,
    error = ErrorRed,
)

private val BlueDark = darkColorScheme(
    primary = BlueDarkPrimary,
    onPrimary = BlueDarkOnPrimary,
    primaryContainer = BlueDarkPrimaryContainer,
    onPrimaryContainer = BlueOnPrimaryContainer,
    secondary = BlueSecondary,
    onSecondary = BlueOnSecondary,
    secondaryContainer = BlueSecondaryContainer,
    onSecondaryContainer = BlueOnSecondaryContainer,
    background = BlueDarkBackground,
    onBackground = BlueDarkOnBackground,
    surface = BlueDarkSurface,
    onSurface = BlueDarkOnSurface,
    error = ErrorRedDark,
)

private val GreenLight = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    tertiary = GreenTertiary,
    onTertiary = GreenOnTertiary,
    tertiaryContainer = GreenTertiaryContainer,
    onTertiaryContainer = GreenOnTertiaryContainer,
    background = GreenBackground,
    onBackground = GreenOnBackground,
    surface = GreenSurface,
    onSurface = GreenOnSurface,
    surfaceVariant = GreenSurfaceVariant,
    onSurfaceVariant = GreenOnSurfaceVariant,
    outline = GreenOutline,
    error = ErrorRed,
)

private val GreenDark = darkColorScheme(
    primary = GreenDarkPrimary,
    onPrimary = GreenDarkOnPrimary,
    primaryContainer = GreenDarkPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    background = GreenDarkBackground,
    onBackground = GreenDarkOnBackground,
    surface = GreenDarkSurface,
    onSurface = GreenDarkOnSurface,
    error = ErrorRedDark,
)

// ─── Theme composable ─────────────────────────────────────────────────────────

@Composable
fun DocumentScannerTheme(
    appTheme: AppTheme = AppTheme.Default,
    // dynamicColor only applies when user picks Default/Dark, to match system wallpaper
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val colorScheme = when (appTheme) {
        AppTheme.Dark -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(LocalContext.current)
            } else DefaultDark
        }
        AppTheme.Default -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val ctx = LocalContext.current
                if (systemDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            } else if (systemDark) DefaultDark else DefaultLight
        }
        AppTheme.MiracleOrange -> if (systemDark) OrangeDark else OrangeLight
        AppTheme.OceanBlue    -> if (systemDark) BlueDark  else BlueLight
        AppTheme.ForestGreen  -> if (systemDark) GreenDark else GreenLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                appTheme != AppTheme.Dark && !systemDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}