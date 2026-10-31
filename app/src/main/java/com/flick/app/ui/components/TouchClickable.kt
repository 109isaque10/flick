package com.flick.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * androidx.tv.material3 clickable components (Button, OutlinedButton,
 * IconButton, Surface(onClick = ...) ...) are built for remote input only: they
 * are focusable and react to DPAD center / Enter, but they carry no pointer
 * input at all, so a tap on a phone or tablet never fires their onClick.
 *
 * Add this to such a component's modifier to make it tappable. It only listens
 * for taps, so the component's existing remote/keyboard handling is untouched
 * and it costs nothing on a TV without a touchscreen.
 */
fun Modifier.touchClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier =
    if (enabled) {
        pointerInput(onClick) { detectTapGestures { onClick() } }
    } else {
        this
    }
