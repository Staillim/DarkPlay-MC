package com.darkplaymc.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DarkPlayApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            "new_songs",
            "Nuevas canciones",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifica cuando se agregan canciones nuevas al dispositivo"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .crossfade(300) // 300ms smooth fade
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
