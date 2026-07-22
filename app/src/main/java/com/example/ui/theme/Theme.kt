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

private val DarkColorScheme =
  darkColorScheme(
    primary = MD3Primary,
    onPrimary = MD3OnPrimary,
    primaryContainer = MD3PrimaryContainer,
    onPrimaryContainer = MD3OnPrimaryContainer,
    secondary = MD3Secondary,
    onSecondary = MD3OnSecondary,
    secondaryContainer = MD3SecondaryContainer,
    onSecondaryContainer = MD3OnSecondaryContainer,
    tertiary = MD3Tertiary,
    background = MD3Background,
    onBackground = MD3OnBackground,
    surface = MD3Surface,
    onSurface = MD3OnSurface,
    surfaceVariant = MD3SurfaceVariant,
    onSurfaceVariant = MD3OnSurfaceVariant,
    outline = MD3Outline,
    outlineVariant = MD3OutlineVariant,
    error = MD3Error,
    onError = MD3OnError,
    errorContainer = MD3ErrorContainer,
    onErrorContainer = MD3OnErrorContainer
  )

private val LightColorScheme =
  lightColorScheme(
    primary = MD3Primary,
    onPrimary = MD3OnPrimary,
    primaryContainer = MD3PrimaryContainer,
    onPrimaryContainer = MD3OnPrimaryContainer,
    secondary = MD3Secondary,
    onSecondary = MD3OnSecondary,
    secondaryContainer = MD3SecondaryContainer,
    onSecondaryContainer = MD3OnSecondaryContainer,
    tertiary = MD3Tertiary,
    background = MD3Background,
    onBackground = MD3OnBackground,
    surface = MD3Surface,
    onSurface = MD3OnSurface,
    surfaceVariant = MD3SurfaceVariant,
    onSurfaceVariant = MD3OnSurfaceVariant,
    outline = MD3Outline,
    outlineVariant = MD3OutlineVariant,
    error = MD3Error,
    onError = MD3OnError,
    errorContainer = MD3ErrorContainer,
    onErrorContainer = MD3OnErrorContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
