package com.example.photofinder.data.ocr

import android.net.Uri
import android.util.Log

/**
 * Placeholder OCR provider for the first runnable stand.
 *
 * It marks images as processed (returning no text) so the indexing pipeline is
 * fully wired end to end. Replace this class with a real provider to activate
 * in-image text search:
 *
 *   1. Read the image bytes from the content URI.
 *   2. Send them to Cloud Vision / Azure AI Vision via the Retrofit/OkHttp layer.
 *   3. Return the recognised text.
 *
 * Tag search and the date-range filter are fully functional without this step.
 */
class StubCloudOcrService : OcrService {
    override suspend fun recognizeText(uri: Uri): String? {
        Log.d("PhotoFinder", "Stub OCR invoked for $uri - returning no text. Wire a real provider here.")
        return null
    }
}
