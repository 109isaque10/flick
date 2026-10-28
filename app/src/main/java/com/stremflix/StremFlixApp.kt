package com.stremflix

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class StremFlixApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Log.d("StremFlixApp", "StremFlix Application initialized")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
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
            .crossfade(true)
            .build()
    }
}
