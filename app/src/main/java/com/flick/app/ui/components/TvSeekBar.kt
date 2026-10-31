package com.flick.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.theme.NetflixWhite
import com.flick.app.ui.theme.isCompactScreen
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val SEEK_STEP_MS = 10_000L
private const val SEEK_DEBOUNCE_MS = 400L

/**
 * Playback scrub bar that adapts to the input device.
 *
 * `androidx.tv.material3` ships no `Slider`, and Material3's `Slider` cannot be
 * driven by a remote (it expects a continuous drag). On TV this renders a single
 * focus stop where LEFT/RIGHT scrub 10s and OK toggles playback; on phones it
 * keeps the draggable Material3 slider, so the mobile player is unchanged.
 */
@Composable
fun TvSeekBar(
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    formatTime: (Long) -> String,
    onInteraction: () -> Unit = {}
) {
    val safeDuration = durationMs.coerceAtLeast(1L)

    if (isCompactScreen()) {
        MobileSeekBar(
            positionMs = positionMs,
            safeDuration = safeDuration,
            modifier = modifier,
            onSeekTo = onSeekTo,
            onInteraction = onInteraction,
            formatTime = formatTime
        )
    } else {
        TvDpadSeekBar(
            positionMs = positionMs,
            safeDuration = safeDuration,
            modifier = modifier,
            onSeekTo = onSeekTo,
            onTogglePlayPause = onTogglePlayPause,
            onInteraction = onInteraction,
            formatTime = formatTime
        )
    }
}

/** Phone: the drag-to-scrub slider the mobile player already had. */
@Composable
private fun MobileSeekBar(
    positionMs: Long,
    safeDuration: Long,
    modifier: Modifier,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit,
    formatTime: (Long) -> String
) {
    var previewMs by remember { mutableStateOf<Long?>(null) }
    val shownMs = previewMs ?: positionMs

    // Drop the local preview once real playback catches up, so the bar cannot
    // stick on a rejected seek.
    LaunchedEffect(positionMs) {
        val target = previewMs ?: return@LaunchedEffect
        if (abs(positionMs - target) < 2_000L) previewMs = null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SeekTimeRow(positionMs = shownMs, durationMs = safeDuration, formatTime = formatTime)
        Slider(
            value = (shownMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f),
            onValueChange = {
                onInteraction()
                previewMs = (it * safeDuration).toLong()
            },
            onValueChangeFinished = {
                previewMs?.let(onSeekTo)
                previewMs = null
            },
            colors = SliderDefaults.colors(
                thumbColor = NetflixRed,
                activeTrackColor = NetflixRed,
                inactiveTrackColor = NetflixLightGray.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * TV: one focus stop. LEFT/RIGHT scrub, OK toggles playback. Seeks are debounced
 * so holding a direction does not fire one `seekTo` per key repeat and stall the
 * decoder. Netflix 2020 flavour: a plain line with no thumb, thicker while
 * focused rather than a scaling thumb.
 */
@Composable
private fun TvDpadSeekBar(
    positionMs: Long,
    safeDuration: Long,
    modifier: Modifier,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onInteraction: () -> Unit,
    formatTime: (Long) -> String
) {
    var focused by remember { mutableStateOf(false) }
    var previewMs by remember { mutableStateOf<Long?>(null) }
    val shownMs = previewMs ?: positionMs
    val fraction = (shownMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(previewMs) {
        val target = previewMs ?: return@LaunchedEffect
        delay(SEEK_DEBOUNCE_MS)
        onSeekTo(target)
    }

    LaunchedEffect(positionMs) {
        val target = previewMs ?: return@LaunchedEffect
        if (abs(positionMs - target) < 2_000L) previewMs = null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (focused && !state.isFocused) previewMs?.let(onSeekTo)
                focused = state.isFocused
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onInteraction()
                        previewMs = (shownMs - SEEK_STEP_MS).coerceIn(0L, safeDuration)
                        true
                    }

                    Key.DirectionRight -> {
                        onInteraction()
                        previewMs = (shownMs + SEEK_STEP_MS).coerceIn(0L, safeDuration)
                        true
                    }

                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onInteraction()
                        onTogglePlayPause()
                        true
                    }

                    else -> false
                }
            }
            .focusable()
    ) {
        SeekTimeRow(
            positionMs = shownMs,
            durationMs = safeDuration,
            formatTime = formatTime,
            highlighted = focused
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Fixed 8dp slot so gaining focus (thicker line) never reflows the
        // overlay around it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (focused) 8.dp else 4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        NetflixLightGray.copy(alpha = if (focused) 0.55f else 0.28f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(NetflixRed)
                )
            }
        }
    }
}

@Composable
private fun SeekTimeRow(
    positionMs: Long,
    durationMs: Long,
    formatTime: (Long) -> String,
    highlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(positionMs),
            color = if (highlighted) NetflixWhite else NetflixLightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = formatTime(durationMs),
            color = if (highlighted) NetflixWhite else NetflixLightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}