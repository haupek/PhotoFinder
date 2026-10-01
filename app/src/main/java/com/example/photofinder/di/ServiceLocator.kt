package com.example.photofinder.di

import android.content.Context
import com.example.photofinder.data.PhotoRepository
import com.example.photofinder.data.db.AppDatabase
import com.example.photofinder.data.media.MediaStoreSource
import com.example.photofinder.data.ocr.MlKitOcrService
import com.example.photofinder.data.ocr.OcrService
import com.example.photofinder.prefs.ConsentManager

/**
 * Minimal manual dependency container. Keeps the MVP free of a DI framework while
 * still wiring the layers in one place.
 *
 * The active OCR provider is the free, on-device ML Kit engine. To offer an
 * optional higher-accuracy cloud mode later (e.g. Google Cloud Vision or Azure
 * AI Vision), implement OcrService accordingly and swap it in here.
 */
object ServiceLocator {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val ocrService: OcrService by lazy { MlKitOcrService(appContext) }

    val photoRepository: PhotoRepository by lazy {
        val db = AppDatabase.get(appContext)
        PhotoRepository(
            dao = db.photoDao(),
            mediaStore = MediaStoreSource(appContext),
            ocrService = ocrService
        )
    }

    val consentManager: ConsentManager by lazy { ConsentManager(appContext) }
}
