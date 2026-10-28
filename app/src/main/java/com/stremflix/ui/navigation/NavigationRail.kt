package com.stremflix.ui.navigation

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremflix.R
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixDarkSurface
import com.stremflix.ui.theme.NetflixDimens
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite

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
@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(4.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NetflixDimens.RailItemHeight)
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(
                color = when {
                    focused -> NetflixWhite.copy(alpha = 0.12f)
                    selected -> NetflixDarkSurface
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixWhite else Color.Transparent,
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
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
            tint = if (selected || focused) NetflixWhite else NetflixLightGray,
            modifier = Modifier.size(22.dp)
        )

        if (expanded) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = if (selected || focused) NetflixWhite else NetflixLightGray,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StremFlixNavRail(
    currentDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    onReloadRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var railHasFocus by remember { mutableStateOf(false) }
    // On launch Compose parks the initial D-pad focus on the first focusable in
    // the tree, which is this rail (home content is composed first but has no
    // focusable until the hero loads). Expanding on that first focus made the
    // app open with the sidebar already open. Focus is still accepted
    // immediately - only the expansion animation waits for the hand-off to the
    // hero, so a deliberate visit still expands normally.
    var expansionArmed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500)
        expansionArmed = true
    }
    val isRailFocused = railHasFocus && expansionArmed
    val railWidth by animateDpAsState(
        targetValue = if (isRailFocused) NetflixDimens.RailWidthExpanded else NetflixDimens.RailWidthCollapsed,
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

    Box(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .statusBarsPadding()
            .onFocusChanged { railHasFocus = it.hasFocus }
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
                    Text(
                        text = "N",
                        color = NetflixRed,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp)
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

                destinations.forEach { dest ->
                    RailItem(
                        icon = dest.icon,
                        label = stringResource(dest.labelRes),
                        selected = currentDestination == dest,
                        expanded = isRailFocused,
                        onClick = { onDestinationSelected(dest) }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                RailItem(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.nav_settings),
                    selected = currentDestination == NavDestination.SETTINGS,
                    expanded = isRailFocused,
                    onClick = { onDestinationSelected(NavDestination.SETTINGS) }
                )

                var reloadFocused by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(4.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NetflixDimens.RailItemHeight)
                        .padding(vertical = 2.dp)
                        .clip(shape)
                        .background(if (reloadFocused) NetflixWhite.copy(alpha = 0.12f) else Color.Transparent)
                        .border(
                            width = if (reloadFocused) 2.dp else 0.dp,
                            color = if (reloadFocused) NetflixWhite else Color.Transparent,
                            shape = shape
                        )
                        .onFocusChanged { reloadFocused = it.isFocused }
                        .clickable {
                            onReloadRequested()
                            (context as? Activity)?.recreate()
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(3.dp))
                    Spacer(modifier = Modifier.width(if (isRailFocused) 14.dp else 11.dp))
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.nav_reload),
                        tint = if (reloadFocused) NetflixWhite else NetflixLightGray,
                        modifier = Modifier.size(22.dp)
                    )
                    if (isRailFocused) {
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = stringResource(R.string.nav_reload),
                            color = if (reloadFocused) NetflixWhite else NetflixLightGray,
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

@Composable
fun StremFlixBottomNavBar(
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
