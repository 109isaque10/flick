@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.tv.material3.ExperimentalTvMaterial3Api::class
)

package com.flick.app.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.flick.app.R
import com.flick.app.data.model.EpisodeModel
import com.flick.app.data.model.MediaItem
import com.flick.app.data.model.SeasonModel
import com.flick.app.ui.components.MobilePrimaryButton
import com.flick.app.ui.components.MobileSecondaryButton
import com.flick.app.ui.components.NetflixEpisodeThumbnailCard
import com.flick.app.ui.components.NetflixSelectionButton
import com.flick.app.ui.components.TvSpinner
import com.flick.app.ui.components.railFullBleed
import com.flick.app.ui.components.touchClickable
import com.flick.app.ui.home.BadgeTag
import com.flick.app.ui.theme.NetflixBlack
import com.flick.app.ui.theme.NetflixBorderHighlight
import com.flick.app.ui.theme.NetflixDarkSurface
import com.flick.app.ui.theme.NetflixDimens
import com.flick.app.ui.theme.NetflixGreenMatch
import com.flick.app.ui.theme.NetflixGoldRating
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixMutedGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.theme.NetflixWhite
import com.flick.app.ui.theme.isCompactScreen

@Composable
fun DetailScreen(
    mediaItem: MediaItem,
    isWatchlisted: Boolean,
    watchProgress: Float, // 0f to 1f
    seasons: List<SeasonModel>,
    episodes: List<EpisodeModel>,
    selectedSeason: SeasonModel?,
    onSeasonSelected: (SeasonModel) -> Unit,
    onPlayClick: (season: Int?, episode: Int?) -> Unit,
    onToggleWatchlist: () -> Unit,
    onBack: () -> Unit,
    onPlayFromBeginning: (() -> Unit)? = null,
    onPlayTrailer: (() -> Unit)? = null,
    smartNextSeason: Int? = null,
    smartNextEpisode: Int? = null,
    episodeProgressMap: Map<String, Float> = emptyMap(),
    completionThreshold: Int = 90,
    selectedProviders: Set<String> = setOf("IMDb", "TMDB", "Rotten Tomatoes", "Metacritic", "Netflix"),
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    // Height as well as width matters: a landscape phone is ~823dp wide but only
    // ~411dp tall, so the two-pane TV layout used to clip its action column.
    // Small tablets also lack room for a fixed 480dp left pane plus artwork.
    val isMobile = isCompactScreen() || LocalConfiguration.current.screenWidthDp < 960

    if (isMobile) {
        MobileDetailContent(
            mediaItem = mediaItem,
            isWatchlisted = isWatchlisted,
            watchProgress = watchProgress,
            seasons = seasons,
            episodes = episodes,
            selectedSeason = selectedSeason,
            onSeasonSelected = onSeasonSelected,
            onPlayClick = onPlayClick,
            onToggleWatchlist = onToggleWatchlist,
            onPlayTrailer = onPlayTrailer,
            onBack = onBack,
            smartNextSeason = smartNextSeason,
            smartNextEpisode = smartNextEpisode,
            episodeProgressMap = episodeProgressMap,
            completionThreshold = completionThreshold,
            selectedProviders = selectedProviders,
            modifier = modifier
        )
    } else {
        TvDetailContent(
            mediaItem = mediaItem,
            isWatchlisted = isWatchlisted,
            watchProgress = watchProgress,
            seasons = seasons,
            episodes = episodes,
            selectedSeason = selectedSeason,
            onSeasonSelected = onSeasonSelected,
            onPlayClick = onPlayClick,
            onPlayFromBeginning = onPlayFromBeginning,
            onPlayTrailer = onPlayTrailer,
            onToggleWatchlist = onToggleWatchlist,
            onBack = onBack,
            smartNextSeason = smartNextSeason,
            smartNextEpisode = smartNextEpisode,
            episodeProgressMap = episodeProgressMap,
            completionThreshold = completionThreshold,
            selectedProviders = selectedProviders,
            modifier = modifier
        )
    }
}

@Composable
private fun DetailTitleLogo(
    mediaItem: MediaItem,
    height: Dp,
    fallbackFontSize: TextUnit,
    fallbackColor: Color = NetflixWhite
) {
    val logoUrl = mediaItem.logoUrl
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = mediaItem.title,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(height),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart
        )
    } else {
        Text(
            text = mediaItem.title,
            color = fallbackColor,
            fontSize = fallbackFontSize,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MobileDetailContent(
    mediaItem: MediaItem,
    isWatchlisted: Boolean,
    watchProgress: Float,
    seasons: List<SeasonModel>,
    episodes: List<EpisodeModel>,
    selectedSeason: SeasonModel?,
    onSeasonSelected: (SeasonModel) -> Unit,
    onPlayClick: (season: Int?, episode: Int?) -> Unit,
    onToggleWatchlist: () -> Unit,
    onPlayTrailer: (() -> Unit)?,
    onBack: () -> Unit,
    smartNextSeason: Int?,
    smartNextEpisode: Int?,
    episodeProgressMap: Map<String, Float>,
    completionThreshold: Int,
    selectedProviders: Set<String>,
    modifier: Modifier = Modifier
) {
    var seasonDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .focusRestorer()
    ) {
        // Banner Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                AsyncImage(
                    model = mediaItem.fullBackdropUrl ?: mediaItem.fullPosterUrl,
                    contentDescription = mediaItem.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .touchClickable { onBack() }
                        .padding(12.dp)
                        .background(NetflixBlack.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = NetflixWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, NetflixBlack.copy(alpha = 0.7f), NetflixBlack),
                                startY = 100f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 12.dp, end = 16.dp)
                ) {
                    DetailTitleLogo(
                        mediaItem = mediaItem,
                        height = 44.dp,
                        fallbackFontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.label_match, mediaItem.matchPercentage),
                            color = NetflixGreenMatch,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = mediaItem.releaseYear,
                            color = NetflixLightGray,
                            fontSize = 12.sp
                        )
                        if (mediaItem.contentRating.isNotBlank()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            BadgeTag(text = mediaItem.contentRating)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RatingsClusterRow(
                        ratings = mediaItem.ratingsCluster,
                        selectedProviders = selectedProviders
                    )
                }
            }
        }

        // Action Buttons Row & Progress
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playText = if (watchProgress > 0f) {
                        stringResource(R.string.action_resume)
                    } else {
                        stringResource(R.string.action_play)
                    }
                    val playAction = {
                        if (mediaItem.mediaType == "tv") {
                            onPlayClick(smartNextSeason ?: selectedSeason?.seasonNumber ?: 1, smartNextEpisode ?: 1)
                        } else {
                            onPlayClick(null, null)
                        }
                    }

                    MobilePrimaryButton(
                        label = playText,
                        onClick = playAction,
                        progress = watchProgress,
                        modifier = Modifier.weight(1.6f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    MobileSecondaryButton(
                        label = stringResource(R.string.action_my_list),
                        onClick = onToggleWatchlist,
                        icon = if (isWatchlisted) Icons.Default.Check else Icons.Default.Add,
                        modifier = Modifier.weight(1f)
                    )

                    if (onPlayTrailer != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        MobileSecondaryButton(
                            label = stringResource(R.string.label_trailer),
                            onClick = onPlayTrailer
                        )
                    }
                }
            }
        }

        // Synopsis
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = mediaItem.overview,
                    color = NetflixLightGray,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                if (mediaItem.cast.isNotEmpty() || mediaItem.director.isNotBlank() ||
                    mediaItem.creators.isNotEmpty() || mediaItem.genres.isNotEmpty()
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (mediaItem.cast.isNotEmpty()) {
                            DetailInfoRow(
                                icon = Icons.Default.Person,
                                text = stringResource(
                                    R.string.detail_cast_prefix,
                                    mediaItem.cast.take(4).joinToString(", ")
                                )
                            )
                        }
                        if (mediaItem.creators.isNotEmpty()) {
                            DetailInfoRow(
                                icon = Icons.Default.Movie,
                                text = stringResource(
                                    R.string.detail_creators_prefix,
                                    mediaItem.creators.take(3).joinToString(", ")
                                )
                            )
                        }
                        if (mediaItem.director.isNotBlank()) {
                            DetailInfoRow(
                                icon = Icons.Default.Movie,
                                text = stringResource(R.string.detail_directors_prefix, mediaItem.director)
                            )
                        }
                        if (mediaItem.genres.isNotEmpty()) {
                            DetailInfoRow(
                                icon = Icons.Default.Category,
                                text = stringResource(
                                    R.string.detail_genres_prefix,
                                    mediaItem.genres.take(3).joinToString(" / ")
                                )
                            )
                        }
                    }
                }
            }
        }

        // TV Show Season Selector & Episode List
        if (mediaItem.mediaType == "tv") {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.title_episodes),
                            color = NetflixWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (seasons.isNotEmpty()) {
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { seasonDropdownExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = selectedSeason?.name ?: stringResource(R.string.season_format, 1),
                                        color = NetflixWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "▾",
                                        color = NetflixLightGray,
                                        fontSize = 12.sp
                                    )
                                }

                                DropdownMenu(
                                    expanded = seasonDropdownExpanded,
                                    onDismissRequest = { seasonDropdownExpanded = false },
                                    modifier = Modifier.background(NetflixDarkSurface)
                                ) {
                                    seasons.forEach { season ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${season.name} (${season.episodeCount})",
                                                    color = NetflixWhite
                                                )
                                            },
                                            onClick = {
                                                onSeasonSelected(season)
                                                seasonDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (seasons.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TvSpinner(color = NetflixRed, size = 20.dp, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.loading_seasons),
                                color = NetflixLightGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (episodes.isNotEmpty()) {
                val distinctEpisodes = episodes.distinctBy { it.id }
                itemsIndexed(distinctEpisodes, key = { _, episode -> episode.id }) { _, episode ->
                    val progress = episodeProgressMap["s${episode.seasonNumber}_e${episode.episodeNumber}"] ?: 0f
                    val isWatched = progress >= (completionThreshold / 100f)
                    val isWatching = progress > 0.05f && !isWatched

                    EpisodeCardItem(
                        episode = episode,
                        isMobile = true,
                        screenPadding = 16.dp,
                        progress = progress,
                        isWatched = isWatched,
                        isWatching = isWatching,
                        onClick = { onPlayClick(episode.seasonNumber, episode.episodeNumber) }
                    )
                }
            } else if (seasons.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        TvSpinner(color = NetflixRed, size = 20.dp, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.loading_episodes),
                            color = NetflixLightGray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvDetailContent(
    mediaItem: MediaItem,
    isWatchlisted: Boolean,
    watchProgress: Float,
    seasons: List<SeasonModel>,
    episodes: List<EpisodeModel>,
    selectedSeason: SeasonModel?,
    onSeasonSelected: (SeasonModel) -> Unit,
    onPlayClick: (season: Int?, episode: Int?) -> Unit,
    onPlayFromBeginning: (() -> Unit)?,
    onPlayTrailer: (() -> Unit)?,
    onToggleWatchlist: () -> Unit,
    onBack: () -> Unit,
    smartNextSeason: Int?,
    smartNextEpisode: Int?,
    episodeProgressMap: Map<String, Float>,
    completionThreshold: Int,
    selectedProviders: Set<String>,
    modifier: Modifier = Modifier
) {
    var showMoreEpisodesView by remember { mutableStateOf(false) }

    // Landing focus for the detail page: the primary action is what a viewer
    // wants, and starting elsewhere forced a D-pad hunt on every open.
    val playFocusRequester = remember { FocusRequester() }
    // Hoisted so the page can settle at its bottom after focus lands: a plain
    // focus request centres the Play action and scrolls the title/synopsis out of
    // frame, which is the opposite of what the 10-foot layout wants. Play should
    // sit at the bottom with the whole info block visible above it.
    val detailScrollState = rememberScrollState()
    LaunchedEffect(mediaItem.id, showMoreEpisodesView) {
        if (!showMoreEpisodesView) {
            // Retry across frames: the page (and the Play button) is not attached
            // on the first frame, and a single attempt used to fall through to
            // whatever focusable the tree supplied, landing on the upper chrome.
            // Check the boolean requestFocus() returns: it reports false without
            // throwing while the node is not ready.
            var focusGained = false
            repeat(6) {
                if (focusGained) return@repeat
                withFrameNanos { }
                focusGained =
                    runCatching { playFocusRequester.requestFocus() }.getOrDefault(false)
            }
            // Let the focus-driven scroll settle, then pin the column to its
            // bottom so Play sits at the bottom of the viewport.
            repeat(2) { withFrameNanos { } }
            runCatching { detailScrollState.scrollTo(detailScrollState.maxValue) }
        }
    }

    BackHandler(enabled = showMoreEpisodesView) {
        showMoreEpisodesView = false
    }

    if (showMoreEpisodesView && mediaItem.mediaType == "tv") {
        TvEpisodesView(
            mediaItem = mediaItem,
            seasons = seasons,
            episodes = episodes,
            selectedSeason = selectedSeason,
            onSeasonSelected = onSeasonSelected,
            onPlayClick = onPlayClick,
            onBack = { showMoreEpisodesView = false },
            episodeProgressMap = episodeProgressMap,
            completionThreshold = completionThreshold
        )
    } else {
        val isTv = mediaItem.mediaType == "tv"
        val resumeSeason = smartNextSeason ?: selectedSeason?.seasonNumber
        val playLabel = when {
            watchProgress > 0f && isTv && resumeSeason != null && smartNextEpisode != null ->
                stringResource(R.string.action_resume_episode_format, resumeSeason, smartNextEpisode!!)
            watchProgress > 0f -> stringResource(R.string.action_resume)
            else -> stringResource(R.string.action_play)
        }
        val fromBeginning = onPlayFromBeginning

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
        ) {
            // The artwork is the page background, not a nested showcase card.
            // TV: it runs full-bleed so it continues underneath the nav rail.
            AsyncImage(
                model = mediaItem.fullBackdropUrl ?: mediaItem.fullPosterUrl,
                contentDescription = mediaItem.title,
                modifier = Modifier.fillMaxSize().railFullBleed(),
                contentScale = ContentScale.Crop
            )

            // Left scrim keeps the copy column readable over any artwork. Its
            // stops are relative to the full screen so it lines up with the
            // full-bleed artwork instead of stopping at the rail.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .railFullBleed()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to NetflixBlack.copy(alpha = 0.97f),
                                0.38f to NetflixBlack.copy(alpha = 0.90f),
                                0.68f to NetflixBlack.copy(alpha = 0.35f),
                                1f to Color.Transparent
                            )
                        )
                    )
            )
            // Bottom fade blends the artwork into the page background.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .railFullBleed()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.35f to Color.Transparent,
                                0.72f to NetflixBlack.copy(alpha = 0.75f),
                                1f to NetflixBlack
                            )
                        )
                    )
            )
            // Top fade keeps the back affordance legible.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .railFullBleed()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to NetflixBlack.copy(alpha = 0.70f),
                                0.22f to Color.Transparent
                            )
                        )
                    )
            )

            // Pinned back affordance; the copy column scrolls beneath it.
            // TV viewers press the remote's Back key, and a visible Back button
            // here is both redundant and the first focusable in the tree, so the
            // initial focus used to land on it (and it clipped the title copy).
            // Keep it only for touch devices.
            if (isCompactScreen()) {
                var backFocused by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 56.dp, top = 16.dp)
                        .background(
                            if (backFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .border(if (backFocused) 2.dp else 0.dp, NetflixBorderHighlight, RoundedCornerShape(20.dp))
                        .onFocusChanged { backFocused = it.isFocused }
                        .clickable { onBack() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = NetflixWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_back),
                        color = NetflixWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 560.dp)
                    .statusBarsPadding()
                    .padding(start = 56.dp, end = 40.dp, top = 88.dp, bottom = 36.dp)
                    .verticalScroll(detailScrollState)
            ) {
                DetailTitleLogo(
                    mediaItem = mediaItem,
                    height = 96.dp,
                    fallbackFontSize = 44.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                NetflixTvMetadataRow(
                    mediaItem = mediaItem,
                    selectedProviders = selectedProviders
                )

                if (mediaItem.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    // The synopsis is truncated to keep the hero compact, but it is
                    // also focusable: OK on it expands the full text in place, so a
                    // viewer can read the whole description without leaving the page.
                    var overviewExpanded by remember(mediaItem.id) { mutableStateOf(false) }
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
                            .clickable { overviewExpanded = !overviewExpanded }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = mediaItem.overview,
                            color = NetflixLightGray,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            maxLines = if (overviewExpanded) Int.MAX_VALUE else 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (mediaItem.cast.isNotEmpty() || mediaItem.director.isNotBlank() ||
                    mediaItem.creators.isNotEmpty() || mediaItem.genres.isNotEmpty()
                ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (mediaItem.cast.isNotEmpty()) {
                            DetailInfoRow(
                                icon = Icons.Default.Person,
                                text = stringResource(
                                    R.string.detail_cast_prefix,
                                    mediaItem.cast.take(4).joinToString(", ")
                                )
                            )
                        }
                        if (mediaItem.creators.isNotEmpty()) {
                            DetailInfoRow(
                                icon = Icons.Default.Movie,
                                text = stringResource(
                                    R.string.detail_creators_prefix,
                                    mediaItem.creators.take(3).joinToString(", ")
                                )
                            )
                        }
                        if (mediaItem.director.isNotBlank()) {
                            DetailInfoRow(
                                icon = Icons.Default.Movie,
                                text = stringResource(R.string.detail_directors_prefix, mediaItem.director)
                            )
                        }
                        if (mediaItem.genres.isNotEmpty()) {
                            DetailInfoRow(
                                icon = Icons.Default.Category,
                                text = stringResource(
                                    R.string.detail_genres_prefix,
                                    mediaItem.genres.take(3).joinToString(" / ")
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                NetflixPrimaryAction(
                    label = playLabel,
                    progress = watchProgress.coerceIn(0f, 1f),
                    onClick = {
                        if (isTv) {
                            onPlayClick(
                                smartNextSeason ?: selectedSeason?.seasonNumber ?: 1,
                                smartNextEpisode ?: 1
                            )
                        } else {
                            onPlayClick(null, null)
                        }
                    },
                    modifier = Modifier
                        .focusRequester(playFocusRequester)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (watchProgress > 0f && fromBeginning != null) {
                        NetflixSecondaryAction(
                            icon = Icons.Default.Replay,
                            label = stringResource(R.string.action_play_from_beginning),
                            onClick = fromBeginning
                        )
                    }
                    if (isTv) {
                        NetflixSecondaryAction(
                            icon = Icons.Default.PlaylistPlay,
                            label = stringResource(R.string.action_more_episodes),
                            onClick = { showMoreEpisodesView = true }
                        )
                    }
                    if (mediaItem.trailerKey != null && onPlayTrailer != null) {
                        NetflixSecondaryAction(
                            icon = Icons.Default.PlayArrow,
                            label = stringResource(R.string.action_play_trailer),
                            onClick = onPlayTrailer
                        )
                    }
                    NetflixSecondaryAction(
                        icon = if (isWatchlisted) Icons.Default.Check else Icons.Default.Add,
                        label = if (isWatchlisted) {
                            stringResource(R.string.action_in_list)
                        } else {
                            stringResource(R.string.action_my_list)
                        },
                        onClick = onToggleWatchlist
                    )
                }
            }
        }
    }
}

@Composable
private fun NetflixTvMetadataRow(
    mediaItem: MediaItem,
    selectedProviders: Set<String>
) {
    val ratings = mediaItem.ratingsCluster
    val hasRatings = !ratings.imdbScore.isNullOrBlank() ||
            !ratings.tmdbScore.isNullOrBlank() ||
            !ratings.rottenTomatoesScore.isNullOrBlank() ||
            !ratings.metacriticScore.isNullOrBlank()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.label_match, mediaItem.matchPercentage),
                color = NetflixGreenMatch,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (mediaItem.releaseYear.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = mediaItem.releaseYear,
                    color = NetflixLightGray,
                    fontSize = 14.sp
                )
            }
            if (mediaItem.durationOrSeasons.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = mediaItem.durationOrSeasons,
                    color = NetflixLightGray,
                    fontSize = 14.sp
                )
            }
            if (mediaItem.contentRating.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                MaturityBadge(text = mediaItem.contentRating)
            }
            // Only render badges the item actually carries.
            mediaItem.qualityBadges.forEach { badge ->
                Spacer(modifier = Modifier.width(8.dp))
                BadgeTag(text = badge)
            }
        }
        if (hasRatings) {
            Spacer(modifier = Modifier.height(8.dp))
            RatingsClusterRow(
                ratings = ratings,
                selectedProviders = selectedProviders
            )
        }
    }
}

@Composable
private fun MaturityBadge(text: String) {
    // Netflix tints film certificates amber and TV ratings neutral grey.
    val accent = if (text.startsWith("TV", ignoreCase = true)) NetflixLightGray else NetflixGoldRating
    Text(
        text = text,
        color = accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .border(1.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NetflixMutedGray,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = NetflixLightGray,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NetflixPrimaryAction(
    label: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NetflixSelectionButton(
        label = label,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        icon = Icons.Default.PlayArrow,
        progress = progress
    )
}

@Composable
private fun NetflixSecondaryAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NetflixSelectionButton(
        label = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        icon = icon
    )
}

@Composable
fun TvEpisodesView(
    mediaItem: MediaItem,
    seasons: List<SeasonModel>,
    episodes: List<EpisodeModel>,
    selectedSeason: SeasonModel?,
    onSeasonSelected: (SeasonModel) -> Unit,
    onPlayClick: (season: Int?, episode: Int?) -> Unit,
    onBack: () -> Unit,
    episodeProgressMap: Map<String, Float>,
    completionThreshold: Int,
    modifier: Modifier = Modifier
) {
    // Opening the season list should land on the season already being watched.
    val seasonListState = rememberLazyListState()
    val selectedSeasonRequester = remember { FocusRequester() }

    LaunchedEffect(selectedSeason?.seasonNumber, seasons) {
        val index = seasons.indexOfFirst { it.seasonNumber == selectedSeason?.seasonNumber }
        if (index >= 0) {
            runCatching { seasonListState.scrollToItem(index) }
        }
        repeat(6) {
            withFrameNanos { }
            if (runCatching { selectedSeasonRequester.requestFocus() }.getOrDefault(false)) {
                return@LaunchedEffect
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // High-res Background Artwork (full-bleed under the TV nav rail)
        AsyncImage(
            model = mediaItem.fullBackdropUrl ?: mediaItem.fullPosterUrl,
            contentDescription = mediaItem.title,
            modifier = Modifier.fillMaxSize().railFullBleed(),
            contentScale = ContentScale.Crop
        )

        // Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .railFullBleed()
                .background(NetflixBlack.copy(alpha = 0.90f))
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // Left Season Selector Pane (Matching Episodes.jpg)
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                var backFocused by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (backFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .border(if (backFocused) 2.dp else 0.dp, NetflixBorderHighlight, RoundedCornerShape(20.dp))
                        .onFocusChanged { backFocused = it.isFocused }
                        .clickable { onBack() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = NetflixWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_back),
                        color = NetflixWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailTitleLogo(
                    mediaItem = mediaItem,
                    height = 44.dp,
                    fallbackFontSize = 28.sp,
                    fallbackColor = NetflixRed
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.label_match, mediaItem.matchPercentage),
                        color = NetflixGreenMatch,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = mediaItem.releaseYear, color = NetflixLightGray, fontSize = 12.sp)
                    // No invented age rating: the badge is omitted when TMDB has none.
                    if (mediaItem.contentRating.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = mediaItem.contentRating,
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val seasonsCount = seasons.size
                    if (seasonsCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (seasonsCount == 1) {
                                stringResource(R.string.season_format, 1)
                            } else {
                                stringResource(R.string.seasons_count_format, seasonsCount)
                            },
                            color = NetflixLightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.title_seasons),
                    color = NetflixWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                LazyColumn(
                    state = seasonListState,
                    // Mirror of the rail: the group resolves the season being
                    // watched synchronously during focus search, so focus lands
                    // there on entry. The scroll above is still needed because
                    // that row may be outside the lazily composed window.
                    modifier = Modifier
                        .weight(1f)
                        .focusProperties { enter = { selectedSeasonRequester } }
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(seasons, key = { it.seasonNumber }) { season ->
                        val isSelected = selectedSeason?.seasonNumber == season.seasonNumber
                        var isSeasonFocused by remember { mutableStateOf(false) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(
                                    if (isSeasonFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(NetflixDimens.CardRadius)
                                )
                                .border(
                                    width = if (isSeasonFocused) 2.dp else 0.dp,
                                    color = if (isSeasonFocused) NetflixWhite else Color.Transparent,
                                    shape = RoundedCornerShape(NetflixDimens.CardRadius)
                                )
                                .onFocusChanged { isSeasonFocused = it.isFocused }
                                .then(
                                    if (isSelected || (selectedSeason == null && seasons.firstOrNull()?.seasonNumber == season.seasonNumber)) {
                                        Modifier.focusRequester(selectedSeasonRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSeasonSelected(season) }
                                .padding(horizontal = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(22.dp)
                                    .background(
                                        if (isSelected) NetflixRed else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${season.name} (${
                                    stringResource(
                                        R.string.episodes_count_format,
                                        season.episodeCount
                                    )
                                })",
                                color = if (isSelected || isSeasonFocused) NetflixWhite else NetflixLightGray,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected || isSeasonFocused) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Right Episode Selection Pane
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = stringResource(
                        R.string.season_episodes_header_format,
                        selectedSeason?.name
                            ?: seasons.firstOrNull()?.name
                            ?: stringResource(R.string.season_format, 1)
                    ),
                    color = NetflixWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (episodes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TvSpinner(color = NetflixRed, size = 44.dp)
                    }
                } else {
                    val distinctEpisodes = remember(episodes) { episodes.distinctBy { it.id } }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().focusRestorer(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        itemsIndexed(
                            distinctEpisodes,
                            key = { _, ep -> "${ep.seasonNumber}_${ep.episodeNumber}" }) { _, ep ->
                            val progress = episodeProgressMap["s${ep.seasonNumber}_e${ep.episodeNumber}"] ?: 0f
                            val isWatched = progress >= (completionThreshold / 100f)
                            val isWatching = progress > 0.05f && !isWatched

                            EpisodeCardItem(
                                episode = ep,
                                isMobile = false,
                                screenPadding = 0.dp,
                                progress = progress,
                                isWatched = isWatched,
                                isWatching = isWatching,
                                onClick = { onPlayClick(ep.seasonNumber, ep.episodeNumber) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeCardItem(
    episode: EpisodeModel,
    isMobile: Boolean,
    screenPadding: androidx.compose.ui.unit.Dp,
    progress: Float = 0f,
    isWatched: Boolean = false,
    isWatching: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val thumbWidth = if (isMobile) 118.dp else 168.dp
    val titleSize = if (isMobile) 14.sp else 16.sp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = screenPadding, vertical = 3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (isFocused) NetflixWhite.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) NetflixWhite else Color.Transparent,
                shape = RoundedCornerShape(2.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The enclosing row owns focus, so the still is told about it rather
        // than competing for it; it only contributes the badge and progress.
        NetflixEpisodeThumbnailCard(
            stillUrl = episode.fullStillUrl,
            title = episode.title,
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber,
            progress = if (isWatched) 1f else progress,
            thumbnailWidth = thumbWidth,
            focused = isFocused,
            showFocusBorder = false
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            val fallbackOverview = stringResource(R.string.episode_simple, episode.episodeNumber)
            Text(
                text = "${episode.episodeNumber}. ${episode.title}",
                color = NetflixWhite,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = stringResource(R.string.duration_minutes_short_format, episode.durationMinutes),
                color = NetflixLightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = episode.overview.ifBlank { fallbackOverview },
                color = NetflixLightGray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun RatingsClusterRow(
    ratings: com.flick.app.data.model.RatingsCluster,
    selectedProviders: Set<String> = setOf("IMDb", "TMDB", "Rotten Tomatoes", "Metacritic", "Netflix"),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedProviders.contains("IMDb") && !ratings.imdbScore.isNullOrBlank()) {
            RatingChip(label = "IMDb", score = ratings.imdbScore)
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (selectedProviders.contains("TMDB") && !ratings.tmdbScore.isNullOrBlank()) {
            RatingChip(label = "TMDB", score = ratings.tmdbScore)
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (selectedProviders.contains("Rotten Tomatoes") && !ratings.rottenTomatoesScore.isNullOrBlank()) {
            RatingChip(label = "RT", score = ratings.rottenTomatoesScore)
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (selectedProviders.contains("Metacritic") && !ratings.metacriticScore.isNullOrBlank()) {
            RatingChip(label = "MC", score = ratings.metacriticScore)
        }
    }
}

@Composable
fun RatingChip(
    label: String,
    score: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(NetflixDarkSurface, RoundedCornerShape(4.dp))
            .border(1.dp, NetflixMutedGray.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label ",
            color = NetflixMutedGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = score,
            color = NetflixWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
