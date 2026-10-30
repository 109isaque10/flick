package com.stremflix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.stremflix.ui.theme.NetflixCardSurface
import com.stremflix.ui.theme.NetflixDarkSurface
import com.stremflix.ui.theme.NetflixLightGray
import com.stremflix.ui.theme.NetflixRed
import com.stremflix.ui.theme.NetflixWhite

/**
 * TV replacement for Material3's `OutlinedTextField`.
 *
 * Focusing a real text field on a TV pops the IME (which covers ~60% of the
 * screen) just by D-padding past it. This renders as a static tile instead; the
 * keyboard is summoned only when the user deliberately activates the tile, and
 * the value is committed with OK rather than auto-saved.
 */
@Composable
fun TvInputCard(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(if (focused) NetflixCardSurface else NetflixDarkSurface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) NetflixWhite else NetflixLightGray.copy(alpha = 0.3f),
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { editing = true }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = NetflixLightGray,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(
                text = value.ifBlank { placeholder },
                color = if (value.isBlank()) NetflixLightGray.copy(alpha = 0.7f) else NetflixWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "✎",
            color = if (focused) NetflixWhite else NetflixLightGray,
            fontSize = 16.sp
        )
    }

    if (editing) {
        TvTextEditDialog(
            label = label,
            initialValue = value,
            placeholder = placeholder,
            onDismiss = { editing = false },
            onConfirm = {
                onValueChange(it)
                editing = false
            }
        )
    }
}

@Composable
private fun TvTextEditDialog(
    label: String,
    initialValue: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draft by remember(initialValue) { mutableStateOf(initialValue) }
    var fieldFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val fieldShape = RoundedCornerShape(6.dp)
    val fieldRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Safe to raise the IME here: the user asked for it by activating the tile.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { fieldRequester.requestFocus() }
        keyboardController?.show()
    }

    fun submit() {
        onConfirm(draft)
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 640.dp)
                .clip(shape)
                .background(NetflixDarkSurface)
                .border(2.dp, NetflixWhite.copy(alpha = 0.85f), shape)
                .padding(20.dp)
        ) {
            Text(
                text = label,
                color = NetflixWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(fieldShape)
                    .background(NetflixCardSurface)
                    .border(
                        width = if (fieldFocused) 2.dp else 1.dp,
                        color = if (fieldFocused) NetflixWhite else NetflixLightGray.copy(alpha = 0.35f),
                        shape = fieldShape
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it.replace("\n", "") },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    textStyle = TextStyle(color = NetflixWhite, fontSize = 15.sp),
                    cursorBrush = SolidColor(NetflixWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fieldRequester)
                        .onFocusChanged { fieldFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        if (draft.isEmpty()) {
                            Text(text = placeholder, color = NetflixLightGray, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TvDialogButton(text = "Cancel", primary = false, onClick = onDismiss)
                Spacer(modifier = Modifier.width(10.dp))
                TvDialogButton(text = "OK", primary = true, onClick = { submit() })
            }
        }
    }
}

@Composable
private fun TvDialogButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (primary) NetflixRed else NetflixCardSurface)
            .border(
                width = if (focused) 2.dp else if (primary) 0.dp else 1.dp,
                color = when {
                    focused -> NetflixWhite
                    primary -> androidx.compose.ui.graphics.Color.Transparent
                    else -> NetflixLightGray.copy(alpha = 0.35f)
                },
                shape = shape
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NetflixWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}