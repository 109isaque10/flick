package com.stremflix.ui.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.stremflix.R
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite
import kotlinx.coroutines.delay

/**
 * Deliberately minimal trailer playback: video, a read-only progress bar and a
 * single play/pause control. No audio/subtitle switching, no seeking, no next
 * episode — a trailer is a short clip, not a viewing session.
 */
@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun TrailerPlayerScreen(
    streamUrl: String,
    title: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // Any remote-key press reveals the controls; nothing else is bound.
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableStateOf(0) }

    BackHandler(onBack = onBack)

    val exoPlayer = remember(streamUrl) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)
            )
            .build()
            .apply {
                setMediaItem(
                    androidx.media3.common.MediaItem.Builder()
                        .setUri(Uri.parse(streamUrl))
                        // The backend serves the manifest without a .mpd suffix, so
                        // the DASH source could not be inferred from the URI.
                        .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
                        .build()
                )
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) hasError = false
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        hasError = true
                    }
                })
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    LaunchedEffect(controlsVisible, interactionTick, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    // The play/pause button stays composed (and focused) even while the overlay
    // is hidden, so remote keys keep arriving here instead of dead-ending.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (!controlsVisible) {
                    controlsVisible = true
                    interactionTick++
                    true
                } else {
                    interactionTick++
                    false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                    this.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Tapping the video toggles the overlay.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    controlsVisible = !controlsVisible
                    interactionTick++
                }
        )

        if (hasError) {
            Text(
                text = stringResource(R.string.player_stream_error),
                color = NetflixWhite,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, NetflixBlack.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = NetflixWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        interactionTick++
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    when {
                        isBuffering -> CircularProgressIndicator(
                            color = NetflixWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )

                        else -> Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(
                                if (isPlaying) R.string.action_pause else R.string.action_play
                            ),
                            tint = NetflixWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Read-only progress: trailers are not resumable, so no seeking.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(NetflixLightGray.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
                ) {
                    if (durationMs > 0L) {
                        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(NetflixRed, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}
