@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.tv.material3.ExperimentalTvMaterial3Api::class
)

package com.flick.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.BorderStroke
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flick.app.R
import com.flick.app.ui.components.touchClickable
import com.flick.app.data.model.ContentRow
import com.flick.app.data.model.MediaItem
import com.flick.app.data.model.SpotlightType
import com.flick.app.ui.components.MobileHeroActionButton
import com.flick.app.ui.components.MobilePrimaryButton
import com.flick.app.ui.components.NetflixPosterCard
import com.flick.app.ui.components.railFullBleed
import com.flick.app.ui.theme.NetflixBlack
import com.flick.app.ui.theme.NetflixBorderHighlight
import com.flick.app.ui.theme.NetflixCardSurface
import com.flick.app.ui.theme.NetflixDarkSurface
import com.flick.app.ui.theme.NetflixDimens
import com.flick.app.ui.theme.NetflixGreenMatch
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixMutedGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.theme.NetflixWhite
import com.flick.app.ui.theme.isCompactScreen
import com.flick.app.ui.theme.isCompactWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

@Composable
fun HomeScreen(
    contentRows: List<ContentRow>,
    heroItem: MediaItem?,
    onMediaSelected: (MediaItem) -> Unit,
    onPlayClicked: (MediaItem) -> Unit,
    isTmdbKeyConfigured: Boolean = true,
    onNavigateToSettings: () -> Unit = {},
    onSelectFilter: (String) -> Unit = {},
    activeFilter: String = "All",
    onSearchClicked: () -> Unit = {},
    showFilterChips: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isPhone = isCompactScreen()
    val horizontalPadding =
        if (isPhone) NetflixDimens.RowContentPaddingMobile else NetflixDimens.RowContentPadding

    // One FocusRequester per card, per row. UP/DOWN between rows is routed to the
    // card at the same index in the neighbouring row instead of being left to the
    // 2D focus search: a row that is scrolled or simply shorter than the one below
    // leaves empty space under the beam, and the search then continues upwards
    // until it hits the hero synopsis box (N7). Requesters are cheap nodes; unlike
    // per-item `focusProperties` they are never consulted during focus search.
    val cardRequestersByRow = remember(contentRows) {
        contentRows.map { row -> List(row.items.size) { FocusRequester() } }
    }

    // The hero's default focus is owned by the screen, not by HeroSection. The
    // hero is a LazyColumn item, so it is disposed whenever it scrolls out of the
    // viewport and composed again when it scrolls back into it. A LaunchedEffect
    // living inside HeroSection therefore ran again on every re-entry and
    // re-requested focus on the synopsis, so UP from any row below jumped into the
    // hero instead of moving one row up (N7). Guarded here, once per hero, it can
    // no longer fire again after the first successful request.
    val heroFocusRequester = remember { FocusRequester() }
    val overviewFocusRequester = remember { FocusRequester() }
    val heroId = heroItem?.id
    var hasRequestedInitialFocus by remember(heroId) { mutableStateOf(false) }
    LaunchedEffect(heroId) {
        if (heroItem == null || hasRequestedInitialFocus) return@LaunchedEffect
        // Land on the synopsis when the item has one: focusing it is what reveals
        // the full billboard, so defaulting to Play left the full hero view hidden
        // on load (D1). Play stays the default when there is no synopsis. Retry
        // across frames because the hero is not attached on the first composition,
        // and check the boolean requestFocus() returns: it reports false without
        // throwing while the node is not ready, so runCatching's flag alone quits
        // too early and leaves focus on whatever the tree supplied first.
        val initialFocus =
            if (heroItem.overview.isNotBlank()) overviewFocusRequester else heroFocusRequester
        repeat(6) {
            withFrameNanos { }
            if (runCatching { initialFocus.requestFocus() }.getOrDefault(false)) {
                hasRequestedInitialFocus = true
                return@LaunchedEffect
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .focusRestorer(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Netflix 2020 phone chrome: the wordmark plus search/profile on every
        // tab. It used to be hidden unless the active filter was "All", so the
        // Movies, TV Shows and New & Popular tabs lost their header entirely.
        if (isPhone) {
            item {
                MobileTopBar(
                    onSearchClicked = onSearchClicked,
                    onProfileClicked = onNavigateToSettings
                )
            }
        }

        // The "Browse" pills are the first row of the Netflix home feed.
        if (isPhone && showFilterChips) {
            item {
                FilterChipRow(
                    activeFilter = activeFilter,
                    onSelectFilter = onSelectFilter,
                    horizontalPadding = horizontalPadding
                )
            }
        }

        // Empty state prompt when TMDB Key is missing
        if (!isTmdbKeyConfigured && contentRows.isEmpty() && heroItem == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(NetflixCardSurface, RoundedCornerShape(NetflixDimens.BigCardRadius))
                            .border(
                                1.dp,
                                NetflixBorderHighlight,
                                RoundedCornerShape(NetflixDimens.BigCardRadius)
                            )
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = NetflixRed,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.tmdb_key_required_title),
                            color = NetflixWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tmdb_key_required_desc),
                            color = NetflixLightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.colors(
                                containerColor = NetflixRed,
                                contentColor = NetflixWhite
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(NetflixDimens.CardRadius)),
                            modifier = Modifier.touchClickable { onNavigateToSettings() }
                        ) {
                            Text(
                                text = stringResource(R.string.btn_go_to_settings),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            if (heroItem != null) {
                item {
                    HeroSection(
                        item = heroItem,
                        heroFocusRequester = heroFocusRequester,
                        overviewFocusRequester = overviewFocusRequester,
                        onPlayClicked = { onPlayClicked(heroItem) },
                        onInfoClicked = { onMediaSelected(heroItem) }
                    )
                }
            }

            itemsIndexed(
                contentRows,
                key = { _, row -> row.id },
                contentType = { _, _ -> "row" }
            ) { rowIndex, row ->
                ContentRowSection(
                    title = row.title,
                    titleRes = row.titleRes,
                    items = row.items,
                    spotlightType = row.spotlightType,
                    onMediaSelected = onMediaSelected,
                    cardRequesters = cardRequestersByRow.getOrNull(rowIndex),
                    // getOrNull(-1) is null, so the first row keeps its current
                    // spatial UP: the billboard is directly above it and that path
                    // already works.
                    aboveCards = cardRequestersByRow.getOrNull(rowIndex - 1),
                    belowCards = cardRequestersByRow.getOrNull(rowIndex + 1)
                )
            }
        }
    }
}

/**
 * Phone-only top bar. Netflix 2020 kept the wordmark in the top-left and the
 * search/profile entries in the top-right across every browse tab.
 */
@Composable
private fun MobileTopBar(
    onSearchClicked: () -> Unit,
    onProfileClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NetflixBlack.copy(alpha = 0.96f),
                        NetflixBlack.copy(alpha = 0.72f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.app_wordmark),
            color = NetflixRed,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.6).sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSearchClicked,
                modifier = Modifier.touchClickable { onSearchClicked() }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.action_search),
                    tint = NetflixWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(
                onClick = onProfileClicked,
                modifier = Modifier.touchClickable { onProfileClicked() }
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = NetflixWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterChipRow(
    activeFilter: String,
    onSelectFilter: (String) -> Unit,
    horizontalPadding: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = horizontalPadding, top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val chips = listOf(
            stringResource(R.string.filter_tv_shows) to "TV Shows",
            stringResource(R.string.filter_movies) to "Movies",
            stringResource(R.string.nav_categories) to "Categories"
        )
        chips.forEach { (label, id) ->
            FilterHeaderChip(
                text = label,
                isSelected = activeFilter == id,
                onClick = { onSelectFilter(id) }
            )
        }
    }
}

@Composable
fun FilterHeaderChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (isSelected) NetflixWhite else NetflixLightGray,
        fontSize = 14.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(NetflixDimens.CardRadius))
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 6.dp)
    )
}

@Composable
fun HeroSection(
    item: MediaItem,
    heroFocusRequester: FocusRequester,
    overviewFocusRequester: FocusRequester,
    onPlayClicked: () -> Unit,
    onInfoClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isPhone = isCompactScreen()
    // A landscape phone is only ~411dp tall, so the 440dp 10-foot billboard used
    // to overflow the window and push the rows off-screen.
    val heroHeight = when {
        // Leave part of the first row visible under the phone top bar.
        configuration.screenHeightDp < 480 -> 220.dp
        isCompactWidth() -> 366.dp
        else -> 440.dp
    }

    val startPadding = if (isPhone) 16.dp else 36.dp
    val endPadding = if (isPhone) 48.dp else 200.dp

    // Netflix opens on content, not chrome: the screen, not this item, requests
    // the hero's default focus (see HomeScreen), so the request cannot be repeated
    // every time this item scrolls back into view.
    val hasOverview = item.overview.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
    ) {
        AsyncImage(
            model = item.fullBackdropUrl ?: item.fullPosterUrl,
            contentDescription = item.title,
            // On TV the billboard runs under the navigation rail; on mobile it
            // simply fills the section.
            modifier = if (isPhone) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxSize().railFullBleed()
            },
            contentScale = ContentScale.Crop
        )

        if (isPhone) {
            PhoneHeroContent(
                item = item,
                onPlayClicked = onPlayClicked,
                onInfoClicked = onInfoClicked,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Dark gradients overlay (top, bottom, left). They are full-bleed
            // with the artwork so the composite stays continuous where it runs
            // under the rail.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .railFullBleed()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                NetflixBlack.copy(alpha = 0.5f),
                                NetflixBlack
                            ),
                            startY = 100f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .railFullBleed()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                NetflixBlack.copy(alpha = 0.9f),
                                NetflixBlack.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            endX = 800f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = startPadding, bottom = 18.dp, end = endPadding),
                verticalArrangement = Arrangement.Bottom
            ) {
                // "N SERIES" eyebrow, exactly where Netflix puts it: above the logo.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.logo_flick),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.mediaType == "tv") {
                            stringResource(R.string.label_series)
                        } else {
                            stringResource(R.string.label_film)
                        },
                        color = NetflixLightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Netflix never set a plain text headline on its billboards: it used
                // the title's transparent logo art. Text is the fallback only when
                // TMDB has no logo for the title.
                if (!item.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isPhone) 58.dp else 96.dp),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart
                    )
                } else {
                    Text(
                        text = item.title,
                        color = NetflixWhite,
                        fontSize = if (isPhone) 26.sp else 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.label_match, item.matchPercentage),
                        color = NetflixGreenMatch,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.releaseYear,
                        color = NetflixLightGray,
                        fontSize = 12.sp
                    )
                    if (item.contentRating.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        BadgeTag(text = item.contentRating)
                    }
                    if (item.durationOrSeasons.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.durationOrSeasons,
                            color = NetflixLightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (hasOverview) {
                    // Focusable synopsis, mirroring the detail page. It is the top of
                    // the feed, so it cancels UP: focus used to fall through to the
                    // navigation rail from here and strand the viewer there, and OK
                    // expands the full text in place.
                    var overviewExpanded by remember(item.id) { mutableStateOf(false) }
                    var overviewFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (overviewFocused) NetflixWhite.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (overviewFocused) 2.dp else 0.dp,
                                color = if (overviewFocused) NetflixWhite else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .onFocusChanged { overviewFocused = it.isFocused }
                            .focusRequester(overviewFocusRequester)
                            .focusProperties {
                                up = FocusRequester.Cancel
                                down = heroFocusRequester
                            }
                            .clickable { overviewExpanded = !overviewExpanded }
                            .padding(6.dp)
                    ) {
                        Text(
                            text = item.overview,
                            color = NetflixLightGray,
                            fontSize = 12.sp,
                            maxLines = if (overviewExpanded) Int.MAX_VALUE else if (isPhone) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onPlayClicked,
                        modifier = Modifier
                            .focusRequester(heroFocusRequester)
                            .focusProperties {
                                up = if (hasOverview) overviewFocusRequester else FocusRequester.Cancel
                            }
                            .touchClickable { onPlayClicked() },
                        colors = ButtonDefaults.colors(
                            containerColor = NetflixWhite,
                            contentColor = NetflixBlack,
                            focusedContainerColor = NetflixRed,
                            focusedContentColor = NetflixWhite
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(NetflixDimens.CardRadius)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_play),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = onInfoClicked,
                        modifier = Modifier.focusProperties {
                            // Same guard as Play: without it UP from the billboard
                            // escaped into the sidebar.
                            up = if (hasOverview) overviewFocusRequester else FocusRequester.Cancel
                        }.touchClickable { onInfoClicked() },
                        shape = OutlinedButtonDefaults.shape(RoundedCornerShape(NetflixDimens.CardRadius)),
                        border = OutlinedButtonDefaults.border(
                            border = Border(
                                BorderStroke(1.dp, NetflixMutedGray),
                                shape = RoundedCornerShape(NetflixDimens.CardRadius)
                            ),
                            focusedBorder = Border(
                                BorderStroke(1.5.dp, NetflixWhite),
                                shape = RoundedCornerShape(NetflixDimens.CardRadius)
                            )
                        ),
                        colors = OutlinedButtonDefaults.colors(
                            containerColor = NetflixDarkSurface.copy(alpha = 0.8f),
                            contentColor = NetflixWhite,
                            focusedContainerColor = NetflixWhite.copy(alpha = 0.2f),
                            focusedContentColor = NetflixWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_more_info),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Phone billboard.
 *
 * Netflix's handset layout is centre-aligned and reads top-to-bottom (art, genre
 * tagline, actions) rather than the 10-foot bottom-left column, so this is a
 * separate composition instead of a flag on the TV one. Touch only: nothing here
 * is a focus target, which is also why the phone hero can never strand D-pad
 * focus the way the TV hero could.
 */
@Composable
private fun PhoneHeroContent(
    item: MediaItem,
    onPlayClicked: () -> Unit,
    onInfoClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Soft scrim so the top chrome (logo / search / profile) stays readable.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.6f),
                            0.5f to Color.Transparent
                        )
                    )
                )
        )

        // Long fade that blends the artwork into the first shelf.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.5f to Color.Transparent,
                            1f to NetflixBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.logo_flick),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (item.mediaType == "tv") {
                        stringResource(R.string.label_series)
                    } else {
                        stringResource(R.string.label_film)
                    },
                    color = NetflixLightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(52.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            } else {
                Text(
                    text = item.title,
                    color = NetflixWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The spec's bullet-separated genre tagline; the metadata row is the
            // fallback for titles TMDB gave no genres.
            if (item.genres.isNotEmpty()) {
                Text(
                    text = item.genres.take(4).joinToString(" \u2022 "),
                    color = NetflixLightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.label_match, item.matchPercentage),
                        color = NetflixGreenMatch,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.releaseYear,
                        color = NetflixLightGray,
                        fontSize = 12.sp
                    )
                    if (item.contentRating.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        BadgeTag(text = item.contentRating)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MobilePrimaryButton(
                    label = stringResource(R.string.action_play),
                    onClick = onPlayClicked,
                    height = 36.dp
                )
                MobileHeroActionButton(
                    label = stringResource(R.string.action_more_info),
                    icon = Icons.Default.Info,
                    onClick = onInfoClicked
                )
            }
        }
    }
}

@Composable
fun BadgeTag(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, NetflixMutedGray, RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = NetflixLightGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * One row shape for the whole app.
 *
 * Rows used to pick a completely different card per [SpotlightType] — a 90sp
 * numeral next to a 125dp poster, a 260dp 16:9 trailer tile, and 135dp posters —
 * so stacked rows had three different heights and the shelf looked ragged. Every
 * row now renders the same 2:3 poster at one width; the ranking is an overlay on
 * that poster instead of a second layout.
 */
@Composable
fun ContentRowSection(
    title: String,
    titleRes: Int? = null,
    items: List<MediaItem>,
    spotlightType: SpotlightType = SpotlightType.NONE,
    onMediaSelected: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    cardRequesters: List<FocusRequester>? = null,
    aboveCards: List<FocusRequester>? = null,
    belowCards: List<FocusRequester>? = null
) {
    if (items.isEmpty()) return

    // Index of the card in this row that currently holds focus, or -1. Vertical
    // navigation uses it to pick the column to land on in the neighbouring row.
    var focusedIndex by remember { mutableStateOf(-1) }

    val isPhone = isCompactScreen()
    val horizontalPadding =
        if (isPhone) NetflixDimens.RowContentPaddingMobile else NetflixDimens.RowContentPadding
    val cardWidth = if (isPhone) NetflixDimens.PosterWidthMobile else NetflixDimens.PosterWidthTv
    val displayTitle = title.ifEmpty { titleRes?.let { stringResource(it) } ?: "" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NetflixDimens.RowVerticalSpacing)
    ) {
        Text(
            text = displayTitle,
            color = NetflixWhite,
            fontSize = if (isPhone) 16.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = NetflixDimens.RowTitleSpacing
            )
        )

        val distinctItems = remember(items) { items.distinctBy { "${it.mediaType}_${it.id}" } }
        LazyRow(
            modifier = Modifier
                .focusRestorer()
                // Route UP/DOWN explicitly. The key is consumed only when the
                // request actually landed on a composed card, so a row whose
                // neighbour is scrolled away still falls back to the default
                // spatial move instead of swallowing the press.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val targets = when (event.key) {
                        Key.DirectionUp -> aboveCards
                        Key.DirectionDown -> belowCards
                        else -> null
                    } ?: return@onPreviewKeyEvent false
                    if (targets.isEmpty() || focusedIndex < 0) return@onPreviewKeyEvent false
                    val target = targets[focusedIndex.coerceAtMost(targets.lastIndex)]
                    runCatching { target.requestFocus() }.getOrDefault(false)
                },
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(NetflixDimens.RowItemSpacing)
        ) {
            itemsIndexed(
                distinctItems,
                key = { _, item -> "${item.mediaType}_${item.id}" },
                contentType = { _, _ -> "card" }
            ) { index, item ->
                MediaCard(
                    item = item,
                    onClick = { onMediaSelected(item) },
                    modifier = Modifier
                        // Reserve the focus-scale overflow so a focused card never
                        // forces the parent list to re-scroll (vertical jitter).
                        .padding(vertical = NetflixDimens.CardFocusOverflow)
                        .onFocusChanged { if (it.isFocused) focusedIndex = index }
                        .then(
                            cardRequesters?.getOrNull(index)?.let { Modifier.focusRequester(it) }
                                ?: Modifier
                        ),
                    cardWidth = cardWidth,
                    rank = if (spotlightType == SpotlightType.TOP_250) index + 1 else null,
                    originalBadge = spotlightType == SpotlightType.N_SERIES
                )
            }
        }
    }
}

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = NetflixDimens.PosterWidth,
    rank: Int? = null,
    originalBadge: Boolean = false
) {
    NetflixPosterCard(
        posterUrl = item.fullPosterUrl,
        title = item.title,
        onClick = onClick,
        modifier = modifier,
        cardWidth = cardWidth,
        rank = rank,
        originalBadge = originalBadge
    )
}

@Composable
fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = NetflixDimens.PosterWidth
) {
    MediaCard(item = item, onClick = onClick, modifier = modifier, cardWidth = cardWidth)
}
