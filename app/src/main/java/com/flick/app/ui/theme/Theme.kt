package com.flick.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Typography as TvTypography
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme

private val FlickColorScheme = darkColorScheme(
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

/**
 * tv-material equivalent of [FlickColorScheme].
 *
 * The focused-pill defaults in tv-material resolve through `inverseSurface` /
 * `inverseOnSurface`, so mapping those to white/black makes every stock
 * `ButtonDefaults`, `ListItemDefaults` and `NavigationDrawerItemDefaults`
 * treatment land on the Netflix "white pill, dark label" focus look with no
 * per-component colour overrides.
 */
private val FlickTvColorScheme = tvDarkColorScheme(
    primary = NetflixRed,
    onPrimary = NetflixWhite,
    primaryContainer = NetflixCardSurface,
    onPrimaryContainer = NetflixWhite,
    inversePrimary = NetflixRed,
    secondary = NetflixGreenMatch,
    onSecondary = NetflixBlack,
    secondaryContainer = NetflixCardSurface,
    onSecondaryContainer = NetflixWhite,
    tertiary = NetflixLightGray,
    onTertiary = NetflixBlack,
    tertiaryContainer = NetflixDarkSurface,
    onTertiaryContainer = NetflixWhite,
    background = NetflixBlack,
    onBackground = NetflixWhite,
    surface = NetflixDarkSurface,
    onSurface = NetflixWhite,
    surfaceVariant = NetflixCardSurface,
    onSurfaceVariant = NetflixLightGray,
    surfaceTint = NetflixRed,
    inverseSurface = NetflixWhite,
    inverseOnSurface = NetflixBlack,
    error = NetflixRed,
    onError = NetflixBlack,
    errorContainer = NetflixCardSurface,
    onErrorContainer = NetflixWhite,
    border = NetflixBorderHighlight,
    borderVariant = NetflixMutedGray,
    scrim = Color(0xCC141414)
)

private val FlickTvTypography = TvTypography(
    displayLarge = Typography.displayLarge,
    headlineMedium = Typography.headlineMedium,
    titleLarge = Typography.titleLarge,
    bodyLarge = Typography.bodyLarge,
    labelMedium = Typography.labelMedium
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FlickTheme(
    content: @Composable () -> Unit
) {
    // tv-material is provided as the outer theme so tv components resolve their
    // own LocalColorScheme/LocalTypography; the material3 theme stays nested and
    // intact for the screens that have not been migrated yet.
    TvMaterialTheme(
        colorScheme = FlickTvColorScheme,
        typography = FlickTvTypography
    ) {
        MaterialTheme(
            colorScheme = FlickColorScheme,
            typography = Typography,
            content = content
        )
    }
}