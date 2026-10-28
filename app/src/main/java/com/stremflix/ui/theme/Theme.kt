package com.stremflix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StremFlixColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = NetflixWhite,
    primaryContainer = NetflixCardSurface,
    onPrimaryContainer = NetflixWhite,
    secondary = NetflixGreenMatch,
    onSecondary = NetflixBlack,
    background = NetflixBlack,
    onBackground = NetflixWhite,
    surface = NetflixDarkSurface,
    onSurface = NetflixWhite,
    surfaceVariant = NetflixCardSurface,
    onSurfaceVariant = NetflixLightGray,
    outline = NetflixMutedGray,
    outlineVariant = NetflixBorderHighlight
)

@Composable
fun StremFlixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StremFlixColorScheme,
        typography = Typography,
        content = content
    )
}
