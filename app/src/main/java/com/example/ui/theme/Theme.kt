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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoLavender,
    secondary = BentoPurpleCardBg,
    tertiary = BentoCycleIconBg,
    background = BentoCharcoalBg,
    surface = Color(0xFF262529),
    onPrimary = BentoCharcoalBg,
    onSecondary = Color.White,
    onTertiary = BentoTextPrimary,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF3A383F)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoBlueCardText,
    secondary = BentoPurpleCardText,
    tertiary = BentoCycleIconTint,
    background = BentoBg,
    surface = BentoWhiteCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = BentoTextPrimary,
    onSurface = BentoTextPrimary,
    outline = BentoBorderColor
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Default to Bento Grid light theme for a stunning modern look
  dynamicColor: Boolean = false, // Keep consistent professional branding
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
