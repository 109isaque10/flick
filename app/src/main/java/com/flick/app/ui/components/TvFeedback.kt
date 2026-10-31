package com.flick.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flick.app.ui.theme.NetflixRed

/**
 * TV-friendly replacement for Material3's `CircularProgressIndicator`, which
 * `androidx.tv.material3` does not ship.
 *
 * A 4dp hairline ring is effectively invisible from sofa distance on a 1080p
 * panel, so every loading state on the 10-foot surface uses this instead: a
 * thicker, larger arc drawn with a plain [Canvas] (no Material dependency at
 * all, which also keeps it usable while a Material theme is not present).
 */
@Composable
fun TvSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = NetflixRed
) {
    val transition = rememberInfiniteTransition(label = "tvSpinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
        label = "tvSpinnerAngle"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val canvasSize = this.size
        val arcSize = Size(
            width = canvasSize.width - strokePx,
            height = canvasSize.height - strokePx
        )
        rotate(degrees = angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}