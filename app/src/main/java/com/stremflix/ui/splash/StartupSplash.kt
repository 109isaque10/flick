package com.stremflix.ui.splash

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremflix.ui.player.buildPlayerLoadControl
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.stremflix.R
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StartupSplash(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Check if user provided splash video in res/raw (e.g. R.raw.netflix_splash or R.raw.splash_video)
    val rawResId = remember(context) {
        val id1 = context.resources.getIdentifier("netflix_splash", "raw", context.packageName)
        if (id1 != 0) id1 else context.resources.getIdentifier("splash_video", "raw", context.packageName)
    }

    var isVideoFinished by remember { mutableStateOf(false) }

    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    if (rawResId != 0) {
        // Play provided splash video using ExoPlayer
        var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

        DisposableEffect(rawResId) {
            val player = ExoPlayer.Builder(context)
                .setLoadControl(buildPlayerLoadControl(context))
                .build().apply {
                val videoUri = Uri.parse("android.resource://${context.packageName}/$rawResId")
                setMediaItem(MediaItem.fromUri(videoUri))
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            isVideoFinished = true
                            onSplashFinished()
                        }
                    }
                })
            }
            exoPlayer = player
            onDispose {
                player.release()
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .clickable {
                    isVideoFinished = true
                    onSplashFinished()
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        // Neutralise OEM theme/canvas defaults: a light default
                        // background here reads as a white haze over the intro.
                        setBackgroundColor(android.graphics.Color.BLACK)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        isFocusable = false
                        this.player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Fallback: Netflix 'Tudum' Style Animated Logo Intro
        LaunchedEffect(Unit) {
            // Animate scale & alpha in
            alpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            scale.animateTo(1.15f, tween(1200, easing = FastOutSlowInEasing))
            delay(400)
            // Fade out
            alpha.animateTo(0f, tween(400))
            onSplashFinished()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixBlack)
                .clickable { onSplashFinished() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                color = NetflixRed,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .padding(16.dp)
            )
        }
    }
}

