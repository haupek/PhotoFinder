package com.example.photofinder

import android.app.Application
import android.net.Uri
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.example.photofinder.data.media.MediaThumbnailFetcher
import com.example.photofinder.di.ServiceLocator
import org.osmdroid.config.Configuration

class PhotoFinderApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // OpenStreetMap blocks generic/example user agents (com.example.* -> 403).
        // Identify the app with a unique agent so tiles load.
        Configuration.getInstance().userAgentValue = "Fotofinder/1.0 (Android)"
    }

    /**
     * Coil image loader tuned for a fast gallery grid: a fetcher that uses the
     * system MediaStore thumbnails, RGB_565 bitmaps, no crossfade, and a larger
     * memory cache so revisited thumbnails appear instantly while scrolling.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(MediaThumbnailFetcher.Factory(this@PhotoFinderApp), Uri::class.java)
            }
            .allowRgb565(true)
            .crossfade(false)
            .memoryCache {
                MemoryCache.Builder(this@PhotoFinderApp)
                    .maxSizePercent(0.30)
                    .build()
            }
            .build()
}
