package com.stremflix.ui.detail

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.stremflix.R
import com.stremflix.data.model.EpisodeModel
import com.stremflix.data.model.MediaItem
import com.stremflix.data.model.SeasonModel
import com.stremflix.ui.home.BadgeTag
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixBorderHighlight
import com.stremflix.ui.theme.NetflixDarkSurface
import com.stremflix.ui.theme.NetflixDimens
import com.stremflix.ui.theme.NetflixGreenMatch
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixMutedGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite
import com.stremflix.ui.theme.isCompactScreen

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

                    Button(
                        onClick = {
                            if (mediaItem.mediaType == "tv") {
                                onPlayClick(smartNextSeason ?: selectedSeason?.seasonNumber ?: 1, smartNextEpisode ?: 1)
                            } else {
                                onPlayClick(null, null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetflixWhite,
                            contentColor = NetflixBlack
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(42.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.action_play),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = playText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            if (watchProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(3.5.dp)
                                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                                        .background(NetflixLightGray.copy(alpha = 0.35f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(watchProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(NetflixRed)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = onToggleWatchlist,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = NetflixDarkSurface.copy(alpha = 0.7f),
                            contentColor = NetflixWhite
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            imageVector = if (isWatchlisted) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = stringResource(R.string.nav_my_list),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_my_list),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
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
                            CircularProgressIndicator(
                                color = NetflixRed,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
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
                        CircularProgressIndicator(
                            color = NetflixRed,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
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

    BackHandler(enabled = showMoreEpisodesView) {
        showMoreEpisodesView = false
    }

    if (showMoreEpisodesView && mediaItem.mediaType == "tv") {
        // Dedicated More Episodes View matching Episodes.jpg reference
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
        ) {
            // High-res Background Artwork
            AsyncImage(
                model = mediaItem.fullBackdropUrl ?: mediaItem.fullPosterUrl,
                contentDescription = mediaItem.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // TV Ambient Dark Overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                NetflixBlack.copy(alpha = 0.96f),
                                NetflixBlack.copy(alpha = 0.88f),
                                NetflixBlack.copy(alpha = 0.45f)
                            ),
                            endX = 1400f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NetflixBlack.copy(alpha = 0.4f),
                                NetflixBlack.copy(alpha = 0.85f),
                                NetflixBlack
                            )
                        )
                    )
            )

            // TV Main 2-Pane Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 36.dp, top = 24.dp, end = 36.dp, bottom = 24.dp)
            ) {
                // Left Details Pane
                Column(
                    modifier = Modifier
                        .width(480.dp)
                        .fillMaxHeight()
                ) {
                    // Back button
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
                            .focusable()
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title (TMDB transparent logo when available)
                    DetailTitleLogo(
                        mediaItem = mediaItem,
                        height = 64.dp,
                        fallbackFontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // TV Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_match, mediaItem.matchPercentage),
                            color = NetflixGreenMatch,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = mediaItem.releaseYear, color = NetflixLightGray, fontSize = 13.sp)
                        if (mediaItem.contentRating.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            BadgeTag(text = mediaItem.contentRating)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        BadgeTag(text = stringResource(R.string.label_ultra_hd))
                        Spacer(modifier = Modifier.width(6.dp))
                        BadgeTag(text = stringResource(R.string.label_dolby))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ratings row
                    RatingsClusterRow(
                        ratings = mediaItem.ratingsCluster,
                        selectedProviders = selectedProviders
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Vertical Action Buttons Column (Matching TVDetails.jpg & Netflix TV layout)
                    val playText = if (watchProgress > 0f) {
                        stringResource(R.string.action_resume)
                    } else {
                        stringResource(R.string.action_play)
                    }

                    Column(
                        modifier = Modifier.width(320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Play / Resume Button
                        var playFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                if (mediaItem.mediaType == "tv") {
                                    onPlayClick(
                                        smartNextSeason ?: selectedSeason?.seasonNumber ?: 1,
                                        smartNextEpisode ?: 1
                                    )
                                } else {
                                    onPlayClick(null, null)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (playFocused) NetflixRed else NetflixWhite,
                                contentColor = if (playFocused) NetflixWhite else NetflixBlack
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .onFocusChanged { playFocused = it.isFocused }
                                .focusable()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.action_play),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = playText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        // 2. Episodes & Info Button (for TV Shows)
                        if (mediaItem.mediaType == "tv") {
                            var epFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { showMoreEpisodesView = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (epFocused) Color.White.copy(alpha = 0.3f) else NetflixDarkSurface,
                                    contentColor = NetflixWhite
                                ),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .border(if (epFocused) 2.dp else 0.dp, NetflixWhite, RoundedCornerShape(4.dp))
                                    .onFocusChanged { epFocused = it.isFocused }
                                    .focusable()
                            ) {
                                Text(
                                    text = stringResource(R.string.episodes_and_info),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // 3. My List Button
                        var listFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = onToggleWatchlist,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (listFocused) Color.White.copy(alpha = 0.3f) else NetflixDarkSurface,
                                contentColor = NetflixWhite
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(if (listFocused) 2.dp else 0.dp, NetflixWhite, RoundedCornerShape(4.dp))
                                .onFocusChanged { listFocused = it.isFocused }
                                .focusable()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isWatchlisted) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = stringResource(R.string.nav_my_list),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_my_list),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Synopsis
                    Text(
                        text = mediaItem.overview,
                        color = NetflixLightGray,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Right Showcase Spotlight Pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(NetflixDimens.BigCardRadius))
                    ) {
                        AsyncImage(
                            model = mediaItem.fullBackdropUrl ?: mediaItem.fullPosterUrl,
                            contentDescription = mediaItem.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, NetflixBlack.copy(alpha = 0.8f))
                                    )
                                )
                        ) {
                            if (mediaItem.mediaType == "tv" && seasons.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                        .background(NetflixBlack.copy(alpha = 0.85f), RoundedCornerShape(NetflixDimens.CardRadius))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (seasons.size == 1) {
                                            stringResource(R.string.season_format, 1)
                                        } else {
                                            stringResource(R.string.seasons_count_format, seasons.size)
                                        },
                                        color = NetflixWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        // High-res Background Artwork
        AsyncImage(
            model = mediaItem.fullBackdropUrl ?: mediaItem.fullPosterUrl,
            contentDescription = mediaItem.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        .focusable()
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
                    modifier = Modifier.weight(1f),
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
                                .focusable()
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
                        CircularProgressIndicator(color = NetflixRed)
                    }
                } else {
                    val distinctEpisodes = remember(episodes) { episodes.distinctBy { it.id } }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) NetflixWhite else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(thumbWidth)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(NetflixDimens.ThumbnailRadius))
                .background(NetflixDarkSurface)
        ) {
            AsyncImage(
                model = episode.fullStillUrl,
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Netflix only reveals the play affordance on the focused row.
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(NetflixBlack.copy(alpha = 0.62f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.action_play),
                        tint = NetflixWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isWatched || isWatching || progress > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(NetflixLightGray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isWatched) 1f else progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(NetflixRed)
                    )
                }
            }
        }

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
    ratings: com.stremflix.data.model.RatingsCluster,
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
