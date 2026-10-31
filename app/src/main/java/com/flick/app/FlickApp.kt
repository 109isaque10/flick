package com.flick.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.flick.app.data.model.TmdbImageSizes
import com.flick.app.work.TraktSyncScheduler

class FlickApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()

        // Select TMDB image buckets before any screen can compose, so poster grids
        // never request artwork larger than this device can display or afford.
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        TmdbImageSizes.configureForDevice(
            lowRam = activityManager.isLowRamDevice,
            uiWidthPx = resources.displayMetrics.widthPixels
        )

        // Trakt/continue-watching reconciliation runs as a constrained background
        // job instead of a network call launched while the first frame composes.
        TraktSyncScheduler.schedule(this)

        Log.d("FlickApp", "Flick Application initialized")
    }

    override fun newImageLoader(): ImageLoader {
        // TV memory guidance (developer.android.com/training/tv/playback/memory):
        // low-RAM TV boxes report isLowRamDevice() == true and target roughly
        // 30-40 MB of graphics memory in total. A flat 20% bitmap cache would allow
        // ~200 MB on a 1 GB heap, which keeps the GC busy and makes poster grids
        // janky while scrolling, so the budget shrinks on constrained devices.
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRam = activityManager.isLowRamDevice
        val memoryCachePercent = if (isLowRam) 0.08 else 0.15

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024L * 1024L) // 100 MB disk cache
                    .build()
            }
            .bitmapConfig(Bitmap.Config.RGB_565) // Halves bitmap memory allocation on Android TV
            .allowRgb565(true)
            // Hardware bitmaps (Coil's default on API 26+) keep decoded textures in
            // GPU memory instead of duplicating them on the Java heap, which is what
            // the TV memory guide recommends.
            .allowHardware(true)
            // No crossfade: it starts an animation per decoded image, so a screen
            // with dozens of posters ran dozens of animators at once — a real cost
            // on a low-power TV box where the transition is barely visible anyway.
            .crossfade(false)
            .build()
    }
}