package com.flick.app.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.flick.app.ui.theme.NetflixDimens

/**
 * TV-only: lets a backdrop/hero artwork start at screen x = 0 so it continues
 * underneath the navigation rail instead of stopping at the rail's right edge.
 *
 * The TV content container is inset by [NetflixDimens.RailWidthCollapsed], which
 * also moves the origin of everything inside it to x = 80dp, so the artwork's
 * own `fillMaxSize()` can never reach the left edge. Requiring the full screen
 * width and offsetting back by the same inset puts the element at
 * `[-inset, screenWidth - inset]` in that shifted space, i.e. screen 0..width.
 *
 * Nothing clips the overflow: the padded container and the root boxes only have
 * backgrounds, and `LazyColumn` trims the cross axis by `MaxSupportedElevation`
 * (30dp) at most — which lands inside the rail's ~97%-opaque edge gradient.
 */
@Composable
fun Modifier.railFullBleed(): Modifier {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return this
        .requiredWidth(screenWidth)
        .offset(x = -NetflixDimens.RailWidthCollapsed)
}
