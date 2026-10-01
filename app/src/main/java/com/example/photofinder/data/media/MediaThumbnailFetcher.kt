package com.example.photofinder.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse

/**
 * Loads fast, pre-generated MediaStore thumbnails for image content URIs via
 * ContentResolver.loadThumbnail, instead of decoding the full-resolution
 * original (which is slow and memory-heavy and causes scroll jank). Falls back
 * to a sub-sampled RGB_565 decode if no system thumbnail is available.
 */
class MediaThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val target = maxOf(
            options.size.width.pxOrElse { 512 },
            options.size.height.pxOrElse { 512 },
            1
        )
        val bitmap = try {
            context.contentResolver.loadThumbnail(data, Size(target, target), null)
        } catch (e: Exception) {
            decodeSampled(target)
        }
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    private fun decodeSampled(target: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(data)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest > 0 && longest / sample > target * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return context.contentResolver.openInputStream(data)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw IllegalStateException("Cannot decode image: $data")
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.authority != MediaStore.AUTHORITY) return null
            return MediaThumbnailFetcher(context, data, options)
        }
    }
}
