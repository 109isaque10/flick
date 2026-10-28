package com.stremflix.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Canonical Netflix-2020 geometry.
 *
 * Every screen used to invent its own border width, corner radius, focus scale
 * and card width, which is why focus frames and poster sizes drifted between
 * Home, Search, Categories and Detail. Everything visual should come from here.
 */
object NetflixDimens {
    // Cards
    val CardRadius = 4.dp
    val CardFocusBorder = 3.dp
    val CardFocusScale = 1.05f
    val PosterWidth = 135.dp
    val PosterWidthMobile = 132.dp
    val PosterWidthTv = 168.dp
    val BigCardRadius = 8.dp
    val ThumbnailRadius = 2.dp

    // Rows
    val RowItemSpacing = 8.dp
    val RowContentPadding = 24.dp
    val RowContentPaddingMobile = 16.dp
    val RowVerticalSpacing = 14.dp
    val RowTitleSpacing = 10.dp

    // Screens
    val ScreenPaddingH = 24.dp
    val ScreenPaddingV = 20.dp
    val SectionSpacing = 32.dp
    val ContentSpacing = 16.dp

    // Navigation
    val RailWidthCollapsed = 80.dp
    val RailWidthExpanded = 240.dp
    val RailItemHeight = 52.dp
    val SidebarWidth = 260.dp
}
