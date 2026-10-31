package com.flick.app.ui.navigation

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flick.app.R
import com.flick.app.ui.theme.NetflixBlack
import com.flick.app.ui.theme.NetflixDarkSurface
import com.flick.app.ui.theme.NetflixDimens
import com.flick.app.ui.components.touchClickable
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.theme.NetflixWhite
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface

enum class NavDestination(@StringRes val labelRes: Int, val icon: ImageVector) {
    SEARCH(R.string.nav_search, Icons.Default.Search),
    HOME(R.string.nav_home, Icons.Default.Home),
    NEW_POPULAR(R.string.nav_new_popular, Icons.Default.TrendingUp),
    TV_SHOWS(R.string.nav_tv_shows, Icons.Default.Tv),
    MOVIES(R.string.nav_movies, Icons.Default.Movie),
    CATEGORIES(R.string.nav_categories, Icons.Default.List),
    MY_LIST(R.string.nav_my_list, Icons.Default.BookmarkBorder),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings)
}

/**
 * One nav row shape for every sidebar entry.
 *
 * Selected = red 3dp bar + white bold (the same marker the categories sidebar
 * and detail actions use). Focused = white 2dp border + faint white wash.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    // Focus flips the row to the inverse (white pill / dark label) treatment,
    // which is exactly what tv-material's own defaults resolve to.
    val contentColor = when {
        focused -> NetflixBlack
        selected -> NetflixWhite
        else -> NetflixLightGray
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(NetflixDimens.RailItemHeight)
            .padding(vertical = 2.dp)
            .touchClickable { onClick() },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) NetflixDarkSurface else Color.Transparent,
            contentColor = contentColor,
            focusedContainerColor = NetflixWhite,
            focusedContentColor = NetflixBlack
        ),
        // The collapsed rail is only RailWidthCollapsed wide, so a 1.1 pop would
        // clip; keep the box static and let the colours carry focus.
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(22.dp)
                    .background(
                        color = if (selected) NetflixRed else Color.Transparent,
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(if (expanded) 14.dp else 11.dp))

            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )

            if (expanded) {
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun FlickNavRail(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    onReloadRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var railHasFocus by remember { mutableStateOf(false) }
    // A rail that merely inherited the initial focus (Compose parks D-pad focus
    // on the first focusable while the hero is still loading) must not unfurl
    // the sidebar on launch. It only expands when it was re-entered after having
    // been left, or when the user actually pressed a key while inside it.
    var railEngaged by remember { mutableStateOf(false) }
    var sawFocusLoss by remember { mutableStateOf(false) }
    // The rail must not unfurl from the focus Compose parks on it during the
    // first composition, so auto-expansion is only allowed once the screen has
    // settled.
    var railReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        repeat(3) { withFrameNanos { } }
        railReady = true
    }
    val isRailFocused = railHasFocus && railEngaged
    // Animated again: the "seconds to unfurl" was a debug-build artifact (the
    // release build is ~7x smaller), so the transition is restored. The content
    // sibling is inset by a constant padding, so animating the rail width only
    // re-measures the rail itself, not the poster rows.
    val railWidth by animateDpAsState(
        targetValue = if (isRailFocused) NetflixDimens.RailWidthExpanded
        else NetflixDimens.RailWidthCollapsed,
        animationSpec = tween(durationMillis = 180),
        label = "railWidth"
    )
    val context = LocalContext.current

    val destinations = remember {
        listOf(
            NavDestination.HOME,
            NavDestination.SEARCH,
            NavDestination.NEW_POPULAR,
            NavDestination.TV_SHOWS,
            NavDestination.MOVIES,
            NavDestination.CATEGORIES,
            NavDestination.MY_LIST
        )
    }

    val focusRequesters = remember { List(destinations.size + 2) { FocusRequester() } }

    // Re-entering the rail must land on the current section, not on whichever
    // item happens to sit geometrically closest to the row the viewer left from
    // (that made re-entry feel random). railEngaged only becomes true when the
    // rail was left and then deliberately re-entered, so the launch-time focus
    // that Compose parks on the rail cannot hijack the hero's first focus.
    val selectedIndex = destinations.indexOf(currentDestination).let {
        if (it >= 0) it else if (currentDestination == NavDestination.SETTINGS) destinations.size else 0
    }
    // The rail container is a focusGroup with a focusRestorer pinned to the
    // current section, so entering it resolves the selected item as the group is
    // entered: the nearest item is never focused first and nothing hops
    // afterwards. The effect below is only a defensive fallback for the case
    // where the restorer has no saved focus to give back.
    LaunchedEffect(railEngaged, selectedIndex) {
        if (railEngaged) {
            val target = focusRequesters[selectedIndex]
            repeat(3) {
                withFrameNanos { }
                if (runCatching { target.requestFocus() }.getOrDefault(false)) {
                    return@LaunchedEffect
                }
            }
        }
    }

    Box(
        modifier = modifier
            .width(railWidth)
            .clipToBounds()
            .fillMaxHeight()
            .statusBarsPadding()
            // Entering the rail lands on the section that is currently open
            // instead of the item geometrically closest to the content, which
            // used to focus the wrong row first and then jump to the selection.
            .focusGroup()
            .focusRestorer(focusRequesters[selectedIndex])
            .onFocusChanged { state ->
                if (state.hasFocus && !railHasFocus) {
                    // Expand as soon as the viewer deliberately navigates into the
                    // sidebar. The old gate waited for a second key press, because
                    // sawFocusLoss stayed false when focus had never left the rail
                    // (the hero simply never took focus), which read as a 1.5s lag.
                    if (railReady || sawFocusLoss) railEngaged = true
                } else if (!state.hasFocus && railHasFocus) {
                    railEngaged = false
                    sawFocusLoss = true
                }
                railHasFocus = state.hasFocus
            }
            // Vertical movement stays inside the sidebar; leaving it requires an
            // explicit left/right, so DPAD_DOWN can never fall through onto the
            // screen behind it.
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) railEngaged = true
                false
            }
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        NetflixBlack.copy(alpha = 0.97f),
                        NetflixBlack.copy(alpha = 0.86f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 10.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Profile header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_flick),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(26.dp)
                    )
                    if (isRailFocused) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.user_profile_master),
                                color = NetflixWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.user_profile_switch),
                                color = NetflixLightGray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                destinations.forEachIndexed { index, dest ->
                    RailItem(
                        icon = dest.icon,
                        label = stringResource(dest.labelRes),
                        selected = currentDestination == dest,
                        expanded = isRailFocused,
                        onClick = { onDestinationSelected(dest) },
                        modifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .focusProperties {
                                up = if (index == 0) {
                                    FocusRequester.Cancel
                                } else {
                                    focusRequesters[index - 1]
                                }
                                down = focusRequesters[index + 1]
                            }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                RailItem(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.nav_settings),
                    selected = currentDestination == NavDestination.SETTINGS,
                    expanded = isRailFocused,
                    onClick = { onDestinationSelected(NavDestination.SETTINGS) },
                    modifier = Modifier
                        .focusRequester(focusRequesters[destinations.size])
                        .focusProperties {
                            up = focusRequesters[destinations.lastIndex]
                            down = focusRequesters[destinations.size + 1]
                        }
                )

                val reloadInteraction = remember { MutableInteractionSource() }
                val reloadFocused by reloadInteraction.collectIsFocusedAsState()
                val reloadContentColor = if (reloadFocused) NetflixBlack else NetflixLightGray

                val reloadAction: () -> Unit = {
                    onReloadRequested()
                    (context as? Activity)?.recreate()
                }
                Surface(
                    onClick = reloadAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NetflixDimens.RailItemHeight)
                        .padding(vertical = 2.dp)
                        .focusRequester(focusRequesters[destinations.size + 1])
                        .focusProperties {
                            up = focusRequesters[destinations.size]
                            down = FocusRequester.Cancel
                        }
                        .touchClickable(onClick = reloadAction),
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = reloadContentColor,
                        focusedContainerColor = NetflixWhite,
                        focusedContentColor = NetflixBlack
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                    interactionSource = reloadInteraction
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(3.dp))
                        Spacer(modifier = Modifier.width(if (isRailFocused) 14.dp else 11.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.nav_reload),
                            tint = reloadContentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isRailFocused) {
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.nav_reload),
                                color = reloadContentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlickBottomNavBar(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = remember {
        listOf(
            NavDestination.HOME,
            NavDestination.SEARCH,
            NavDestination.NEW_POPULAR,
            NavDestination.MY_LIST
        )
    }
    val overflow = remember {
        listOf(
            NavDestination.TV_SHOWS,
            NavDestination.MOVIES,
            NavDestination.CATEGORIES,
            NavDestination.SETTINGS
        )
    }
    var moreOpen by remember { mutableStateOf(false) }

    NavigationBar(
        modifier = modifier,
        containerColor = NetflixBlack.copy(alpha = 0.98f),
        contentColor = NetflixWhite
    ) {
        primary.forEach { dest ->
            val isSelected = currentDestination == dest
            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(dest) },
                icon = {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = stringResource(dest.labelRes),
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(dest.labelRes),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NetflixWhite,
                    selectedTextColor = NetflixWhite,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = NetflixLightGray,
                    unselectedTextColor = NetflixLightGray
                )
            )
        }

        // "More" exposes the destinations the bar cannot fit, instead of
        // jumping straight to Settings.
        run {
            val overflowSelected = currentDestination in overflow
            NavigationBarItem(
                selected = overflowSelected,
                onClick = { moreOpen = true },
                icon = {
                    Box {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.nav_more),
                            modifier = Modifier.size(22.dp)
                        )
                        DropdownMenu(
                            expanded = moreOpen,
                            onDismissRequest = { moreOpen = false },
                            modifier = Modifier.background(NetflixDarkSurface)
                        ) {
                            overflow.forEach { dest ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(dest.labelRes),
                                            color = if (currentDestination == dest) NetflixWhite else NetflixLightGray,
                                            fontWeight = if (currentDestination == dest) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        moreOpen = false
                                        onDestinationSelected(dest)
                                    }
                                )
                            }
                        }
                    }
                },
                label = {
                    Text(
                        text = stringResource(R.string.nav_more),
                        fontSize = 10.sp,
                        fontWeight = if (overflowSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NetflixWhite,
                    selectedTextColor = NetflixWhite,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = NetflixLightGray,
                    unselectedTextColor = NetflixLightGray
                )
            )
        }
    }
}
