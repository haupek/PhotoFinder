package com.example.photofinder.data.ocr

import android.net.Uri

/**
 * Abstraction over the OCR provider. The MVP ships a stub; a Cloud Vision /
 * Azure AI Vision implementation (or a later on-device ML Kit engine) can be
 * dropped in without touching the repository, worker or UI.
 */
interface OcrService {
    /**
     * Recognise text contained in the image referenced by [uri].
     * Returns the recognised text, an empty string when no text was found,
     * or null when recognition could not be performed.
     */
    suspend fun recognizeText(uri: Uri): String?
}
