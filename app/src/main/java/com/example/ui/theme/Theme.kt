package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BentoDarkColorScheme = darkColorScheme(
    primary = BentoLilac,
    onPrimary = BentoLilacDark,
    primaryContainer = BentoSurfaceHero,
    onPrimaryContainer = BentoLilac,
    secondary = BentoLilacLight,
    onSecondary = BentoOnDarkLilac,
    secondaryContainer = BentoSurfaceContainer,
    onSecondaryContainer = BentoLilacLight,
    tertiary = BentoGreenPulse,
    onTertiary = BentoLilacDark,
    background = BentoCanvas,
    onBackground = TextPrimary,
    surface = BentoSurfaceHero,
    onSurface = TextPrimary,
    surfaceVariant = BentoSurfaceContainer,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = BentoSurfaceContainer,
    surfaceContainerHigh = BentoSurfaceHero,
    surfaceContainerHighest = BentoSurfaceElevated,
    outline = BentoBorder,
    outlineVariant = BentoBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> BentoDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BentoCanvas.toArgb()
                window.navigationBarColor = BentoCanvas.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
