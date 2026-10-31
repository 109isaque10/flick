package com.flick.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Phones and portrait tablets: one column of chrome and the bottom navigation.
 *
 * Every screen used to test `screenWidthDp < 600` by hand, and some also tested
 * `orientation == PORTRAIT`, so the same window could be "mobile" on one screen
 * and "TV" on the next.
 */
@Composable
fun isCompactWidth(): Boolean = LocalConfiguration.current.screenWidthDp < 600

/**
 * True on phones in either orientation.
 *
 * A landscape phone reports a wide `screenWidthDp` (a 1080p 420dpi phone is
 * 823x411dp), so width alone promoted phones to the 10-foot layout: the nav rail
 * appeared, the 440dp hero overflowed the 411dp window and the mobile top bar
 * vanished.
 */
@Composable
fun isCompactScreen(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp < 600 || configuration.screenHeightDp < 480
}
