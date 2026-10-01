package com.example.photofinder.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.photofinder.di.ServiceLocator

/**
 * Incremental OCR indexing in the background: syncs new gallery images into the
 * local index and processes a batch of un-indexed photos through the OCR
 * provider. Already-indexed photos are skipped, so reruns are cheap.
 */
class OcrIndexWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = ServiceLocator.photoRepository
        return try {
            repository.syncFromMediaStore()
            repository.runOcrBatch()
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "photofinder_ocr_index"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<OcrIndexWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
