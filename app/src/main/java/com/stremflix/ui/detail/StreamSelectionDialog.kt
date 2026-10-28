package com.stremflix.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stremflix.R
import com.stremflix.data.model.StremioStream
import com.stremflix.ui.theme.NetflixBlack
import com.stremflix.ui.theme.NetflixBorderHighlight
import com.stremflix.ui.theme.NetflixCardSurface
import com.stremflix.ui.theme.NetflixDarkSurface
import com.stremflix.ui.theme.NetflixDimens
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixMutedGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite

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
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(NetflixDimens.BigCardRadius),
            color = NetflixDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, NetflixBorderHighlight, RoundedCornerShape(NetflixDimens.BigCardRadius))
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Stream",
                            color = NetflixRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = mediaTitle,
                            color = NetflixWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                color = NetflixLightGray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(NetflixCardSurface, RoundedCornerShape(20.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
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
                            CircularProgressIndicator(
                                color = NetflixRed,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Fetching streams from Stremio addon...",
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
                                contentDescription = "Warning",
                                tint = NetflixRed,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Streams Found",
                                color = NetflixWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Check your Stremio Addon URL in Settings or enter a custom stream URL below.",
                                color = NetflixLightGray,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            var customUrlInput by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = customUrlInput,
                                onValueChange = { customUrlInput = it },
                                placeholder = { Text("Direct stream URL (https://... or magnet:)", fontSize = 12.sp, color = NetflixMutedGray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NetflixRed,
                                    unfocusedBorderColor = NetflixMutedGray,
                                    focusedTextColor = NetflixWhite,
                                    unfocusedTextColor = NetflixWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        if (customUrlInput.isNotBlank()) {
                                            onStreamSelected(
                                                StremioStream(
                                                    name = "Custom Link",
                                                    title = "User Direct URL",
                                                    url = customUrlInput
                                                )
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Play Custom URL", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        onDismiss()
                                        onNavigateToSettings()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NetflixCardSurface),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Open Settings", fontSize = 13.sp, color = NetflixWhite)
                                }
                            }
                        }
                    }
                } else {
                    // Streams Available
                    Text(
                        text = "${streams.size} streams available from Stremio addon:",
                        color = NetflixLightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(streams) { stream ->
                            StreamCardItem(
                                stream = stream,
                                onClick = { onStreamSelected(stream) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamCardItem(
    stream: StremioStream,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    // Parse quality tag from stream name, description, title, or filename
    val descriptionText = stream.description ?: stream.title ?: ""
    val filenameText = stream.behaviorHints?.filename ?: ""
    val fullText = "${stream.name ?: ""} $descriptionText $filenameText"

    val qualityTag = when {
        fullText.contains("4K", ignoreCase = true) || 
        fullText.contains("2160p", ignoreCase = true) || 
        fullText.contains("2160", ignoreCase = true) || 
        fullText.contains("UHD", ignoreCase = true) || 
        fullText.contains("REMUX", ignoreCase = true) -> "4K"
        fullText.contains("1080p", ignoreCase = true) || fullText.contains("1080", ignoreCase = true) || fullText.contains("FHD", ignoreCase = true) -> "FHD"
        fullText.contains("480p", ignoreCase = true) || fullText.contains("480", ignoreCase = true) || fullText.contains("360p", ignoreCase = true) || fullText.contains("SD", ignoreCase = true) -> "SD"
        else -> "HD"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isFocused) Color.White.copy(alpha = 0.12f) else NetflixBlack,
                RoundedCornerShape(NetflixDimens.CardRadius)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) NetflixWhite else NetflixMutedGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(NetflixDimens.CardRadius)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quality Badge Box
        Box(
            modifier = Modifier
                .background(NetflixRed, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = qualityTag,
                color = NetflixWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stream.name ?: "Stremio Stream",
                color = NetflixWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val displaySubtitle = if (!stream.description.isNullOrBlank()) stream.description else stream.title
            if (!displaySubtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displaySubtitle,
                    color = NetflixLightGray,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(NetflixRed.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = NetflixRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
