package com.stremflix.ui.player

import android.app.ActivityManager
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl

/**
 * RAM-class aware ExoPlayer buffer budget.
 *
 * Media3's default keeps roughly 50s of media queued regardless of byte size,
 * which on a 1 GB TV box is several times the Anon+Swap budget the Android TV
 * memory guide allows for a 1080p stream. Bounding both the byte total and the
 * time window lets playback degrade gracefully instead of driving direct
 * reclaim (which stalls the allocating thread and shows up as jank).
 *
 * Targets follow the guide's 1-minute-of-video figures: 48 MB on low-RAM
 * devices, then 64 MB / 96 MB as the per-app heap class grows.
 */
@OptIn(UnstableApi::class)
fun buildPlayerLoadControl(context: Context): DefaultLoadControl {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val lowRam = activityManager.isLowRamDevice
    val heapClassMb = activityManager.memoryClass

    val targetBytesMb: Int
    val minBufferMs: Int
    val maxBufferMs: Int
    when {
        lowRam -> {
            targetBytesMb = 48
            minBufferMs = 15_000
            maxBufferMs = 30_000
        }
        heapClassMb <= 192 -> {
            targetBytesMb = 64
            minBufferMs = 15_000
            maxBufferMs = 40_000
        }
        else -> {
            targetBytesMb = 96
            minBufferMs = 20_000
            maxBufferMs = 50_000
        }
    }

    return DefaultLoadControl.Builder()
        .setBufferDurationsMs(minBufferMs, maxBufferMs, 1_500, 3_000)
        .setTargetBufferBytes(targetBytesMb * 1024 * 1024)
        // On constrained devices prefer honoring the byte cap over buffering the
        // full time window; elsewhere the time window can win.
        .setPrioritizeTimeOverSizeThresholds(!lowRam)
        .build()
}
