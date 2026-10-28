package com.stremflix.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stremflix.R
import com.stremflix.data.model.ContentRow
import com.stremflix.data.model.MediaItem
import com.stremflix.data.model.SpotlightType
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixBorderHighlight
import com.stremflix.ui.theme.NetflixCardSurface
import com.stremflix.ui.theme.NetflixDarkSurface
import com.stremflix.ui.theme.NetflixDimens
import com.stremflix.ui.theme.NetflixGreenMatch
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixMutedGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite
import com.stremflix.ui.theme.isCompactScreen
import com.stremflix.ui.theme.isCompactWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack),
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetflixRed,
                                contentColor = NetflixWhite
                            ),
                            shape = RoundedCornerShape(NetflixDimens.CardRadius)
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
                        onPlayClicked = { onPlayClicked(heroItem) },
                        onInfoClicked = { onMediaSelected(heroItem) }
                    )
                }
            }

            items(contentRows, key = { it.id }) { row ->
                ContentRowSection(
                    title = row.title,
                    titleRes = row.titleRes,
                    items = row.items,
                    spotlightType = row.spotlightType,
                    onMediaSelected = onMediaSelected
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
            IconButton(onClick = onSearchClicked) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.action_search),
                    tint = NetflixWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onProfileClicked) {
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

    // Netflix opens on content, not chrome. Compose otherwise hands the initial
    // D-pad focus to the first focusable in the tree, which is the navigation
    // rail, so the app started with an expanded sidebar instead of Home.
    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { playFocusRequester.requestFocus() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
    ) {
        AsyncImage(
            model = item.fullBackdropUrl ?: item.fullPosterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark gradients overlay (top, bottom, left)
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                Text(
                    text = "N",
                    color = NetflixRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
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

            Text(
                text = item.overview,
                color = NetflixLightGray,
                fontSize = 12.sp,
                maxLines = if (isPhone) 2 else 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                var playFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onPlayClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (playFocused) NetflixRed else NetflixWhite,
                        contentColor = if (playFocused) NetflixWhite else NetflixBlack
                    ),
                    shape = RoundedCornerShape(NetflixDimens.CardRadius),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier
                        .focusRequester(playFocusRequester)
                        .onFocusChanged { playFocused = it.isFocused }
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

                var infoFocused by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = onInfoClicked,
                    shape = RoundedCornerShape(NetflixDimens.CardRadius),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                if (infoFocused) NetflixWhite else NetflixMutedGray,
                                if (infoFocused) NetflixWhite else NetflixMutedGray
                            )
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (infoFocused) {
                            NetflixWhite.copy(alpha = 0.2f)
                        } else {
                            NetflixDarkSurface.copy(alpha = 0.8f)
                        },
                        contentColor = NetflixWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .onFocusChanged { infoFocused = it.isFocused }
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
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

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
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(NetflixDimens.RowItemSpacing)
        ) {
            itemsIndexed(
                distinctItems,
                key = { _, item -> "${item.mediaType}_${item.id}" }
            ) { index, item ->
                MediaCard(
                    item = item,
                    onClick = { onMediaSelected(item) },
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
    var isFocused by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (isFocused) NetflixDimens.CardFocusScale else 1f,
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .then(
                if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth()
            )
            .aspectRatio(2f / 3f)
            .scale(scaleAnim)
            .clip(RoundedCornerShape(NetflixDimens.CardRadius))
            .background(NetflixCardSurface)
            .border(
                width = if (isFocused) NetflixDimens.CardFocusBorder else 0.dp,
                color = if (isFocused) NetflixWhite else Color.Transparent,
                shape = RoundedCornerShape(NetflixDimens.CardRadius)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.fullPosterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (rank != null || originalBadge) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                NetflixBlack.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }

        if (originalBadge) {
            Text(
                text = "N",
                color = NetflixRed,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 4.dp)
            )
        }

        if (rank != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
            ) {
                Text(
                    text = stringResource(
                        if (rank <= 10) R.string.badge_top_10 else R.string.badge_top_250
                    ),
                    color = NetflixWhite.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "$rank",
                    color = NetflixWhite,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 42.sp
                )
            }
        }
    }
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
