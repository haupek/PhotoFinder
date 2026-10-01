package com.example.photofinder.prefs

import android.content.Context

/**
 * Stores the one-time decision about cloud-based OCR. The notice is shown exactly
 * once before the first OCR run, as required by the concept.
 */
class ConsentManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("photofinder_prefs", Context.MODE_PRIVATE)

    /** True once the user has seen and answered the cloud OCR notice. */
    var noticeShown: Boolean
        get() = prefs.getBoolean(KEY_NOTICE_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTICE_SHOWN, value).apply()

    /** True when the user has agreed to cloud OCR processing. */
    var ocrEnabled: Boolean
        get() = prefs.getBoolean(KEY_OCR_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OCR_ENABLED, value).apply()

    companion object {
        private const val KEY_NOTICE_SHOWN = "ocr_notice_shown"
        private const val KEY_OCR_ENABLED = "ocr_enabled"
    }
}
