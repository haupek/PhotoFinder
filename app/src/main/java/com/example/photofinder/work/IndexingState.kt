package com.example.photofinder.work

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide, in-memory progress of the OCR indexing. The foreground service
 * writes to it; the UI (ViewModel) observes it. Lives as long as the app process,
 * which is exactly as long as the foreground service can run.
 */
object IndexingState {

    data class Progress(
        val running: Boolean = false,
        val indexed: Int = 0,
        val total: Int = 0
    )

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    fun update(running: Boolean, indexed: Int, total: Int) {
        _progress.value = Progress(running = running, indexed = indexed, total = total)
    }

    fun setRunning(running: Boolean) {
        _progress.update { it.copy(running = running) }
    }
}
