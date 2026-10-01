package com.example.photofinder.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Free, on-device OCR provider based on ML Kit Text Recognition v2.
 *
 * Runs entirely on the device: no per-image cost, no network transfer, works
 * offline. The bundled Latin model covers German/English text in screenshots,
 * documents, signs and notes. Additional scripts (Chinese, Devanagari, Japanese,
 * Korean) can be added as separate ML Kit dependencies and chained here if needed.
 */
class MlKitOcrService(private val context: Context) : OcrService {

    private val recognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(uri: Uri): String? {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result -> continuation.resume(result.text) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }
}
