@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.tv.material3.ExperimentalTvMaterial3Api::class
)

package com.flick.app.ui.player

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.KeyEvent as AndroidKeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.flick.app.R
import com.flick.app.data.model.EpisodeModel
import com.flick.app.data.model.SeasonModel
import com.flick.app.ui.theme.NetflixBlack
import com.flick.app.ui.theme.NetflixDarkSurface
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixMutedGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.components.TvOptionMenuDialog
import com.flick.app.ui.components.TvSeekBar
import com.flick.app.ui.components.TvSpinner
import com.flick.app.ui.theme.NetflixWhite
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    subtitle: String?,
    nextEpisode: EpisodeModel?,
    nextEpisodeStillUrl: String?,
    initialPositionMs: Long = 0L,
    onBack: () -> Unit,
    onProgressUpdate: (Long, Long) -> Unit,
    onPrefetchTriggered: () -> Unit,
    onPlayNextEpisode: (EpisodeModel) -> Unit,
    upNextThreshold: Int = 90,
    episodes: List<EpisodeModel> = emptyList(),
    seasons: List<SeasonModel> = emptyList(),
    currentSeasonNumber: Int? = null,
    currentEpisodeNumber: Int? = null,
    onSelectEpisode: ((Int, Int) -> Unit)? = null,
    episodeProgressMap: Map<String, Float> = emptyMap(),
    completionThreshold: Int = 90,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val activity = (context as? Activity)
    val lifecycleOwner = LocalLifecycleOwner.current

    val configuration = LocalConfiguration.current
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    val isTvDevice = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val isMobile = !isTvDevice && (configuration.screenHeightDp < 550 || configuration.screenWidthDp < 700)

    // Hide status bar & navigation bar for full screen immersive playback, lock landscape orientation
    val view = LocalView.current
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    var savedPlaybackPosition by rememberSaveable { mutableLongStateOf(0L) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }

    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAudioSubtitlesDialog by remember { mutableStateOf(false) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    // Season currently listed by the in-player drawer. Set from the playing
    // episode every time the drawer opens so it never starts on season 1.
    var drawerSeason by remember { mutableStateOf<Int?>(null) }

    var selectedAudioTrack by remember { mutableStateOf("") }
    var selectedSubtitleTrack by remember { mutableStateOf("Off") }

    // Populated from the real track list; empty until the stream exposes its tracks.
    var availableAudioTracks by remember { mutableStateOf(emptyList<String>()) }
    var availableSubtitleTracks by remember { mutableStateOf(listOf("Off")) }

    var prefetchFired by remember { mutableStateOf(false) }
    var showUpNextPopup by remember { mutableStateOf(false) }
    var userDismissedUpNext by remember { mutableStateOf(false) }
    var upNextCountdown by remember { mutableStateOf(20) }

    // Single shot guards so up-next, end-of-media and the countdown cannot all
    // start the next episode.
    var reachedEnd by remember { mutableStateOf(false) }
    var autoAdvanceHandled by remember { mutableStateOf(false) }

    var lastProgressSave by remember { mutableLongStateOf(0L) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    // Reset playback and popup state when streamUrl or episode changes
    LaunchedEffect(streamUrl, currentEpisodeNumber, currentSeasonNumber) {
        prefetchFired = false
        showUpNextPopup = false
        userDismissedUpNext = false
        upNextCountdown = 20
        currentPosition = 0L
        duration = 1L
        savedPlaybackPosition = 0L
        reachedEnd = false
        autoAdvanceHandled = false
        lastProgressSave = 0L
        playerError = null
    }

    // Initialize ExoPlayer with HTTP Data Source & cross protocol redirects
    DisposableEffect(streamUrl, reloadKey) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(buildPlayerLoadControl(context))
            .build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
                if (initialPositionMs > 0L) {
                    seekTo(initialPositionMs)
                } else if (savedPlaybackPosition > 0L) {
                    seekTo(savedPlaybackPosition)
                }
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_ENDED) {
                            // Resolved outside the listener: the episode lists may
                            // still be loading and would be stale here.
                            reachedEnd = true
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        isBuffering = false
                        playerError = error.localizedMessage
                            ?: context.getString(R.string.player_stream_error)
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        val aSet = mutableSetOf<String>()
                        val sSet = mutableSetOf("Off")

                        for (group in tracks.groups) {
                            if (group.type == C.TRACK_TYPE_AUDIO) {
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    val lang = format.language?.let { Locale(it).displayLanguage }
                                        ?.takeIf { it.isNotBlank() }
                                        ?: format.label
                                        ?: context.getString(R.string.audio_track_number_format, aSet.size + 1)
                                    aSet.add(lang)
                                    if (group.isTrackSelected(i)) {
                                        selectedAudioTrack = lang
                                    }
                                }
                            } else if (group.type == C.TRACK_TYPE_TEXT) {
                                for (i in 0 until group.length) {
                                    val format = group.getTrackFormat(i)
                                    val lang = format.language?.let { Locale(it).displayLanguage }
                                        ?.takeIf { it.isNotBlank() }
                                        ?: context.getString(R.string.subtitle_track_number_format, sSet.size)

                                    val props = mutableListOf<String>()
                                    if ((format.selectionFlags and C.SELECTION_FLAG_FORCED) != 0) {
                                        props.add("Forced")
                                    }
                                    if ((format.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0) {
                                        props.add("Default")
                                    }
                                    if ((format.roleFlags and C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND) != 0 || (format.roleFlags and C.ROLE_FLAG_CAPTION) != 0) {
                                        props.add("SDH")
                                    }
                                    val label = format.label
                                    if (!label.isNullOrBlank()) {
                                        if (label.contains("forced", ignoreCase = true) && !props.contains("Forced")) {
                                            props.add("Forced")
                                        }
                                        if (label.contains("sdh", ignoreCase = true) && !props.contains("SDH")) {
                                            props.add("SDH")
                                        }
                                        if (label.contains("cc", ignoreCase = true) && !props.contains("CC")) {
                                            props.add("CC")
                                        }
                                    }

                                    val trackName = if (props.isNotEmpty()) {
                                        "$lang [${props.joinToString(", ")}]"
                                    } else if (!label.isNullOrBlank() && !label.equals(lang, ignoreCase = true)) {
                                        "$lang ($label)"
                                    } else {
                                        lang
                                    }

                                    sSet.add(trackName)
                                    if (group.isTrackSelected(i)) {
                                        selectedSubtitleTrack = trackName
                                    }
                                }
                            }
                        }
                        if (aSet.isNotEmpty()) availableAudioTracks = aSet.toList()
                        // "Off" is always present, so this replaces the list even
                        // when the stream ships a single subtitle track.
                        availableSubtitleTracks = sSet.toList()
                    }
                })
            }
        exoPlayer = player

        onDispose {
            // Persist before the player goes away, otherwise backing out of an
            // episode loses everything played since the last periodic save.
            if (player.duration > 0L) {
                onProgressUpdate(player.currentPosition, player.duration)
            }
            player.release()
        }
    }

    // Lifecycle Observer to handle screen lock / background resume without unloading
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer?.let { player ->
                        savedPlaybackPosition = player.currentPosition
                        player.pause()
                    }
                }

                // A TV app that is not an audio app must not keep media
                // resources alive once it is no longer visible. Stopping the
                // player drops its decoder buffers and queued media while
                // keeping the instance, so returning is a re-prepare rather
                // than a full re-create.
                Lifecycle.Event.ON_STOP -> {
                    exoPlayer?.let { player ->
                        savedPlaybackPosition = player.currentPosition
                        player.pause()
                        player.stop()
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer?.let { player ->
                        if (player.playbackState == Player.STATE_IDLE) {
                            if (savedPlaybackPosition > 0L) {
                                player.seekTo(savedPlaybackPosition)
                            }
                            player.prepare()
                        }
                        player.playWhenReady = true
                    }
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // The overlay (and its focus targets) is removed while the controls are
    // hidden, so focus has to be parked back on a control when they reappear.
    val controlsFocusRequester = remember { FocusRequester() }
    // The overlay is removed from composition while it is hidden (or while a
    // playback error is shown), which used to drop D-pad focus entirely: no
    // node was focused, so remote keys (including OK) never reached this screen.
    // This sink is composed whenever the overlay is unavailable and restores it.
    val playerFocusSink = remember { FocusRequester() }
    // Landing targets inside the episodes drawer, so opening it actually moves
    // D-pad focus off the player buttons: the first episode row is the primary
    // target; the selected season chip is the fallback when the list is empty.
    val drawerFocusRequester = remember { FocusRequester() }
    val drawerEpisodeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isControlsVisible, playerError) {
        // The overlay's focus target leaves composition in the same frame the
        // controls hide, so the sink may not be attached on the first attempt.
        // Retrying is what makes OK bring the overlay back instead of being
        // swallowed while nothing is focused.
        repeat(3) {
            withFrameNanos { }
            val target =
                if (isControlsVisible && playerError == null) controlsFocusRequester
                else playerFocusSink
            if (runCatching { target.requestFocus() }.getOrDefault(false)) {
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(showEpisodesDrawer) {
        if (showEpisodesDrawer) {
            // The drawer mounts in the same frame it is opened, so no focus
            // target is attached yet. Retry across frames, and check the boolean
            // requestFocus() returns: it reports false without throwing while the
            // node is not ready, so runCatching's flag alone stopped the retry on
            // the first frame and focus stayed on the player controls.
            repeat(8) {
                withFrameNanos { }
                if (runCatching { drawerEpisodeFocusRequester.requestFocus() }.getOrDefault(false)) {
                    return@LaunchedEffect
                }
                if (runCatching { drawerFocusRequester.requestFocus() }.getOrDefault(false)) {
                    return@LaunchedEffect
                }
            }
        } else {
            repeat(3) {
                withFrameNanos { }
                val target =
                    if (isControlsVisible && playerError == null) controlsFocusRequester else null
                if (target != null &&
                    runCatching { target.requestFocus() }.getOrDefault(false)
                ) {
                    return@LaunchedEffect
                }
            }
        }
    }

    // Auto-hide controls timer. The drawer is part of the overlay, so it pins
    // the controls visible while it is open.
    LaunchedEffect(isControlsVisible, lastInteractionTime, showEpisodesDrawer) {
        if (isControlsVisible && !showEpisodesDrawer) {
            delay(8000)
            isControlsVisible = false
        }
    }

    // Periodic Progress & Prefetch / Countdown Loop
    LaunchedEffect(exoPlayer, upNextThreshold, nextEpisode, currentEpisodeNumber, currentSeasonNumber, episodes) {
        val preloadThreshold = (upNextThreshold - 5).coerceAtLeast(10)
        // Only offer up-next when an episode genuinely follows the current one.
        val hasNext = nextEpisode != null || episodes.any {
            (it.seasonNumber == (currentSeasonNumber ?: 1) && it.episodeNumber == (currentEpisodeNumber ?: 1) + 1) ||
                    (it.seasonNumber == (currentSeasonNumber ?: 1) + 1 && it.episodeNumber == 1)
        }
        while (true) {
            exoPlayer?.let { player ->
                val playerDuration = player.duration
                val playerPos = player.currentPosition

                // Only evaluate progress & popups when duration is valid and playback is ready
                if (player.playbackState == Player.STATE_READY && playerDuration > 60000L && playerPos >= 0L) {
                    currentPosition = playerPos
                    duration = playerDuration
                    // Room + Trakt were being written every 500 ms; the UI still
                    // updates at 500 ms but persistence runs at most every 10 s.
                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastProgressSave >= 10_000L) {
                        lastProgressSave = nowMs
                        onProgressUpdate(currentPosition, duration)
                    }

                    val progressPct = (currentPosition.toFloat() / duration.toFloat()) * 100f
                    val remainingMs = duration - currentPosition

                    // Reset trigger if user sought backwards below preload threshold or near the beginning
                    if (progressPct < preloadThreshold.toFloat() && remainingMs > 60000L) {
                        prefetchFired = false
                        userDismissedUpNext = false
                    }

                    // Must have played at least 30 seconds into the episode to prevent early triggering
                    if (currentPosition > 30000L) {
                        // Prefetch next stream at preload threshold or when < 60s remaining
                        if ((progressPct >= preloadThreshold.toFloat() || remainingMs <= 60000L) && !prefetchFired && hasNext) {
                            prefetchFired = true
                            onPrefetchTriggered()
                        }

                        // Show Up Next Popup at upNextThreshold or when < 35s remaining
                        if ((progressPct >= upNextThreshold.toFloat() || remainingMs <= 35000L) && !showUpNextPopup && !userDismissedUpNext && hasNext) {
                            showUpNextPopup = true
                        }
                    }
                } else if (playerPos >= 0L) {
                    currentPosition = playerPos
                    if (playerDuration > 0L) {
                        duration = playerDuration
                    }
                }
            }
            delay(500)
        }
    }

    // Up Next Countdown Timer - Defaults to playing next episode automatically
    LaunchedEffect(showUpNextPopup) {
        if (showUpNextPopup) {
            upNextCountdown = 20
            while (upNextCountdown > 0) {
                delay(1000)
                upNextCountdown--
            }
            showUpNextPopup = false
            // Hand off to the single advance path below so the countdown and
            // end-of-media cannot both start the next episode.
            if (!autoAdvanceHandled) reachedEnd = true
        }
    }

    // Single advance path: resolves the next episode with fresh parameters and
    // advances at most once per episode.
    LaunchedEffect(reachedEnd, nextEpisode, episodes, currentSeasonNumber, currentEpisodeNumber) {
        if (!reachedEnd || autoAdvanceHandled) return@LaunchedEffect
        val next = nextEpisode
            ?: episodes.firstOrNull {
                it.seasonNumber == (currentSeasonNumber ?: 1) &&
                        it.episodeNumber == (currentEpisodeNumber ?: 1) + 1
            }
            ?: episodes.firstOrNull {
                it.seasonNumber == (currentSeasonNumber ?: 1) + 1 && it.episodeNumber == 1
            }
        // On a finale there is nothing to play, so stay paused on the last frame
        // instead of requesting an episode that does not exist.
        if (next != null) {
            autoAdvanceHandled = true
            onPlayNextEpisode(next)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    lastInteractionTime = System.currentTimeMillis()
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                        AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (!isControlsVisible) {
                                isControlsVisible = true
                                true
                            } else false
                        }

                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        AndroidKeyEvent.KEYCODE_MEDIA_PLAY,
                        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            exoPlayer?.let {
                                if (it.isPlaying) it.pause() else it.play()
                            }
                            isControlsVisible = true
                            true
                        }

                        AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                            exoPlayer?.seekTo((currentPosition - 10000).coerceAtLeast(0L))
                            isControlsVisible = true
                            true
                        }

                        AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            exoPlayer?.seekTo((currentPosition + 10000).coerceAtMost(duration))
                            isControlsVisible = true
                            true
                        }

                        AndroidKeyEvent.KEYCODE_DPAD_UP,
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!isControlsVisible) {
                                isControlsVisible = true
                                true
                            } else false
                        }

                        else -> false
                    }
                } else false
            }
            // Pointer-only: Modifier.clickable here added a focus node above the
            // controls that swallowed DPAD_CENTER, so the remote toggled the
            // overlay instead of activating a button.
            .pointerInput(Unit) {
                detectTapGestures {
                    isControlsVisible = !isControlsVisible
                    if (isControlsVisible) lastInteractionTime = System.currentTimeMillis()
                }
            }
    ) {
        // ExoPlayer Video Surface with Fit Aspect Ratio
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    // Never let the TV OS draw a default (light) focus highlight
                    // over the video surface.
                    isFocusable = false
                    this.player = exoPlayer
                    subtitleView?.apply {
                        setUserDefaultStyle()
                        setUserDefaultTextSize()
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                view.keepScreenOn = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // Playback error surface: without it an unplayable debrid link spins forever.
        playerError?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NetflixBlack.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(420.dp)
                ) {
                    Text(
                        text = stringResource(R.string.player_error_title),
                        color = NetflixWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        color = NetflixLightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                playerError = null
                                reloadKey++
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = NetflixWhite,
                                contentColor = NetflixBlack,
                                focusedContainerColor = NetflixWhite,
                                focusedContentColor = NetflixBlack
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = stringResource(R.string.action_retry),
                                color = NetflixBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.colors(
                                containerColor = Color(0xFF333333),
                                contentColor = NetflixWhite,
                                focusedContainerColor = NetflixRed,
                                focusedContentColor = NetflixWhite
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = stringResource(R.string.action_back),
                                color = NetflixWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Focus holder whenever the visible overlay is unavailable. It cannot
        // steal focus from the overlay because it only exists in that state.
        if (!isControlsVisible || playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(playerFocusSink)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isControlsVisible = !isControlsVisible
                        lastInteractionTime = System.currentTimeMillis()
                    }
            )
        }

        // Loading Spinner (When controls are hidden)
        if (isBuffering && !isControlsVisible) {
            TvSpinner(
                color = NetflixRed,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Custom Netflix Player Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible && playerError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NetflixBlack.copy(alpha = 0.85f),
                                Color.Transparent,
                                NetflixBlack.copy(alpha = 0.95f)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        var backFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                onBack()
                            },
                            modifier = Modifier
                                .background(
                                    if (backFocused) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                                    CircleShape
                                )
                                .border(if (backFocused) 2.dp else 0.dp, NetflixWhite, CircleShape)
                                .onFocusChanged {
                                    backFocused = it.isFocused; if (it.isFocused) lastInteractionTime =
                                    System.currentTimeMillis()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = NetflixWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = if (!subtitle.isNullOrBlank()) "$subtitle · $title" else title,
                            color = NetflixWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    var castFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
                            /* Cast trigger */
                        },
                        modifier = Modifier
                            .background(
                                if (castFocused) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                                CircleShape
                            )
                            .border(if (castFocused) 2.dp else 0.dp, NetflixWhite, CircleShape)
                            .onFocusChanged {
                                castFocused = it.isFocused; if (it.isFocused) lastInteractionTime =
                                System.currentTimeMillis()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Chromecast",
                            tint = NetflixWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Center Play / Rewind 10s / Forward 10s Controls
                // Up Next owns the lower-right corner once it appears; keeping the
                // transport row behind it made the two cards overlap on short
                // landscape phones.
                if (!showUpNextPopup) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var rewFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = {
                                exoPlayer?.seekTo((currentPosition - 10000).coerceAtLeast(0L))
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    if (rewFocused) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                                    CircleShape
                                )
                                .border(if (rewFocused) 2.dp else 0.dp, NetflixWhite, CircleShape)
                                .onFocusChanged {
                                    rewFocused = it.isFocused; if (it.isFocused) lastInteractionTime =
                                    System.currentTimeMillis()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = stringResource(R.string.action_rewind_10s),
                                tint = NetflixWhite,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        var playFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .background(NetflixBlack.copy(alpha = 0.35f), CircleShape)
                                .border(if (playFocused) 3.dp else 0.dp, NetflixWhite, CircleShape)
                                .onFocusChanged {
                                    playFocused = it.isFocused; if (it.isFocused) lastInteractionTime =
                                    System.currentTimeMillis()
                                }
                                .focusRequester(controlsFocusRequester)
                                .clickable {
                                    exoPlayer?.let {
                                        if (it.isPlaying) it.pause() else it.play()
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBuffering) {
                                TvSpinner(
                                    color = NetflixWhite,
                                    size = 52.dp,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) stringResource(R.string.action_pause) else stringResource(
                                        R.string.action_play
                                    ),
                                    tint = NetflixWhite,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        var fwdFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = {
                                exoPlayer?.seekTo((currentPosition + 10000).coerceAtMost(duration))
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    if (fwdFocused) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                                    CircleShape
                                )
                                .border(if (fwdFocused) 2.dp else 0.dp, NetflixWhite, CircleShape)
                                .onFocusChanged {
                                    fwdFocused = it.isFocused; if (it.isFocused) lastInteractionTime =
                                    System.currentTimeMillis()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = stringResource(R.string.action_forward_10s),
                                tint = NetflixWhite,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                }

                // Bottom Overlay: Timeline + Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // D-pad seek bar: LEFT/RIGHT scrub 10s, OK toggles playback.
                    TvSeekBar(
                        positionMs = currentPosition,
                        durationMs = duration,
                        modifier = Modifier.fillMaxWidth(),
                        onSeekTo = { target ->
                            exoPlayer?.seekTo(target)
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onTogglePlayPause = {
                            exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        formatTime = ::formatTime
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom Actions Bar (Speed, Episodes, Audio & Subtitles, Next Ep.)
                    // Netflix uses flat label buttons here, not filled pills.
                    val effectiveNext =
                        nextEpisode
                            ?: episodes.firstOrNull {
                                it.seasonNumber == (currentSeasonNumber ?: 1) &&
                                        it.episodeNumber == (currentEpisodeNumber ?: 1) + 1
                            }
                            ?: episodes.firstOrNull {
                                it.seasonNumber == (currentSeasonNumber ?: 1) + 1 && it.episodeNumber == 1
                            }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val speedLabel = if (currentSpeed == 1f) "1x" else "${currentSpeed}x"
                        PlayerAction(
                            icon = Icons.Default.Speed,
                            label = stringResource(R.string.action_speed, speedLabel),
                            onClick = { showSpeedMenu = true },
                            onInteracted = { lastInteractionTime = System.currentTimeMillis() }
                        )

                        if (showSpeedMenu) {
                            TvOptionMenuDialog(
                                title = stringResource(R.string.action_speed, speedLabel),
                                options = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f),
                                selected = currentSpeed,
                                optionLabel = { speed -> "${speed}x" },
                                onSelect = { speed ->
                                    currentSpeed = speed
                                    exoPlayer?.setPlaybackSpeed(speed)
                                    showSpeedMenu = false
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDismiss = { showSpeedMenu = false }
                            )
                        }

                        if (episodes.isNotEmpty() || currentEpisodeNumber != null || nextEpisode != null) {
                            PlayerAction(
                                icon = Icons.Default.VideoLibrary,
                                label = stringResource(R.string.title_episodes),
                                onClick = {
                                    drawerSeason = currentSeasonNumber
                                        ?: episodes.firstOrNull()?.seasonNumber
                                    showEpisodesDrawer = true
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onInteracted = { lastInteractionTime = System.currentTimeMillis() }
                            )
                        }

                        PlayerAction(
                            icon = Icons.Default.ChatBubbleOutline,
                            label = stringResource(R.string.action_audio_subtitles),
                            onClick = { showAudioSubtitlesDialog = true },
                            onInteracted = { lastInteractionTime = System.currentTimeMillis() }
                        )

                        if (effectiveNext != null) {
                            PlayerAction(
                                icon = Icons.Default.SkipNext,
                                label = stringResource(R.string.action_next_episode),
                                onClick = { onPlayNextEpisode(effectiveNext) },
                                onInteracted = { lastInteractionTime = System.currentTimeMillis() }
                            )
                        }
                    }
                }
            }
        }

        // Audio & Subtitles Selection Dialog (Strictly matching Image 2)
        if (showAudioSubtitlesDialog) {
            AudioSubtitlesSelectionDialog(
                selectedAudio = selectedAudioTrack,
                selectedSubtitle = selectedSubtitleTrack,
                audioTracks = availableAudioTracks,
                subtitleTracks = availableSubtitleTracks,
                onDismiss = { showAudioSubtitlesDialog = false },
                onApply = { audio, subtitleTrack ->
                    selectedAudioTrack = audio
                    selectedSubtitleTrack = subtitleTrack
                    showAudioSubtitlesDialog = false

                    // Apply track selection to ExoPlayer
                    exoPlayer?.let { player ->
                        fun parseLangCode(name: String): String {
                            val lower = name.lowercase()
                            return when {
                                lower.contains("portuguese") || lower.contains("português") || lower == "pt" -> "pt"
                                lower.contains("english") || lower.contains("inglês") || lower == "en" -> "en"
                                lower.contains("spanish") || lower.contains("español") || lower == "es" -> "es"
                                lower.contains("french") || lower.contains("francês") || lower == "fr" -> "fr"
                                lower.contains("german") || lower.contains("alemão") || lower == "de" -> "de"
                                lower.contains("italian") || lower.contains("italiano") || lower == "it" -> "it"
                                lower.contains("japanese") || lower.contains("japonês") || lower == "ja" -> "ja"
                                lower.contains("chinese") || lower.contains("chinês") || lower == "zh" -> "zh"
                                lower.contains("russian") || lower.contains("russo") || lower == "ru" -> "ru"
                                lower.contains("arabic") || lower.contains("árabe") || lower == "ar" -> "ar"
                                else -> name.take(2).lowercase()
                            }
                        }

                        val builder = player.trackSelectionParameters.buildUpon()
                        if (subtitleTrack == "Off") {
                            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        } else {
                            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            var overrideApplied = false
                            for (group in player.currentTracks.groups) {
                                if (group.type == C.TRACK_TYPE_TEXT) {
                                    for (i in 0 until group.length) {
                                        val format = group.getTrackFormat(i)
                                        val langName = format.language?.let { Locale(it).displayLanguage }
                                            ?.takeIf { it.isNotBlank() }
                                            ?: format.label
                                            ?: "Subtitle $i"
                                        val isMatch = langName.equals(subtitleTrack, ignoreCase = true) ||
                                                (format.label != null && format.label.equals(
                                                    subtitleTrack,
                                                    ignoreCase = true
                                                )) ||
                                                subtitleTrack.startsWith(langName, ignoreCase = true) ||
                                                (format.label != null && subtitleTrack.contains(
                                                    format.label!!,
                                                    ignoreCase = true
                                                ))
                                        if (isMatch) {
                                            builder.addOverride(
                                                androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i)
                                            )
                                            overrideApplied = true
                                            break
                                        }
                                    }
                                    if (overrideApplied) break
                                }
                            }
                            if (!overrideApplied) {
                                val cleanSubtitleName = subtitleTrack.takeWhile { it != '[' && it != '(' }.trim()
                                builder.setPreferredTextLanguage(parseLangCode(cleanSubtitleName))
                            }
                        }

                        if (audio.isNotBlank()) {
                            builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            var overrideApplied = false
                            for (group in player.currentTracks.groups) {
                                if (group.type == C.TRACK_TYPE_AUDIO) {
                                    for (i in 0 until group.length) {
                                        val format = group.getTrackFormat(i)
                                        val langName = format.language?.let { Locale(it).displayLanguage }
                                            ?.takeIf { it.isNotBlank() }
                                            ?: format.label
                                            ?: "Audio $i"
                                        if (langName.equals(
                                                audio,
                                                ignoreCase = true
                                            ) || (format.label != null && format.label.equals(audio, ignoreCase = true))
                                        ) {
                                            builder.addOverride(
                                                androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i)
                                            )
                                            overrideApplied = true
                                            break
                                        }
                                    }
                                    if (overrideApplied) break
                                }
                            }
                            if (!overrideApplied) {
                                builder.setPreferredAudioLanguage(parseLangCode(audio))
                            }
                        }
                        player.trackSelectionParameters = builder.build()
                    }
                }
            )
        }

        // In-Player Episodes Selector Drawer (Netflix 2020 era)
        if (showEpisodesDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NetflixBlack.copy(alpha = 0.65f))
                    // Pointer-only so the scrim never becomes a focus target
                    // inside the drawer's focus group.
                    .pointerInput(Unit) {
                        detectTapGestures { showEpisodesDrawer = false }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(360.dp)
                        .background(NetflixDarkSurface.copy(alpha = 0.98f))
                        // Consume taps inside the sheet without dimming or
                        // closing it, and without adding a focus target.
                        .pointerInput(Unit) {
                            detectTapGestures { }
                        }
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.episodes_and_info),
                                color = NetflixWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showEpisodesDrawer = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = NetflixLightGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val distinctEpisodes = episodes.distinctBy { it.id }
                        val drawerSeasons =
                            (seasons.map { it.seasonNumber } + distinctEpisodes.map { it.seasonNumber })
                                .distinct()
                                .sorted()
                        // Never open on a season that has no episodes, and default
                        // to the season being watched.
                        val activeSeason = drawerSeason
                            ?: currentSeasonNumber
                            ?: drawerSeasons.firstOrNull()
                        val shownEpisodes = activeSeason?.let { season ->
                            distinctEpisodes.filter { it.seasonNumber == season }
                        } ?: distinctEpisodes

                        if (drawerSeasons.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().focusRestorer(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(drawerSeasons) { seasonNumber ->
                                    val isSelected = seasonNumber == activeSeason
                                    var chipFocused by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSelected) NetflixRed else NetflixDarkSurface
                                            )
                                            .border(
                                                width = if (chipFocused) 2.dp else 0.dp,
                                                color = if (chipFocused) NetflixWhite else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .onFocusChanged { chipFocused = it.isFocused }
                                            // focusRequester must precede clickable: attached
                                            // after it, the requester sits inside the focus
                                            // target, so requestFocus() silently fails.
                                            .then(
                                                if (isSelected) {
                                                    Modifier.focusRequester(drawerFocusRequester)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                drawerSeason = seasonNumber
                                                lastInteractionTime = System.currentTimeMillis()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Season $seasonNumber",
                                            color = NetflixWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (shownEpisodes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.loading_episodes),
                                    color = NetflixLightGray,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().focusRestorer(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    shownEpisodes,
                                    key = { _, ep -> "${ep.seasonNumber}_${ep.episodeNumber}" }
                                ) { index, ep ->
                                    val isCurrent = ep.seasonNumber == (currentSeasonNumber
                                        ?: 1) && ep.episodeNumber == (currentEpisodeNumber ?: 1)
                                    val progress = episodeProgressMap["s${ep.seasonNumber}_e${ep.episodeNumber}"] ?: 0f
                                    val isWatched = progress >= (completionThreshold / 100f)
                                    var epItemFocused by remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (index == 0) {
                                                    Modifier.focusRequester(drawerEpisodeFocusRequester)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (epItemFocused) Color.White.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = if (epItemFocused) 2.dp else 0.dp,
                                                color = if (epItemFocused) NetflixWhite else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .onFocusChanged { epItemFocused = it.isFocused }
                                            .clickable {
                                                showEpisodesDrawer = false
                                                if (onSelectEpisode != null) {
                                                    onSelectEpisode(ep.seasonNumber, ep.episodeNumber)
                                                } else {
                                                    onPlayNextEpisode(ep)
                                                }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(112.dp)
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(NetflixBlack)
                                        ) {
                                            AsyncImage(
                                                model = ep.fullStillUrl,
                                                contentDescription = ep.title,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )

                                            if (isWatched || progress > 0.01f) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxWidth()
                                                        .height(3.dp)
                                                        .background(NetflixLightGray.copy(alpha = 0.3f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(
                                                                if (isWatched) 1f
                                                                else progress.coerceIn(0f, 1f)
                                                            )
                                                            .fillMaxHeight()
                                                            .background(NetflixRed)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(28.dp)
                                                    .background(NetflixRed, RoundedCornerShape(1.5.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${ep.episodeNumber}. ${ep.title}",
                                                color = if (isCurrent) NetflixRed else NetflixWhite,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            if (isCurrent) {
                                                Text(
                                                    text = stringResource(R.string.playing_now),
                                                    color = NetflixRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            } else {
                                                Text(
                                                    text = stringResource(
                                                        R.string.duration_minutes_short_format,
                                                        ep.durationMinutes
                                                    ),
                                                    color = NetflixLightGray,
                                                    fontSize = 11.sp
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
        }

        // Up Next Modal Popup Overlay
        val displayNextEpisode = nextEpisode
            ?: episodes.find {
                it.seasonNumber == (currentSeasonNumber ?: 1) && it.episodeNumber == (currentEpisodeNumber ?: 1) + 1
            }
            ?: episodes.find { it.seasonNumber == (currentSeasonNumber ?: 1) + 1 && it.episodeNumber == 1 }
            ?: episodes.find { it.episodeNumber == (currentEpisodeNumber ?: 1) + 1 }
        if (showUpNextPopup && displayNextEpisode != null) {
            val nextEpisodeSoon = stringResource(R.string.next_episode_soon)
            val nextEpTitle = displayNextEpisode?.title
                ?: stringResource(R.string.episode_simple, (currentEpisodeNumber ?: 1) + 1)
            val nextEpSeason = displayNextEpisode?.seasonNumber ?: currentSeasonNumber ?: 1
            val nextEpNum = displayNextEpisode?.episodeNumber ?: ((currentEpisodeNumber ?: 1) + 1)
            val nextEpStill = displayNextEpisode?.fullStillUrl ?: nextEpisodeStillUrl
            val nextEpOverview = displayNextEpisode?.overview?.ifBlank { nextEpisodeSoon }
                ?: nextEpisodeSoon
            val nextEpAirDate = displayNextEpisode?.airDate

            val triggerPlayNext = {
                showUpNextPopup = false
                if (displayNextEpisode != null) {
                    onPlayNextEpisode(displayNextEpisode)
                }
            }

            if (isMobile) {
                // Horizontal Compact Card for Mobile
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 16.dp,
                            bottom = if (isControlsVisible) 148.dp else 14.dp
                        )
                        .width(320.dp)
                        .background(NetflixBlack.copy(alpha = 0.88f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .clickable { triggerPlayNext() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Thumbnail (16:9) with border and play button
                        Box(
                            modifier = Modifier
                                .width(108.dp)
                                .aspectRatio(16f / 9f)
                                .border(1.5.dp, NetflixWhite, RoundedCornerShape(2.dp))
                                .clip(RoundedCornerShape(2.dp))
                                .background(NetflixDarkSurface)
                        ) {
                            AsyncImage(
                                model = nextEpStill,
                                contentDescription = nextEpTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(28.dp)
                                    .background(NetflixBlack.copy(alpha = 0.5f), CircleShape)
                                    .border(1.5.dp, NetflixWhite, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.action_play_next),
                                    tint = NetflixWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(3.dp)
                                    .background(NetflixBlack.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.season_episode_tag_format,
                                        nextEpSeason,
                                        nextEpNum
                                    ),
                                    color = NetflixWhite,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Info Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    color = NetflixWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        showUpNextPopup = false
                                        userDismissedUpNext = true
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.close),
                                        tint = NetflixLightGray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (upNextCountdown == 1) {
                                    stringResource(R.string.starting_in_one_second)
                                } else {
                                    stringResource(R.string.starting_in_seconds, upNextCountdown)
                                },
                                color = NetflixRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = nextEpTitle,
                                color = NetflixLightGray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // Vertical Rich Card for TV & Large Screens
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 40.dp,
                            bottom = if (isControlsVisible) 160.dp else 28.dp
                        )
                        .width(310.dp)
                        .background(NetflixBlack.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        // Header: Show Title & Countdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = NetflixWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (upNextCountdown == 1) {
                                        stringResource(R.string.starting_in_one_second)
                                    } else {
                                        stringResource(R.string.starting_in_seconds, upNextCountdown)
                                    },
                                    color = NetflixWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            IconButton(
                                onClick = {
                                    showUpNextPopup = false
                                    userDismissedUpNext = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = NetflixLightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Thumbnail with Solid White Border and Center Play Button
                        var thumbnailFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .border(
                                    2.dp,
                                    if (thumbnailFocused) NetflixRed else NetflixWhite,
                                    RoundedCornerShape(2.dp)
                                )
                                .clip(RoundedCornerShape(2.dp))
                                .background(NetflixDarkSurface)
                                .onFocusChanged { thumbnailFocused = it.isFocused }
                                .clickable { triggerPlayNext() }
                        ) {
                            AsyncImage(
                                model = nextEpStill,
                                contentDescription = nextEpTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Center Play Button Icon with Outlined Circle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(50.dp)
                                    .background(NetflixBlack.copy(alpha = 0.45f), CircleShape)
                                    .border(2.5.dp, NetflixWhite, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.action_play_next),
                                    tint = NetflixWhite,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Bottom-Left S1: E2 Label
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                                    .background(NetflixBlack.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.season_episode_tag_format,
                                        nextEpSeason,
                                        nextEpNum
                                    ),
                                    color = NetflixWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Episode Title
                        Text(
                            text = nextEpTitle,
                            color = NetflixWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Optional Air Date / Meta line
                        if (!nextEpAirDate.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = nextEpAirDate,
                                color = NetflixLightGray,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Overview Description
                        Text(
                            text = nextEpOverview,
                            color = NetflixLightGray,
                            fontSize = 11.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    onInteracted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) NetflixWhite else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onInteracted()
            }
            // `clickable` already makes this node focusable and consumes
            // DPAD_CENTER. An extra `focusable()` created a second focus target
            // that swallowed D-pad focus without ever handling activation, which
            // is why TV remotes could not select any player control.
            .clickable {
                onInteracted()
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NetflixWhite,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = NetflixWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AudioSubtitlesSelectionDialog(
    selectedAudio: String,
    selectedSubtitle: String,
    audioTracks: List<String>,
    subtitleTracks: List<String>,
    onDismiss: () -> Unit,
    onApply: (audio: String, subtitle: String) -> Unit
) {
    // Only tracks the stream actually exposes are offered; there are no invented languages.
    val audioList = audioTracks
    val subtitleList = subtitleTracks.ifEmpty { listOf("Off") }

    var tempAudio by remember {
        mutableStateOf(audioList.firstOrNull { it == selectedAudio } ?: audioList.firstOrNull() ?: "")
    }
    var tempSubtitle by remember {
        mutableStateOf(subtitleList.firstOrNull { it == selectedSubtitle } ?: subtitleList.first())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.88f),
            colors = SurfaceDefaults.colors(containerColor = NetflixBlack),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 60.dp)
                ) {
                    // Audio Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.audio_track_title),
                            color = NetflixWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.focusRestorer(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(audioList) { audio ->
                                val isSelected = audio == tempAudio
                                var rowFocused by remember { mutableStateOf(false) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (rowFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = if (rowFocused) 2.dp else 0.dp,
                                            color = if (rowFocused) NetflixWhite else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .onFocusChanged { rowFocused = it.isFocused }
                                        .clickable { tempAudio = audio }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.accessibility_selected),
                                            tint = NetflixWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(28.dp))
                                    }

                                    Text(
                                        text = audio,
                                        color = if (isSelected) NetflixWhite else NetflixMutedGray,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Subtitles Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.subtitle_track_title),
                            color = NetflixWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.focusRestorer(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(subtitleList) { sub ->
                                val isSelected = sub == tempSubtitle
                                var subRowFocused by remember { mutableStateOf(false) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (subRowFocused) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = if (subRowFocused) 2.dp else 0.dp,
                                            color = if (subRowFocused) NetflixWhite else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .onFocusChanged { subRowFocused = it.isFocused }
                                        .clickable { tempSubtitle = sub }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.accessibility_selected),
                                            tint = NetflixWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(28.dp))
                                    }

                                    Text(
                                        text = if (sub == "Off") stringResource(R.string.track_off) else sub,
                                        color = if (isSelected) NetflixWhite else NetflixMutedGray,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Right Action Buttons (Cancel & Apply)
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFF333333),
                            contentColor = NetflixWhite,
                            focusedContainerColor = NetflixRed,
                            focusedContentColor = NetflixWhite
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = NetflixWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onApply(tempAudio, tempSubtitle) },
                        colors = ButtonDefaults.colors(
                            containerColor = NetflixWhite,
                            contentColor = NetflixBlack,
                            focusedContainerColor = NetflixRed,
                            focusedContentColor = NetflixWhite
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = stringResource(R.string.action_apply),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
