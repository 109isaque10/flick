package com.stremflix

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.stremflix.data.local.SettingsDataStore
import com.stremflix.data.local.StremFlixDatabase
import com.stremflix.data.model.ContentRow
import com.stremflix.data.model.EpisodeModel
import com.stremflix.data.model.MediaItem
import com.stremflix.data.model.SeasonModel
import com.stremflix.data.model.StremioStream
import com.stremflix.data.repository.MediaRepository
import com.stremflix.data.repository.StremioRepository
import com.stremflix.data.repository.TraktRepository
import com.stremflix.ui.categories.CategoriesScreen
import com.stremflix.ui.categories.GenreItem
import com.stremflix.ui.detail.DetailScreen
import com.stremflix.ui.detail.StreamSelectionDialog
import com.stremflix.ui.home.HomeScreen
import com.stremflix.ui.navigation.NavDestination
import com.stremflix.ui.navigation.StremFlixBottomNavBar
import com.stremflix.ui.navigation.StremFlixNavRail
import com.stremflix.ui.player.PlayerScreen
import com.stremflix.ui.player.TrailerPlayerScreen
import com.stremflix.ui.player.TrailerSource
import com.stremflix.ui.search.SearchScreen
import com.stremflix.ui.settings.SettingsScreen
import com.stremflix.ui.splash.StartupSplash
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixDimens
import com.stremflix.ui.theme.StremFlixTheme
import com.stremflix.ui.theme.isCompactScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Poster grid cap for TV search; the extra rows were never reachable by D-pad. */
private const val SEARCH_RESULT_LIMIT = 15

class MainActivity : ComponentActivity() {

    private lateinit var db: StremFlixDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var mediaRepository: MediaRepository
    private lateinit var stremioRepository: StremioRepository
    private lateinit var traktRepository: TraktRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent, dark system-bar styles so the OS never paints its light
        // protection scrim/gradient over the dark UI or the fullscreen player.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        db = StremFlixDatabase.getDatabase(this)
        settingsDataStore = SettingsDataStore(this)
        mediaRepository = MediaRepository(
            cacheDao = db.cacheDao(),
            watchHistoryDao = db.watchHistoryDao(),
            watchlistDao = db.watchlistDao(),
            settingsDataStore = settingsDataStore,
            mediaMetadataDao = db.mediaMetadataDao(),
            watchProgressDao = db.watchProgressDao()
        )
        stremioRepository = StremioRepository(mediaRepository, settingsDataStore)
        traktRepository = TraktRepository(
            settingsDataStore = settingsDataStore,
            watchlistDao = db.watchlistDao(),
            watchProgressDao = db.watchProgressDao()
        )

        setContent {
            StremFlixTheme {
                MainAppEntry(
                    mediaRepository = mediaRepository,
                    stremioRepository = stremioRepository,
                    traktRepository = traktRepository,
                    settingsDataStore = settingsDataStore
                )
            }
        }
    }
}

@Composable
fun MainAppEntry(
    mediaRepository: MediaRepository,
    stremioRepository: StremioRepository,
    traktRepository: TraktRepository,
    settingsDataStore: SettingsDataStore
) {
    var showSplash by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        MainAppContent(
            mediaRepository = mediaRepository,
            stremioRepository = stremioRepository,
            traktRepository = traktRepository,
            settingsDataStore = settingsDataStore
        )

        if (showSplash) {
            StartupSplash(onSplashFinished = { showSplash = false })
        }
    }
}

@Composable
fun MainAppContent(
    mediaRepository: MediaRepository,
    stremioRepository: StremioRepository,
    traktRepository: TraktRepository,
    settingsDataStore: SettingsDataStore
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Trakt watchlist/playback reconciliation now runs as a constrained
    // WorkManager job (StremFlixApp -> TraktSyncScheduler), so launch is no
    // longer held up by network calls while the first frame composes.

    val selectedProviders by settingsDataStore.selectedRatingProviders.collectAsStateWithLifecycle(initialValue = emptySet())

    var currentNavDestination by remember { mutableStateOf(NavDestination.HOME) }
    var selectedMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    var activeStreamUrl by remember { mutableStateOf<String?>(null) }
    var activeStreamTitle by remember { mutableStateOf("") }
    var activeStreamSubtitle by remember { mutableStateOf<String?>(null) }

    // Trailers are short clips, so they keep their own minimal player state
    // instead of going through the episode/stream pipeline.
    var activeTrailerUrl by remember { mutableStateOf<String?>(null) }
    var activeTrailerTitle by remember { mutableStateOf("") }

    // Stream Selection Dialog State
    var streamSelectionItem by remember { mutableStateOf<MediaItem?>(null) }
    var streamSelectionSeason by remember { mutableStateOf<Int?>(null) }
    var streamSelectionEpisode by remember { mutableStateOf<Int?>(null) }
    var streamSelectionList by remember { mutableStateOf<List<StremioStream>?>(null) }
    var isFetchingStreams by remember { mutableStateOf(false) }

    // Home & New/Popular Data
    var contentRows by remember { mutableStateOf<List<ContentRow>>(emptyList()) }
    var newAndPopularRows by remember { mutableStateOf<List<ContentRow>>(emptyList()) }
    var heroItem by remember { mutableStateOf<MediaItem?>(null) }

    // Active Playing Tracked State
    var activePlayingItem by remember { mutableStateOf<MediaItem?>(null) }
    var activePlayingSeason by remember { mutableStateOf<Int?>(null) }
    var activePlayingEpisode by remember { mutableStateOf<Int?>(null) }
    var activePlayingInitialPositionMs by remember { mutableStateOf(0L) }
    // Set by the detail page's "Play from beginning" action so the saved
    // resume position is ignored for that one playback.
    var forcePlayFromBeginning by remember { mutableStateOf(false) }

    // Detail Data
    var tvSeasons by remember { mutableStateOf<List<SeasonModel>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<SeasonModel?>(null) }
    var seasonEpisodes by remember { mutableStateOf<List<EpisodeModel>>(emptyList()) }
    var isMediaWatchlisted by remember { mutableStateOf(false) }

    // Player Pre-fetch & Up Next Data
    var availableStreams by remember { mutableStateOf<List<StremioStream>>(emptyList()) }
    var nextEpisodeToPrefetch by remember { mutableStateOf<EpisodeModel?>(null) }

    // Search Data
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var lastSearchQuery by remember { mutableStateOf<String?>(null) }

    // Landing target for D-pad focus after a sidebar selection, so the rail
    // collapses instead of staying open over the screen.
    val contentFocusRequester = remember { FocusRequester() }
    // Explicit target for LEFT out of the content pane, so entering the rail
    // skips Compose's 2D spatial search over every card currently on screen.
    val railFocusRequester = remember { FocusRequester() }
    val contentInteractionSource = remember { MutableInteractionSource() }

    // Categories Data
    val sampleGenres = remember {
        listOf(
            GenreItem(28, "Action"),
            GenreItem(12, "Adventure"),
            GenreItem(16, "Animation"),
            GenreItem(35, "Comedy"),
            GenreItem(80, "Crime"),
            GenreItem(99, "Documentary"),
            GenreItem(18, "Drama"),
            GenreItem(10751, "Family"),
            GenreItem(14, "Fantasy"),
            GenreItem(27, "Horror"),
            GenreItem(9648, "Mystery"),
            GenreItem(10749, "Romance"),
            GenreItem(878, "Sci-Fi"),
            GenreItem(53, "Thriller")
        )
    }
    var selectedGenre by remember { mutableStateOf<GenreItem?>(sampleGenres.first()) }
    var categoryItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isCategoryLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedGenre, currentNavDestination) {
        // Categories costs two discover requests; it must not run while the user
        // is still on Home, and its results are dropped when the tab is left so
        // the TV does not keep a second full catalogue in memory.
        if (currentNavDestination == NavDestination.CATEGORIES) {
            selectedGenre?.let { genre ->
                isCategoryLoading = true
                categoryItems = mediaRepository.getMediaByGenre(genre.id)
                isCategoryLoading = false
            }
        } else {
            categoryItems = emptyList()
        }
    }

    // Settings Data
    val tmdbApiKey by settingsDataStore.tmdbApiKey.collectAsState(initial = "")
    val tmdbLanguage by settingsDataStore.tmdbLanguage.collectAsState(initial = "en-US")
    val tmdbRegion by settingsDataStore.tmdbRegion.collectAsState(initial = "US")
    val stremioAddonUrl by settingsDataStore.stremioAddonUrl.collectAsState(initial = "https://torrentio.strem.fun/")
    val omdbApiKey by settingsDataStore.omdbApiKey.collectAsState(initial = "")
    val youtubeBackendUrl by settingsDataStore.youtubeBackendUrl.collectAsState(initial = "")
    val selectedRatingProviders by settingsDataStore.selectedRatingProviders.collectAsState(
        initial = setOf(
            "IMDb",
            "TMDB",
            "Rotten Tomatoes",
            "Metacritic",
            "Netflix"
        )
    )
    val stremioIdStrategy by settingsDataStore.stremioIdStrategy.collectAsState(initial = "IMDB")
    val completionThreshold by settingsDataStore.completionThreshold.collectAsState(initial = 90)
    val upNextThreshold by settingsDataStore.popupThreshold.collectAsState(initial = 90)
    val traktApiKey by settingsDataStore.traktApiKey.collectAsState(initial = "")
    val traktClientSecret by settingsDataStore.traktClientSecret.collectAsState(initial = "")
    val traktUsername by traktRepository.traktUsername.collectAsState(initial = null)
    val savedWatchlist by mediaRepository.getWatchlistItems().collectAsState(initial = emptyList())

    var traktDeviceCode by remember { mutableStateOf<String?>(null) }
    var traktVerificationUrl by remember { mutableStateOf<String?>(null) }

    var smartNextSeason by remember { mutableStateOf<Int?>(null) }
    var smartNextEpisode by remember { mutableStateOf<Int?>(null) }
    var episodeProgressMap by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var currentItemWatchProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(selectedMediaItem, completionThreshold) {
        selectedMediaItem?.let { item ->
            if (item.mediaType == "tv") {
                val (s, e) = mediaRepository.getSmartNextEpisode(item.id, completionThreshold)
                smartNextSeason = s
                smartNextEpisode = e
            } else {
                smartNextSeason = null
                smartNextEpisode = null
            }
        }
    }

    // Collect Watch Progress for Selected Media Item (for episodes & show)
    LaunchedEffect(selectedMediaItem?.id, selectedMediaItem?.mediaType, activePlayingItem?.id) {
        val targetItem = selectedMediaItem ?: activePlayingItem
        if (targetItem != null) {
            if (targetItem.mediaType == "tv") {
                mediaRepository.getWatchProgressForShow(targetItem.id).collect { progressList ->
                    episodeProgressMap = progressList.associate {
                        "s${it.seasonNumber ?: 1}_e${it.episodeNumber ?: 1}" to it.progressPercentage
                    }
                    val latest = progressList.maxByOrNull { it.lastWatchedAt }
                    currentItemWatchProgress = latest?.progressPercentage ?: 0f
                }
            } else {
                val latest = mediaRepository.getLatestWatchProgress(targetItem.id, targetItem.mediaType)
                currentItemWatchProgress = latest?.progressPercentage ?: 0f
                episodeProgressMap = emptyMap()
            }
        }
    }

    // Load Initial Home Content when key or language/region changes.
    // Rows stream in progressively, so the first row paints after one round trip
    // instead of waiting for every request to finish.
    LaunchedEffect(tmdbApiKey, tmdbLanguage, tmdbRegion, traktApiKey) {
        newAndPopularRows = emptyList() // refetched lazily on next visit
        var heroResolved = false
        mediaRepository.getHomeContentRowsFlow().collect { rows ->
            contentRows = rows
            if (!heroResolved) {
                val rawHero = rows.firstOrNull { it.items.isNotEmpty() }?.items?.firstOrNull()
                if (rawHero != null) {
                    heroResolved = true
                    launch {
                        heroItem =
                            mediaRepository.getMediaDetailsWithRatings(rawHero.id, rawHero.mediaType, rawHero)
                    }
                }
            }
        }
    }

    // New & Popular duplicates several home requests, so it is only loaded
    // when its tab is actually opened.
    LaunchedEffect(currentNavDestination, tmdbApiKey, tmdbLanguage, tmdbRegion, traktApiKey) {
        if (currentNavDestination == NavDestination.NEW_POPULAR) {
            if (newAndPopularRows.isEmpty()) {
                newAndPopularRows = mediaRepository.getNewAndPopularContentRows()
            }
        } else {
            // These rows duplicate most of Home; keeping them alive after the tab
            // is left wastes TV memory for no benefit.
            newAndPopularRows = emptyList()
        }
    }

    // Load Search Results (debounced so typing does not fire a request per keystroke)
    LaunchedEffect(searchQuery) {
        val query = searchQuery
        // One character matches nearly everything and costs the same request as
        // a real query on a TV stick, so wait for a minimally useful prefix.
        if (query.trim().length >= 2) {
            // The effect restarts on every keystroke, so this is a true
            // "stopped typing" debounce. 2s of quiet avoids firing a request
            // almost every second while the viewer is still entering text.
            delay(2000)
            // The TV keyboard fires one change per key, so identical consecutive
            // queries (e.g. spaces trimmed by the field) reuse the last payload.
            if (query != lastSearchQuery) {
                lastSearchQuery = query
                searchResults = mediaRepository.searchMedia(query).take(SEARCH_RESULT_LIMIT)
            }
        } else {
            lastSearchQuery = null
            searchResults = emptyList()
        }
    }

    // Load Detail Data & TV Seasons when selectedMediaItem changes
    LaunchedEffect(selectedMediaItem?.id, selectedMediaItem?.mediaType) {
        selectedMediaItem?.let { item ->
            if (item.mediaType == "tv") {
                // Seasons and the enriched detail are independent calls; running
                // them in parallel halves the wait before the page is usable.
                coroutineScope {
                    val seasonsDeferred = async { mediaRepository.getTvSeasons(item.id) }
                    val updatedDeferred = async {
                        mediaRepository.getMediaDetailsWithRatings(item.id, item.mediaType, item)
                    }
                    val seasons = seasonsDeferred.await()
                    tvSeasons = seasons
                    val firstSeason = seasons.firstOrNull { it.seasonNumber > 0 } ?: seasons.firstOrNull()
                    selectedSeason = firstSeason
                    if (firstSeason != null) {
                        seasonEpisodes = mediaRepository.getSeasonEpisodes(item.id, firstSeason.seasonNumber)
                    }
                    selectedMediaItem = updatedDeferred.await()
                }
            } else {
                tvSeasons = emptyList()
                seasonEpisodes = emptyList()
                selectedMediaItem = mediaRepository.getMediaDetailsWithRatings(item.id, item.mediaType, item)
            }
        }
    }

    // Collect Watchlist Status independently
    LaunchedEffect(selectedMediaItem?.id, selectedMediaItem?.mediaType) {
        selectedMediaItem?.let { item ->
            mediaRepository.isWatchlisted("${item.mediaType}_${item.id}").collect {
                isMediaWatchlisted = it
            }
        }
    }

    // Load Season Episodes when selectedSeason changes
    LaunchedEffect(selectedSeason) {
        selectedMediaItem?.let { item ->
            selectedSeason?.let { season ->
                seasonEpisodes = mediaRepository.getSeasonEpisodes(item.id, season.seasonNumber)
            }
        }
    }

    // Calculate next episode to prefetch whenever active playing or stream selection parameters change
    LaunchedEffect(
        activePlayingItem?.id,
        activePlayingSeason,
        activePlayingEpisode,
        streamSelectionItem?.id,
        streamSelectionSeason,
        streamSelectionEpisode
    ) {
        val item = activePlayingItem ?: streamSelectionItem
        val season = activePlayingSeason ?: streamSelectionSeason
        val episode = activePlayingEpisode ?: streamSelectionEpisode
        if (item != null && item.mediaType == "tv" && season != null && episode != null) {
            val currentSeasonEpisodes = mediaRepository.getSeasonEpisodes(item.id, season)
            val nextInSeason = currentSeasonEpisodes.find { it.episodeNumber == episode + 1 }
            if (nextInSeason != null) {
                nextEpisodeToPrefetch = nextInSeason
            } else {
                val nextSeasonEpisodes = mediaRepository.getSeasonEpisodes(item.id, season + 1)
                nextEpisodeToPrefetch = nextSeasonEpisodes.find { it.episodeNumber == 1 }
            }
        } else {
            nextEpisodeToPrefetch = null
        }
    }

    // Play Stream Helper Function - Triggers Stream Selection Dialog or auto-plays next episode
    fun playMediaStream(
        item: MediaItem,
        season: Int? = null,
        episode: Int? = null,
        autoPlayDirectly: Boolean = false,
        fromBeginning: Boolean = false
    ) {
        coroutineScope.launch {
            forcePlayFromBeginning = fromBeginning
            val actualSeason: Int?
            val actualEpisode: Int?
            if (item.mediaType == "tv") {
                if (season != null && episode != null) {
                    actualSeason = season
                    actualEpisode = episode
                } else {
                    val (s, e) = mediaRepository.getSmartNextEpisode(item.id, completionThreshold)
                    actualSeason = s
                    actualEpisode = e
                }
            } else {
                actualSeason = null
                actualEpisode = null
            }

            if (autoPlayDirectly) {
                val streams = stremioRepository.resolveStreams(
                    tmdbId = item.id,
                    mediaType = item.mediaType,
                    seasonNumber = actualSeason,
                    episodeNumber = actualEpisode
                )
                if (streams.isNotEmpty()) {
                    val topStream = streams.first()
                    val savedPosition = mediaRepository.getSavedPlaybackPosition(
                        tmdbId = item.id,
                        mediaType = item.mediaType,
                        seasonNumber = actualSeason,
                        episodeNumber = actualEpisode,
                        completionThreshold = completionThreshold
                    )
                    activePlayingInitialPositionMs = if (forcePlayFromBeginning) 0L else savedPosition
                    activeStreamUrl = topStream.url
                    activeStreamTitle = item.title
                    val epSubtitle =
                        if (item.mediaType == "tv" && actualSeason != null && actualEpisode != null) "S${actualSeason}:E${actualEpisode}" else null
                    activeStreamSubtitle = epSubtitle
                    activePlayingItem = item
                    activePlayingSeason = actualSeason
                    activePlayingEpisode = actualEpisode
                    streamSelectionItem = null
                    return@launch
                }
            }

            streamSelectionItem = item
            streamSelectionSeason = actualSeason
            streamSelectionEpisode = actualEpisode
            isFetchingStreams = true
            streamSelectionList = null

            val streams = stremioRepository.resolveStreams(
                tmdbId = item.id,
                mediaType = item.mediaType,
                seasonNumber = actualSeason,
                episodeNumber = actualEpisode
            )
            availableStreams = streams
            streamSelectionList = streams
            isFetchingStreams = false
        }
    }

    // Single source of truth for the Home chips, which used to duplicate this map.
    val handleFilterSelection: (String) -> Unit = { filter ->
        val target = when (filter) {
            "TV Shows" -> NavDestination.TV_SHOWS
            "Movies" -> NavDestination.MOVIES
            "Categories" -> NavDestination.CATEGORIES
            else -> null
        }
        if (target != null) currentNavDestination = target
    }

    // Without this, back from any tab exits the app instead of returning Home.
    BackHandler(
        enabled = activeStreamUrl == null &&
                selectedMediaItem == null &&
                currentNavDestination != NavDestination.HOME
    ) {
        currentNavDestination = NavDestination.HOME
    }

    // App Layout with Left NavRail or Fullscreen ExoPlayer
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
    ) {
        if (activeTrailerUrl != null) {
            TrailerPlayerScreen(
                streamUrl = activeTrailerUrl!!,
                title = activeTrailerTitle,
                onBack = { activeTrailerUrl = null }
            )
        } else if (activeStreamUrl != null) {
            // Fullscreen Player
            PlayerScreen(
                streamUrl = activeStreamUrl!!,
                title = activeStreamTitle,
                subtitle = activeStreamSubtitle,
                nextEpisode = nextEpisodeToPrefetch,
                nextEpisodeStillUrl = nextEpisodeToPrefetch?.fullStillUrl,
                initialPositionMs = activePlayingInitialPositionMs,
                onBack = {
                    activeStreamUrl = null
                    activePlayingItem = null
                    activePlayingSeason = null
                    activePlayingEpisode = null
                    activePlayingInitialPositionMs = 0L
                    nextEpisodeToPrefetch = null
                },
                upNextThreshold = upNextThreshold,
                episodes = seasonEpisodes,
                seasons = tvSeasons,
                currentSeasonNumber = activePlayingSeason,
                currentEpisodeNumber = activePlayingEpisode,
                episodeProgressMap = episodeProgressMap,
                completionThreshold = completionThreshold,
                onSelectEpisode = { s, e ->
                    (activePlayingItem ?: selectedMediaItem)?.let { item ->
                        playMediaStream(item, s, e)
                    }
                },
                onProgressUpdate = { posMs, durMs ->
                    (activePlayingItem ?: selectedMediaItem)?.let { item ->
                        coroutineScope.launch {
                            val seasonNum = activePlayingSeason ?: streamSelectionSeason
                            val episodeNum = activePlayingEpisode ?: streamSelectionEpisode
                            // Movies have no episode, so nothing is invented for them.
                            val episodeTitle = if (item.mediaType == "tv") {
                                seasonEpisodes
                                    .firstOrNull {
                                        it.seasonNumber == seasonNum && it.episodeNumber == episodeNum
                                    }?.title
                            } else {
                                null
                            }

                            mediaRepository.saveWatchProgress(
                                tmdbId = item.id,
                                mediaType = item.mediaType,
                                title = item.title,
                                posterPath = item.posterPath,
                                backdropPath = item.backdropPath,
                                positionMs = posMs,
                                durationMs = durMs,
                                seasonNumber = seasonNum,
                                episodeNumber = episodeNum,
                                episodeTitle = episodeTitle
                            )
                            if (durMs > 0) {
                                val progressPct = (posMs.toFloat() / durMs.toFloat()) * 100f
                                traktRepository.scrobbleProgressToTrakt(
                                    tmdbId = item.id,
                                    mediaType = item.mediaType,
                                    progressPct = progressPct,
                                    season = seasonNum,
                                    episode = episodeNum
                                )
                            }
                        }
                    }
                },
                onPrefetchTriggered = {
                    nextEpisodeToPrefetch?.let { ep ->
                        (activePlayingItem ?: selectedMediaItem)?.let { item ->
                            coroutineScope.launch {
                                stremioRepository.resolveStreams(
                                    tmdbId = item.id,
                                    mediaType = item.mediaType,
                                    seasonNumber = ep.seasonNumber,
                                    episodeNumber = ep.episodeNumber
                                )
                            }
                        }
                    }
                },
                onPlayNextEpisode = { ep ->
                    (activePlayingItem ?: selectedMediaItem)?.let { item ->
                        playMediaStream(item, ep.seasonNumber, ep.episodeNumber, autoPlayDirectly = true)
                    }
                }
            )
        } else if (selectedMediaItem != null) {
            // Media Detail Screen
            DetailScreen(
                mediaItem = selectedMediaItem!!,
                isWatchlisted = isMediaWatchlisted,
                selectedProviders = selectedProviders,
                watchProgress = currentItemWatchProgress,
                seasons = tvSeasons,
                episodes = seasonEpisodes,
                selectedSeason = selectedSeason,
                onSeasonSelected = { season -> selectedSeason = season },
                smartNextSeason = smartNextSeason,
                smartNextEpisode = smartNextEpisode,
                episodeProgressMap = episodeProgressMap,
                completionThreshold = completionThreshold,
                onPlayClick = { season, episode ->
                    selectedMediaItem?.let { item ->
                        val targetSeason = season ?: smartNextSeason ?: 1
                        val targetEpisode = episode ?: smartNextEpisode ?: 1
                        playMediaStream(item, targetSeason, targetEpisode)
                    }
                },
                onPlayFromBeginning = {
                    selectedMediaItem?.let { item ->
                        if (item.mediaType == "tv") {
                            playMediaStream(item, smartNextSeason ?: 1, smartNextEpisode ?: 1, fromBeginning = true)
                        } else {
                            playMediaStream(item, fromBeginning = true)
                        }
                    }
                },
                onPlayTrailer = selectedMediaItem?.trailerKey?.let { trailerKey ->
                    {
                        val target = selectedMediaItem
                        val resolvedUrl = TrailerSource.manifestUrl(youtubeBackendUrl, trailerKey)
                        if (resolvedUrl != null) {
                            activeTrailerTitle = target?.trailerName
                                ?.takeIf { it.isNotBlank() }
                                ?: target?.title.orEmpty()
                            activeTrailerUrl = resolvedUrl
                        } else {
                            // No inline resolver configured: hand the key to the
                            // YouTube app rather than faking playback.
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(TrailerSource.youtubeWatchUrl(trailerKey))
                            )
                            runCatching { context.startActivity(intent) }
                        }
                    }
                },
                onToggleWatchlist = {
                    selectedMediaItem?.let { item ->
                        coroutineScope.launch { mediaRepository.toggleWatchlist(item, traktRepository) }
                    }
                },
                onBack = { selectedMediaItem = null }
            )
        } else {
            // Main Dashboard with Adaptive Navigation (Bottom Bar for Mobile, Rail for TV/Landscape)
            val isMobileLayout = isCompactScreen()

            val screenContent: @Composable () -> Unit = {
                // A plain `when` (rather than Crossfade) keeps exactly one screen
                // composed: on TV the outgoing screen's posters and rows are
                // released immediately instead of lingering through a fade.
                val dest = currentNavDestination
                when (dest) {
                    NavDestination.HOME -> {
                        HomeScreen(
                            contentRows = contentRows,
                            heroItem = heroItem,
                            onMediaSelected = { selectedMediaItem = it },
                            onPlayClicked = { item -> playMediaStream(item) },
                            isTmdbKeyConfigured = tmdbApiKey.isNotBlank(),
                            onNavigateToSettings = { currentNavDestination = NavDestination.SETTINGS },
                            onSelectFilter = handleFilterSelection,
                            activeFilter = "All",
                            onSearchClicked = { currentNavDestination = NavDestination.SEARCH },
                            showFilterChips = true
                        )
                    }

                    NavDestination.NEW_POPULAR -> {
                        HomeScreen(
                            contentRows = if (newAndPopularRows.isNotEmpty()) newAndPopularRows else contentRows,
                            heroItem = newAndPopularRows.firstOrNull()?.items?.firstOrNull() ?: heroItem,
                            onMediaSelected = { selectedMediaItem = it },
                            onPlayClicked = { item -> playMediaStream(item) },
                            isTmdbKeyConfigured = tmdbApiKey.isNotBlank(),
                            onNavigateToSettings = { currentNavDestination = NavDestination.SETTINGS },
                            onSelectFilter = handleFilterSelection,
                            activeFilter = "NewPopular",
                            onSearchClicked = { currentNavDestination = NavDestination.SEARCH },
                            showFilterChips = true
                        )
                    }

                    NavDestination.MOVIES -> {
                        // Derived once per `contentRows` change. Building the list
                        // inline made a new List instance on every recomposition, so
                        // HomeScreen could never skip and fully re-laid every row.
                        val movieRows = remember(contentRows) {
                            contentRows.filter { it.id == "popular_movies" || it.id == "action_adventure" }
                        }
                        HomeScreen(
                            contentRows = if (movieRows.isNotEmpty()) movieRows else contentRows,
                            heroItem = contentRows.firstOrNull { it.id == "popular_movies" }?.items?.firstOrNull()
                                ?: heroItem,
                            onMediaSelected = { selectedMediaItem = it },
                            onPlayClicked = { item -> playMediaStream(item) },
                            isTmdbKeyConfigured = tmdbApiKey.isNotBlank(),
                            onNavigateToSettings = { currentNavDestination = NavDestination.SETTINGS },
                            onSelectFilter = handleFilterSelection,
                            activeFilter = "Movies",
                            onSearchClicked = { currentNavDestination = NavDestination.SEARCH },
                            showFilterChips = true
                        )
                    }

                    NavDestination.TV_SHOWS -> {
                        val tvRows = remember(contentRows) {
                            contentRows.filter { it.id == "bingeworthy_tv" }
                        }
                        HomeScreen(
                            contentRows = if (tvRows.isNotEmpty()) tvRows else contentRows,
                            heroItem = contentRows.firstOrNull { it.id == "bingeworthy_tv" }?.items?.firstOrNull()
                                ?: heroItem,
                            onMediaSelected = { selectedMediaItem = it },
                            onPlayClicked = { item -> playMediaStream(item) },
                            isTmdbKeyConfigured = tmdbApiKey.isNotBlank(),
                            onNavigateToSettings = { currentNavDestination = NavDestination.SETTINGS },
                            onSelectFilter = handleFilterSelection,
                            activeFilter = "TV Shows",
                            onSearchClicked = { currentNavDestination = NavDestination.SEARCH },
                            showFilterChips = true
                        )
                    }

                    NavDestination.MY_LIST -> {
                        val myListRows = remember(savedWatchlist) {
                            listOf(
                                ContentRow(
                                    id = "my_watchlist",
                                    title = "",
                                    titleRes = R.string.row_my_list,
                                    items = savedWatchlist
                                )
                            )
                        }
                        HomeScreen(
                            contentRows = myListRows,
                            heroItem = savedWatchlist.firstOrNull(),
                            onMediaSelected = { selectedMediaItem = it },
                            onPlayClicked = { item -> playMediaStream(item) },
                            isTmdbKeyConfigured = tmdbApiKey.isNotBlank(),
                            onNavigateToSettings = { currentNavDestination = NavDestination.SETTINGS },
                            onSearchClicked = { currentNavDestination = NavDestination.SEARCH }
                        )
                    }

                    NavDestination.SEARCH -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            searchResults = searchResults,
                            onMediaSelected = { selectedMediaItem = it }
                        )
                    }

                    NavDestination.CATEGORIES -> {
                        CategoriesScreen(
                            genres = sampleGenres,
                            selectedGenre = selectedGenre,
                            onGenreSelected = { selectedGenre = it },
                            categoryItems = categoryItems,
                            isLoading = isCategoryLoading,
                            onMediaSelected = { selectedMediaItem = it }
                        )
                    }

                    NavDestination.SETTINGS -> {
                        SettingsScreen(
                            tmdbApiKey = tmdbApiKey,
                            onSaveTmdbApiKey = { key ->
                                coroutineScope.launch { settingsDataStore.saveTmdbApiKey(key) }
                            },
                            tmdbLanguage = tmdbLanguage,
                            onSaveTmdbLanguage = { lang ->
                                coroutineScope.launch { settingsDataStore.saveTmdbLanguage(lang) }
                            },
                            tmdbRegion = tmdbRegion,
                            onSaveTmdbRegion = { reg ->
                                coroutineScope.launch { settingsDataStore.saveTmdbRegion(reg) }
                            },
                            stremioAddonUrl = stremioAddonUrl,
                            onSaveStremioAddonUrl = { url ->
                                coroutineScope.launch { settingsDataStore.saveStremioAddonUrl(url) }
                            },
                            omdbApiKey = omdbApiKey,
                            onSaveOmdbApiKey = { key ->
                                coroutineScope.launch { settingsDataStore.saveOmdbApiKey(key) }
                            },
                            youtubeBackendUrl = youtubeBackendUrl,
                            onSaveYoutubeBackendUrl = { url ->
                                coroutineScope.launch { settingsDataStore.saveYoutubeBackendUrl(url) }
                            },
                            selectedProviders = selectedRatingProviders,
                            onToggleProvider = { provider ->
                                val updated = if (selectedRatingProviders.contains(provider)) {
                                    selectedRatingProviders - provider
                                } else {
                                    selectedRatingProviders + provider
                                }
                                coroutineScope.launch { settingsDataStore.setRatingProviders(updated) }
                            },
                            stremioIdStrategy = stremioIdStrategy,
                            onSelectStremioStrategy = { strat ->
                                coroutineScope.launch { settingsDataStore.setStremioIdStrategy(strat) }
                            },
                            completionThreshold = completionThreshold,
                            onSaveCompletionThreshold = { pct ->
                                coroutineScope.launch { settingsDataStore.saveCompletionThreshold(pct) }
                            },
                            upNextThreshold = upNextThreshold,
                            onSaveUpNextThreshold = { pct ->
                                coroutineScope.launch { settingsDataStore.saveUpNextThreshold(pct) }
                            },
                            traktApiKey = traktApiKey,
                            onSaveTraktApiKey = { key ->
                                coroutineScope.launch { settingsDataStore.saveTraktApiKey(key) }
                            },
                            traktClientSecret = traktClientSecret,
                            onSaveTraktClientSecret = { secret ->
                                coroutineScope.launch { settingsDataStore.saveTraktClientSecret(secret) }
                            },
                            traktUsername = traktUsername,
                            traktDeviceCode = traktDeviceCode,
                            traktVerificationUrl = traktVerificationUrl,
                            onRequestTraktPairing = {
                                coroutineScope.launch {
                                    val res = traktRepository.requestDeviceCode()
                                    traktDeviceCode = res?.userCode
                                    traktVerificationUrl = res?.verificationUrl
                                    if (res != null) {
                                        // Poll until the user approves (or the code expires);
                                        // success lands in the settings flows.
                                        traktRepository.startDeviceAuthPolling(
                                            deviceCode = res.deviceCode,
                                            intervalSeconds = res.interval
                                        )
                                    }
                                }
                            },
                            onDisconnectTrakt = {
                                coroutineScope.launch { traktRepository.disconnectTrakt() }
                            }
                        )
                    }
                }
            }

            if (isMobileLayout) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        screenContent()
                    }
                    StremFlixBottomNavBar(
                        currentDestination = currentNavDestination,
                        onDestinationSelected = { dest -> currentNavDestination = dest }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = NetflixDimens.RailWidthCollapsed)
                            .focusRequester(contentFocusRequester)
                            .focusProperties { left = railFocusRequester }
                            .clickable(
                                interactionSource = contentInteractionSource,
                                indication = null
                            ) { }
                    ) {
                        screenContent()
                    }
                    StremFlixNavRail(
                        currentDestination = currentNavDestination,
                        entryFocusRequester = railFocusRequester,
                        onDestinationSelected = { dest ->
                            currentNavDestination = dest
                            // Pull focus out of the rail so the sidebar collapses
                            // and the selected screen becomes the D-pad surface.
                            runCatching { contentFocusRequester.requestFocus() }
                        }
                    )
                }
            }
        }

        // Stream Selection Dialog Overlay
        streamSelectionItem?.let { item ->
            val isTvShow = item.mediaType == "tv"
            val epSubtitle =
                if (isTvShow && streamSelectionSeason != null && streamSelectionEpisode != null) "S${streamSelectionSeason}:E${streamSelectionEpisode}" else null
            StreamSelectionDialog(
                mediaTitle = item.title,
                subtitle = epSubtitle,
                streams = streamSelectionList,
                isLoading = isFetchingStreams,
                onStreamSelected = { selectedStream ->
                    coroutineScope.launch {
                        val savedPos = mediaRepository.getSavedPlaybackPosition(
                            tmdbId = item.id,
                            mediaType = item.mediaType,
                            seasonNumber = streamSelectionSeason,
                            episodeNumber = streamSelectionEpisode,
                            completionThreshold = completionThreshold
                        )
                        activePlayingInitialPositionMs = if (forcePlayFromBeginning) 0L else savedPos
                        activePlayingItem = item
                        activePlayingSeason = streamSelectionSeason
                        activePlayingEpisode = streamSelectionEpisode
                        streamSelectionItem = null
                        activeStreamUrl = selectedStream.url
                        activeStreamTitle = item.title
                        activeStreamSubtitle = epSubtitle
                    }
                },
                onDismiss = { streamSelectionItem = null },
                onNavigateToSettings = {
                    streamSelectionItem = null
                    currentNavDestination = NavDestination.SETTINGS
                }
            )
        }
    }
}
