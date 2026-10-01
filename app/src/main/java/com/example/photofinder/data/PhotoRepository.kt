package com.example.photofinder.data

import com.example.photofinder.data.db.PhotoDao
import com.example.photofinder.data.db.PhotoEntity
import com.example.photofinder.data.db.TagEntity
import com.example.photofinder.data.media.GeoReadResult
import com.example.photofinder.data.media.MediaStoreSource
import com.example.photofinder.data.ocr.OcrService
import com.example.photofinder.domain.model.LocationMode
import com.example.photofinder.domain.model.Photo
import com.example.photofinder.domain.model.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class PhotoRepository(
    private val dao: PhotoDao,
    private val mediaStore: MediaStoreSource,
    private val ocrService: OcrService
) {

    suspend fun syncFromMediaStore() = withContext(Dispatchers.IO) {
        val images = mediaStore.queryImages()
        if (images.isNotEmpty()) {
            dao.insertNewPhotos(images)
        }
    }

    /**
     * Backfills the source folder for rows created before that column existed
     * (freshly inserted rows already carry it). Runs as a single transaction so
     * it is fast even for large libraries, and only while some rows lack it.
     * Returns true if anything was updated. Kept off the initial-display path.
     */
    suspend fun backfillFolders(): Boolean = withContext(Dispatchers.IO) {
        if (dao.missingBucketCount() <= 0) return@withContext false
        val images = mediaStore.queryImages()
        if (images.isEmpty()) return@withContext false
        dao.backfillBuckets(images)
        true
    }

    suspend fun totalCount(): Int = withContext(Dispatchers.IO) { dao.totalCount() }
    suspend fun indexedCount(): Int = withContext(Dispatchers.IO) { dao.indexedCount() }
    suspend fun pendingOcrCount(): Int = withContext(Dispatchers.IO) { dao.pendingOcrCount() }
    suspend fun geoPendingCount(): Int = withContext(Dispatchers.IO) { dao.geoPendingCount() }
    suspend fun coordinatesCount(): Int = withContext(Dispatchers.IO) { dao.coordinatesCount() }

    suspend fun runOcrBatch(limit: Int = 20): Int = withContext(Dispatchers.IO) {
        val ids = dao.unindexedIds(limit)
        for (id in ids) {
            val text = runCatching { ocrService.recognizeText(mediaStore.uriFor(id)) }.getOrNull()
            dao.storeOcrResult(id, text)
        }
        ids.size
    }

    suspend fun indexAllPending(
        parallelism: Int,
        onProgress: suspend (indexed: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val total = dao.totalCount()
        val chunkSize = (parallelism * 4).coerceAtLeast(parallelism)
        while (true) {
            val ids = dao.unindexedIds(chunkSize)
            if (ids.isEmpty()) break
            val semaphore = Semaphore(parallelism)
            coroutineScope {
                ids.forEach { id ->
                    launch {
                        semaphore.withPermit {
                            val text = runCatching {
                                ocrService.recognizeText(mediaStore.uriFor(id))
                            }.getOrNull()
                            dao.storeOcrResult(id, text)
                        }
                    }
                }
            }
            onProgress(dao.indexedCount(), total)
        }
    }

    /**
     * Reads GPS for every photo not yet checked. The pending ids are snapshotted
     * once, so photos that turn out Unreadable stay unchecked and are retried on a
     * later run instead of looping forever within this run.
     */
    suspend fun indexAllPendingGeo(parallelism: Int) = withContext(Dispatchers.IO) {
        val ids = dao.geoUncheckedIds(Int.MAX_VALUE)
        if (ids.isEmpty()) return@withContext
        val semaphore = Semaphore(parallelism)
        coroutineScope {
            ids.forEach { id ->
                launch {
                    semaphore.withPermit {
                        when (val result = mediaStore.readGps(id)) {
                            is GeoReadResult.Found -> dao.storeGeo(id, result.lat, result.lon)
                            GeoReadResult.None -> dao.storeGeo(id, null, null)
                            GeoReadResult.Unreadable -> {
                                // Leave geoChecked = 0 so it is retried on a later run.
                            }
                        }
                    }
                }
            }
        }
    }

    /** Marks all photos for GPS re-reading (e.g. after granting media location). */
    suspend fun resetGeo() = withContext(Dispatchers.IO) {
        dao.resetGeoChecked()
    }

    suspend fun search(query: SearchQuery): List<Photo> = withContext(Dispatchers.IO) {
        val tag = query.tag?.trim()?.lowercase().orEmpty()
        val rawText = query.text?.trim().orEmpty()
        val text = rawText.lowercase()
        val from = query.fromMillis ?: 0L
        val to = query.toMillis ?: 0L

        val locMode = when (query.locationMode) {
            LocationMode.NONE -> 0
            LocationMode.WITH -> 1
            LocationMode.WITHOUT -> 2
            LocationMode.RADIUS -> 1
        }

        val hasCenter = query.locationMode == LocationMode.RADIUS &&
            query.centerLat != null && query.centerLon != null && query.radiusKm != null

        var minLat = 0.0
        var maxLat = 0.0
        var minLon = 0.0
        var maxLon = 0.0
        if (hasCenter) {
            val lat = query.centerLat!!
            val lon = query.centerLon!!
            val radiusKm = query.radiusKm!!.toDouble()
            val dLat = radiusKm / 111.0
            val dLon = radiusKm / (111.320 * max(0.000001, cos(Math.toRadians(lat))))
            minLat = lat - dLat
            maxLat = lat + dLat
            minLon = lon - dLon
            maxLon = lon + dLon
        }

        val rows = dao.search(
            hasTag = if (tag.isEmpty()) 0 else 1,
            tag = tag,
            hasText = if (text.isEmpty()) 0 else 1,
            text = text,
            fromMillis = from,
            toMillis = to,
            locMode = locMode,
            useBbox = if (hasCenter) 1 else 0,
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon
        )

        var filtered = rows
        if (rawText.isNotEmpty() && (query.caseSensitive || query.wholeWord)) {
            filtered = filtered.filter {
                textMatches(it.ocrText, rawText, query.caseSensitive, query.wholeWord)
            }
        }
        if (hasCenter) {
            val lat = query.centerLat!!
            val lon = query.centerLon!!
            val radiusKm = query.radiusKm!!.toDouble()
            filtered = filtered.filter {
                val pLat = it.latitude
                val pLon = it.longitude
                pLat != null && pLon != null && haversineKm(lat, lon, pLat, pLon) <= radiusKm
            }
        }

        filtered.map { it.toPhoto() }
    }

    suspend fun tagsFor(mediaId: Long): List<String> = withContext(Dispatchers.IO) {
        dao.tagsForPhoto(mediaId)
    }

    suspend fun deleteFromIndex(mediaId: Long) = withContext(Dispatchers.IO) {
        dao.deletePhoto(mediaId)
        dao.deleteTagsForPhoto(mediaId)
    }

    suspend fun deleteManyFromIndex(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        dao.deletePhotos(ids)
        dao.deleteTagsForPhotos(ids)
    }

    suspend fun tagSuggestions(): List<String> = withContext(Dispatchers.IO) {
        dao.allTagSuggestions()
    }

    suspend fun addTag(mediaId: Long, tag: String) = withContext(Dispatchers.IO) {
        val clean = tag.trim()
        if (clean.isNotEmpty()) {
            dao.addTag(TagEntity(mediaId = mediaId, tag = clean, tagLower = clean.lowercase()))
        }
    }

    suspend fun addTagToMany(mediaIds: Collection<Long>, tag: String) = withContext(Dispatchers.IO) {
        val clean = tag.trim()
        if (clean.isEmpty()) return@withContext
        val lower = clean.lowercase()
        for (id in mediaIds) {
            dao.addTag(TagEntity(mediaId = id, tag = clean, tagLower = lower))
        }
    }

    suspend fun removeTag(mediaId: Long, tag: String) = withContext(Dispatchers.IO) {
        dao.removeTag(mediaId, tag.trim().lowercase())
    }

    /**
     * Refined text match for the "whole word" and "case sensitive" options. The
     * SQL LIKE pre-filter already narrowed the candidates (case-insensitive
     * substring), so this only needs to confirm the stricter criteria.
     */
    private fun textMatches(
        ocrText: String?,
        query: String,
        caseSensitive: Boolean,
        wholeWord: Boolean
    ): Boolean {
        if (ocrText.isNullOrEmpty()) return false
        val haystack = if (caseSensitive) ocrText else ocrText.lowercase()
        val needle = if (caseSensitive) query else query.lowercase()
        if (needle.isEmpty()) return true
        return if (wholeWord) {
            val pattern = "(?<![\\p{L}\\p{N}])" + Regex.escape(needle) + "(?![\\p{L}\\p{N}])"
            Regex(pattern).containsMatchIn(haystack)
        } else {
            haystack.contains(needle)
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun PhotoEntity.toPhoto(): Photo = Photo(
        mediaId = mediaId,
        uri = mediaStore.uriFor(mediaId).toString(),
        displayName = displayName,
        dateTakenMillis = dateTakenMillis,
        ocrText = ocrText,
        latitude = latitude,
        longitude = longitude,
        folder = bucketName
    )
}
