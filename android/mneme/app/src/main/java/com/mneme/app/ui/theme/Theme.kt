package com.mneme.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Product look: ink + sage — quiet, private, everyday (not purple/glow AI chrome)
private val Ink = Color(0xFF121A17)
private val Paper = Color(0xFFF3F0E8)
private val Sage = Color(0xFF2F5D50)
private val SageSoft = Color(0xFFD7E4DE)
private val Mist = Color(0xFFE8E6DF)
private val Ember = Color(0xFFB55B2A)

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageSoft,
    onPrimaryContainer = Ink,
    secondary = Ember,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF3D4742),
    outline = Color(0xFF8A938E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FBFB0),
    onPrimary = Ink,
    primaryContainer = Color(0xFF1E3B33),
    onPrimaryContainer = SageSoft,
    secondary = Color(0xFFE0A07A),
    onSecondary = Ink,
    background = Color(0xFF0E1412),
    onBackground = Color(0xFFE8E6DF),
    surface = Color(0xFF0E1412),
    onSurface = Color(0xFFE8E6DF),
    surfaceVariant = Color(0xFF1A2420),
    onSurfaceVariant = Color(0xFFB7C0BB),
    outline = Color(0xFF6B7570),
)

private val BrandTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
)

@Composable
fun MnemeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = BrandTypography,
        content = content,
    )
}
