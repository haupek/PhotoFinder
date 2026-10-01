package com.example.photofinder.work

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.photofinder.R
import com.example.photofinder.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service that runs GPS and OCR indexing to completion. Keeps running
 * during standby and stops on completion or when the app is removed from recents.
 */
class OcrIndexingService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val started = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(indexed = 0, total = 0, indeterminate = true)
        if (started.compareAndSet(false, true)) {
            scope.launch { runIndexing() }
        }
        return START_NOT_STICKY
    }

    private suspend fun runIndexing() {
        val repo = ServiceLocator.photoRepository
        try {
            repo.syncFromMediaStore()

            var total = repo.totalCount()
            var indexed = repo.indexedCount()
            // Mark busy up front so the GPS pass also shows activity.
            IndexingState.update(running = true, indexed = indexed, total = total)
            startInForeground(indexed, total, indeterminate = total == 0)

            val parallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

            if (hasMediaLocationPermission()) {
                repo.indexAllPendingGeo(parallelism)
            }

            indexed = repo.indexedCount()
            total = repo.totalCount()
            IndexingState.update(running = true, indexed = indexed, total = total)

            repo.indexAllPending(parallelism) { idx, tot ->
                IndexingState.update(running = true, indexed = idx, total = tot)
                updateNotification(idx, tot)
            }

            indexed = repo.indexedCount()
            total = repo.totalCount()
            IndexingState.update(running = false, indexed = indexed, total = total)
        } catch (t: Throwable) {
            IndexingState.setRunning(false)
        } finally {
            stopForegroundAndSelf()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopForegroundAndSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        IndexingState.setRunning(false)
        job.cancel()
        super.onDestroy()
    }

    private fun hasMediaLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_MEDIA_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun startInForeground(indexed: Int, total: Int, indeterminate: Boolean) {
        val notification = buildNotification(indexed, total, indeterminate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun updateNotification(indexed: Int, total: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification(indexed, total, indeterminate = false))
    }

    private fun buildNotification(indexed: Int, total: Int, indeterminate: Boolean): Notification {
        val text = if (total > 0) {
            getString(R.string.index_status, indexed, total)
        } else {
            getString(R.string.notif_indexing_title)
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_indexing_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        when {
            indeterminate -> builder.setProgress(0, 0, true)
            total > 0 -> builder.setProgress(total, indexed, false)
        }
        return builder.build()
    }

    private fun stopForegroundAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIF_ID = 4711
        private const val CHANNEL_ID = "ocr_indexing"

        fun start(context: Context) {
            val intent = Intent(context, OcrIndexingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
