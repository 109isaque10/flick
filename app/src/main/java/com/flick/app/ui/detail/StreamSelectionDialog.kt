package com.flick.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning

import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.flick.app.ui.components.touchClickable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flick.app.R
import com.flick.app.data.model.StremioStream
import com.flick.app.data.model.providerName
import com.flick.app.ui.components.TvInputCard
import com.flick.app.ui.components.TvSpinner
import com.flick.app.ui.theme.NetflixBorderHighlight
import com.flick.app.ui.theme.NetflixCardSurface
import com.flick.app.ui.theme.NetflixDarkSurface
import com.flick.app.ui.theme.NetflixDimens
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.theme.NetflixWhite

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamSelectionDialog(
    mediaTitle: String,
    subtitle: String?,
    streams: List<StremioStream>?,
    isLoading: Boolean,
    onStreamSelected: (StremioStream) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {

    // The dialog is entered from the detail page or the player; land on the
    // first source so one OK press starts playback.
    val firstStreamRequester = remember { FocusRequester() }
    LaunchedEffect(streams?.isNotEmpty() == true) {
        if (streams?.isNotEmpty() == true) {
            withFrameNanos { }
            runCatching { firstStreamRequester.requestFocus() }
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(NetflixDimens.BigCardRadius),
            colors = SurfaceDefaults.colors(containerColor = NetflixDarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, NetflixBorderHighlight, RoundedCornerShape(NetflixDimens.BigCardRadius))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.select_stream_title),
                            color = NetflixWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val contextLine = buildString {
                            append(mediaTitle)
                            if (!subtitle.isNullOrBlank()) {
                                append(" \u2022 ")
                                append(subtitle)
                            }
                        }
                        Text(
                            text = contextLine,
                            color = NetflixLightGray,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(NetflixCardSurface, RoundedCornerShape(20.dp))
                            .size(36.dp)
                            .touchClickable { onDismiss() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = NetflixWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TvSpinner(
                                color = NetflixRed,
                                size = 42.dp,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.searching_addon_streams),
                                color = NetflixLightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (streams.isNullOrEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(NetflixCardSurface, RoundedCornerShape(NetflixDimens.BigCardRadius))
                                .padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NetflixRed,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_streams_found),
                                color = NetflixWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.no_streams_hint),
                                color = NetflixLightGray,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            var customUrlInput by remember { mutableStateOf("") }
                            TvInputCard(
                                label = stringResource(R.string.custom_stream_label),
                                value = customUrlInput,
                                placeholder = stringResource(R.string.custom_stream_placeholder),
                                onValueChange = { customUrlInput = it },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val playCustomUrl: () -> Unit = {
                                if (customUrlInput.isNotBlank()) {
                                    onStreamSelected(
                                        StremioStream(
                                            name = "Custom Link",
                                            title = "User Direct URL",
                                            url = customUrlInput
                                        )
                                    )
                                }
                            }
                            val openSettings: () -> Unit = {
                                onDismiss()
                                onNavigateToSettings()
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = playCustomUrl,
                                    colors = ButtonDefaults.colors(containerColor = NetflixRed),
                                    shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
                                    modifier = Modifier.touchClickable(onClick = playCustomUrl)
                                ) {
                                    Text(stringResource(R.string.play_custom_url), fontSize = 13.sp)
                                }

                                Button(
                                    onClick = openSettings,
                                    colors = ButtonDefaults.colors(containerColor = NetflixCardSurface),
                                    shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
                                    modifier = Modifier.touchClickable(onClick = openSettings)
                                ) {
                                    Text(stringResource(R.string.open_settings), fontSize = 13.sp, color = NetflixWhite)
                                }
                            }
                        }
                    }
                } else {
                    // Streams Available
                    Text(
                        text = stringResource(R.string.streams_available_count_format, streams.size),
                        color = NetflixLightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(streams) { index, stream ->
                            StreamCardItem(
                                stream = stream,
                                isDefault = index == 0,
                                onClick = { onStreamSelected(stream) },
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstStreamRequester)
                                } else {
                                    Modifier
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamCardItem(
    stream: StremioStream,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val badges = remember(stream) { parseStreamBadges(stream) }
    val provider = stream.providerName
    val shape = RoundedCornerShape(2.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(
                if (isFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) NetflixWhite else Color.Transparent,
                shape = shape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        if (isDefault) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(56.dp)
                    .background(NetflixRed)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            badges.resolution?.let { resolution ->
                StreamOutlineTag(text = resolution)
                Spacer(modifier = Modifier.width(12.dp))
            }

            val technical = listOfNotNull(badges.codec, badges.bitrate).joinToString(" \u2022 ")
            Text(
                text = technical.ifEmpty { stream.name ?: "" },
                color = NetflixLightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            badges.audio?.let { audio ->
                Spacer(modifier = Modifier.width(10.dp))
                StreamOutlineTag(text = audio)
            }

            Spacer(modifier = Modifier.weight(1f))

            val meta = listOfNotNull(badges.size, provider).joinToString(" \u2022 ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    color = NetflixLightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StreamOutlineTag(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, NetflixLightGray.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = NetflixWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class StreamBadges(
    val resolution: String?,
    val codec: String?,
    val bitrate: String?,
    val audio: String?,
    val size: String?
)

private val StreamBitrateRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:mbps|mb/s|mbit/s)""", RegexOption.IGNORE_CASE)
private val StreamSizeRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(gib|gb|mib|mb)\b""", RegexOption.IGNORE_CASE)
private val StreamAudioRegex = Regex("""\b([257]\.1)\b""")

/**
 * Heuristically derives the badges shown on a stream row from the addon-provided free text in
 * `name` / `description` / `title` / `behaviorHints.filename`. Every field is optional and may be
 * null; addons are not required to encode any of this.
 */
private fun parseStreamBadges(stream: StremioStream): StreamBadges {
    val text = listOfNotNull(
        stream.name,
        stream.description,
        stream.title,
        stream.behaviorHints?.filename
    ).joinToString(" ")

    val resolution = when {
        Regex("""\b(4k|2160p?|uhd|remux)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
            if (Regex("""\bhdr\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) "4K HDR" else "4K"

        Regex("""\b(1080p?|fhd)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "1080p"
        Regex("""\b(720p?)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "720p"
        Regex("""\b(480p?|360p?)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "SD"
        else -> null
    }

    val codec = when {
        Regex("""\b(hevc|x265|h\.?265)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "HEVC"
        Regex("""\bav1\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "AV1"
        Regex("""\b(x264|h\.?264|avc)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "H.264"
        else -> null
    }

    val bitrate = StreamBitrateRegex.find(text)?.groupValues?.get(1)?.let { raw ->
        "${raw.replace(',', '.').removeSuffix(".0")} Mbps"
    }

    val audio = when {
        Regex("""\batmos\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "Atmos"
        else -> StreamAudioRegex.find(text)?.groupValues?.get(1)
    }

    val size = StreamSizeRegex.find(text)?.let { match ->
        val value = match.groupValues[1].replace(',', '.')
        val unit = match.groupValues[2].uppercase().replace("IB", "B")
        "$value $unit"
    }

    return StreamBadges(resolution, codec, bitrate, audio, size)
}
