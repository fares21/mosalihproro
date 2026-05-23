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

private val DarkColorScheme =
  darkColorScheme(
    primary = VibrantPurple,
    secondary = VibrantPurpleLight,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF313033)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VibrantPurple,
    onPrimary = Color.White,
    primaryContainer = VibrantPrimaryContainer,
    onPrimaryContainer = VibrantOnPrimaryContainer,
    secondary = VibrantSecondary,
    onSecondary = Color.White,
    background = VibrantBg,
    surface = VibrantSurface,
    onBackground = VibrantDarkText,
    onSurface = VibrantDarkText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Enforce our custom vibrant light theme for Odoo-style branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
