package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = Orange500,
    secondary = Neutral800,
    tertiary = Red500,
    background = PitchBlack,
    surface = Neutral900,
    onPrimary = BrightWhite,
    onSecondary = Neutral100,
    onTertiary = BrightWhite,
    onBackground = Neutral100,
    onSurface = Neutral100
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme // Force dark theme for the camera app

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
