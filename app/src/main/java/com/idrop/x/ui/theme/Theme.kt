package com.idrop.x.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Iris = Color(0xFF5865F2)
val Cyan = Color(0xFF22D3EE)
val Violet = Color(0xFFA855F7)
val Mint = Color(0xFF34D399)
val Amber = Color(0xFFFBBF24)
val Ink = Color(0xFF070A14)
val Panel = Color(0xFF0E1220)
val PanelRaised = Color(0xFF151A2E)
val TextPrimary = Color(0xFFF4F6FF)
val TextMuted = Color(0xFF9AA3C7)
val OutlineSoft = Color(0xFF2A3152)

val Aurora = Brush.linearGradient(listOf(Iris, Violet, Cyan))

private val IdropColors = darkColorScheme(
    primary = Iris,
    onPrimary = Color.White,
    primaryContainer = Iris.copy(alpha = 0.22f),
    onPrimaryContainer = Color.White,
    secondary = Cyan,
    onSecondary = Color(0xFF001318),
    secondaryContainer = Cyan.copy(alpha = 0.18f),
    onSecondaryContainer = Cyan,
    tertiary = Violet,
    onTertiary = Color.White,
    background = Ink,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = TextMuted,
    outline = OutlineSoft,
    error = Color(0xFFFF6B81),
)

val IdropTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 52.sp,
        letterSpacing = (-1.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
    ),
)

@Composable
fun IdropTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IdropColors,
        typography = IdropTypography,
        content = content,
    )
}
