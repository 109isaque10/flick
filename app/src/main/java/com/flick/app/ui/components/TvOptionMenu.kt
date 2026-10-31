package com.flick.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.flick.app.ui.theme.NetflixDarkSurface
import com.flick.app.ui.theme.NetflixLightGray
import com.flick.app.ui.theme.NetflixRed
import com.flick.app.ui.theme.NetflixWhite

/**
 * TV modal list replacing Material3's `DropdownMenu`, which anchors to touch
 * coordinates and hands D-pad focus to an unanchored popup.
 *
 * The dialog has no platform default width, opens with focus already on the
 * current value, and every row is a single focus stop with a white ring, per
 * the 10-foot focus rules.
 */
@Composable
fun <T> TvOptionMenuDialog(
    title: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp)
                .heightIn(max = 560.dp)
                .clip(shape)
                .background(NetflixDarkSurface)
                .border(2.dp, NetflixWhite.copy(alpha = 0.85f), shape)
                .padding(20.dp)
        ) {
            Text(
                text = title,
                color = NetflixWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(options) { option ->
                    TvOptionRow(
                        label = optionLabel(option),
                        selected = option == selected,
                        requestFocusOnStart = option == selected,
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvOptionRow(
    label: String,
    selected: Boolean,
    requestFocusOnStart: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val requester = remember { FocusRequester() }

    if (requestFocusOnStart) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(
                when {
                    focused -> NetflixWhite.copy(alpha = 0.16f)
                    selected -> NetflixWhite.copy(alpha = 0.08f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) NetflixWhite else Color.Transparent,
                shape = shape
            )
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (selected) "✓" else " ",
            color = NetflixRed,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = label,
            color = if (selected || focused) NetflixWhite else NetflixLightGray,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}