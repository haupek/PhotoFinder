package com.example.photofinder.ui.search

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.example.photofinder.di.ServiceLocator
import com.example.photofinder.domain.model.LocationMode
import com.example.photofinder.domain.model.Photo
import com.example.photofinder.domain.model.SortField
import com.example.photofinder.domain.model.SearchQuery
import com.example.photofinder.util.LocationResolver
import com.example.photofinder.work.IndexingState
import com.example.photofinder.work.OcrIndexingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val tag: String = "",
    val text: String = "",
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
    val results: List<Photo> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val hasSearched: Boolean = false,
    val isSearching: Boolean = false,
    val searchExpanded: Boolean = true,
    val columns: Int = 3,
    val textWholeWord: Boolean = true,
    val textCaseSensitive: Boolean = false,
    val sortField: SortField = SortField.DATE,
    val sortAscending: Boolean = false,
    val sortCenterLat: Double? = null,
    val sortCenterLon: Double? = null,
    val resolvingSort: Boolean = false,
    val sortMessage: String? = null,

    val indexing: Boolean = false,
    val totalCount: Int = 0,
    val indexedCount: Int = 0,
    val coordinatesCount: Int = 0,
    val geoPending: Int = 0,
    val mediaLocationGranted: Boolean = false,

    val showConsentDialog: Boolean = false,

    val locationMode: LocationMode = LocationMode.NONE,
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    val centerLabel: String = "",
    val radiusKm: Int = 25,
    val placeInput: String = "",
    val resolvingLocation: Boolean = false,
    val locationMessage: String? = null,
    val showLocationDialog: Boolean = false,
    val showFilterSheet: Boolean = false,
    val showMapPicker: Boolean = false,

    val editingPhotoId: Long? = null,
    val editingTags: List<String> = emptyList(),

    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val showBatchTagDialog: Boolean = false,

    val viewerIndex: Int? = null,
    val viewerTags: List<String> = emptyList(),
    val viewerPlace: String? = null,

    val showMap: Boolean = false,
    val mapCenterLat: Double? = null,
    val mapCenterLon: Double? = null
) {
    val pendingCount: Int get() = (totalCount - indexedCount).coerceAtLeast(0)
}

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.photoRepository
    private val consent = ServiceLocator.consentManager
    private val locationResolver = LocationResolver(app)

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var wasIndexing = false

    init {
        viewModelScope.launch {
            IndexingState.progress.collect { p ->
                _state.update {
                    if (p.running) {
                        it.copy(indexing = true, indexedCount = p.indexed, totalCount = p.total)
                    } else {
                        it.copy(indexing = false)
                    }
                }
                if (wasIndexing && !p.running) {
                    refreshCounts()
                }
                wasIndexing = p.running
            }
        }
    }

    fun onPermissionGranted() {
        // Critical path: index new photos, then show the gallery immediately.
        viewModelScope.launch {
            runCatching { repository.syncFromMediaStore() }
            refreshCounts()
            // Gallery-style start: show all photos initially (empty query = all).
            if (!_state.value.hasSearched) {
                search()
            }
            if (consent.noticeShown && consent.ocrEnabled) {
                OcrIndexingService.start(getApplication())
            }
        }
        // Background: fill in source folders without ever blocking the gallery.
        viewModelScope.launch {
            val changed = runCatching { repository.backfillFolders() }.getOrDefault(false)
            if (changed && _state.value.hasSearched) {
                search()
            }
        }
    }

    fun onTagChange(value: String) = _state.update { it.copy(tag = value) }
    fun onTextChange(value: String) = _state.update { it.copy(text = value) }

    /** Clears the text field and refreshes results if a search is already active. */
    fun clearText() {
        _state.update { it.copy(text = "") }
        if (_state.value.hasSearched) search()
    }
    fun toggleWholeWord() = _state.update { it.copy(textWholeWord = !it.textWholeWord) }
    fun toggleCaseSensitive() = _state.update { it.copy(textCaseSensitive = !it.textCaseSensitive) }
    fun onFromChange(value: Long?) = _state.update { it.copy(fromMillis = value) }
    fun onToChange(value: Long?) = _state.update { it.copy(toMillis = value) }

    fun toggleSearchMask() = _state.update { it.copy(searchExpanded = !it.searchExpanded) }

    fun reset() {
        _state.update {
            it.copy(
                tag = "", text = "", fromMillis = null, toMillis = null,
                results = emptyList(), hasSearched = false, searchExpanded = true,
                textWholeWord = true, textCaseSensitive = false,
                locationMode = LocationMode.NONE,
                centerLat = null, centerLon = null, centerLabel = "",
                placeInput = "", locationMessage = null
            )
        }
        clearSelection()
        // Clearing all filters returns to the full gallery view.
        search()
    }

    fun search() {
        val s = _state.value
        val toInclusive = s.toMillis?.let { it + DAY_MILLIS - 1 }
        val query = SearchQuery(
            tag = s.tag.ifBlank { null },
            text = s.text.ifBlank { null },
            fromMillis = s.fromMillis,
            toMillis = toInclusive,
            locationMode = s.locationMode,
            centerLat = s.centerLat,
            centerLon = s.centerLon,
            radiusKm = if (s.locationMode == LocationMode.RADIUS) s.radiusKm else null,
            caseSensitive = s.textCaseSensitive,
            wholeWord = s.textWholeWord
        )
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            try {
                val results = repository.search(query)
                val sorted = applySort(results, _state.value)
                val validIds = sorted.map { it.mediaId }.toSet()
                _state.update {
                    it.copy(
                        results = sorted,
                        hasSearched = true,
                        searchExpanded = false,
                        selectedIds = it.selectedIds intersect validIds
                    )
                }
            } finally {
                _state.update { it.copy(isSearching = false) }
            }
        }
    }

    // --- Location filter ---

    fun openFilterSheet() {
        _state.update { it.copy(showFilterSheet = true, locationMessage = null) }
        // Re-check the media-location permission so its status is current.
        viewModelScope.launch { refreshCounts() }
    }

    fun closeFilterSheet() = _state.update { it.copy(showFilterSheet = false) }

    fun openLocationDialog() {
        _state.update { it.copy(showLocationDialog = true, locationMessage = null) }
        // Re-check the media-location permission so the status and the request
        // button always reflect the current grant when the dialog opens.
        viewModelScope.launch { refreshCounts() }
    }
    fun closeLocationDialog() = _state.update { it.copy(showLocationDialog = false) }

    fun setLocationMode(mode: LocationMode) = _state.update { it.copy(locationMode = mode) }
    fun setRadiusKm(km: Int) = _state.update { it.copy(radiusKm = km, locationMode = LocationMode.RADIUS) }
    fun onPlaceInputChange(value: String) = _state.update { it.copy(placeInput = value) }

    fun openMapPicker() = _state.update { it.copy(showMapPicker = true) }
    fun closeMapPicker() = _state.update { it.copy(showMapPicker = false) }

    fun setMapCenter(lat: Double, lon: Double) {
        _state.update {
            it.copy(
                showMapPicker = false,
                locationMode = LocationMode.RADIUS,
                centerLat = lat,
                centerLon = lon,
                centerLabel = MAP_LABEL,
                locationMessage = null
            )
        }
    }

    fun useCurrentLocation() {
        _state.update { it.copy(resolvingLocation = true, locationMessage = null) }
        viewModelScope.launch {
            val location = locationResolver.freshLocation()
            if (location != null) {
                _state.update {
                    it.copy(
                        resolvingLocation = false,
                        locationMode = LocationMode.RADIUS,
                        centerLat = location.first,
                        centerLon = location.second,
                        centerLabel = CURRENT_LABEL,
                        locationMessage = null
                    )
                }
            } else {
                _state.update {
                    it.copy(resolvingLocation = false, locationMessage = MSG_NO_LOCATION)
                }
            }
        }
    }

    fun resolvePlace() {
        val place = _state.value.placeInput.trim()
        if (place.isEmpty()) return
        _state.update { it.copy(resolvingLocation = true, locationMessage = null) }
        viewModelScope.launch {
            val location = locationResolver.geocode(place)
            if (location != null) {
                _state.update {
                    it.copy(
                        resolvingLocation = false,
                        locationMode = LocationMode.RADIUS,
                        centerLat = location.first,
                        centerLon = location.second,
                        centerLabel = place,
                        locationMessage = null
                    )
                }
            } else {
                _state.update {
                    it.copy(resolvingLocation = false, locationMessage = MSG_GEOCODE_FAILED)
                }
            }
        }
    }

    // --- Sorting ---

    fun selectSortField(field: SortField) {
        if (field == SortField.DISTANCE) {
            _state.update { it.copy(sortField = SortField.DISTANCE, sortMessage = null) }
            val st = _state.value
            val hasFilterCenter = st.centerLat != null && st.centerLon != null
            val hasResolvedCenter = st.sortCenterLat != null && st.sortCenterLon != null
            if (hasFilterCenter || hasResolvedCenter) {
                // Reference = the filtered place if set, otherwise the last resolved current location.
                reapplySort()
            } else {
                // No filtered place and nothing resolved yet: use the current device location.
                resolveSortLocation()
            }
        } else {
            _state.update { it.copy(sortField = field, sortMessage = null) }
            reapplySort()
        }
    }

    fun setSortAscending(ascending: Boolean) {
        _state.update { it.copy(sortAscending = ascending) }
        reapplySort()
    }

    private fun resolveSortLocation() {
        _state.update { it.copy(resolvingSort = true, sortMessage = null) }
        viewModelScope.launch {
            val loc = locationResolver.freshLocation()
            if (loc != null) {
                _state.update {
                    it.copy(
                        resolvingSort = false,
                        sortCenterLat = loc.first,
                        sortCenterLon = loc.second,
                        sortMessage = null
                    )
                }
                reapplySort()
            } else {
                _state.update {
                    it.copy(resolvingSort = false, sortField = SortField.DATE, sortMessage = MSG_NO_LOCATION)
                }
                reapplySort()
            }
        }
    }

    private fun reapplySort() {
        _state.update { it.copy(results = applySort(it.results, it)) }
    }

    private fun applySort(list: List<Photo>, s: SearchUiState): List<Photo> = when (s.sortField) {
        SortField.NAME -> {
            val sorted = list.sortedBy { it.displayName.lowercase() }
            if (s.sortAscending) sorted else sorted.reversed()
        }
        SortField.DISTANCE -> {
            // Reference point: the filtered place if one is set, otherwise the resolved current location.
            val useFilter = s.centerLat != null && s.centerLon != null
            val lat = if (useFilter) s.centerLat else s.sortCenterLat
            val lon = if (useFilter) s.centerLon else s.sortCenterLon
            if (lat == null || lon == null) {
                list.sortedByDescending { it.dateTakenMillis }
            } else {
                val withCoords = list
                    .filter { it.latitude != null && it.longitude != null }
                    .sortedBy { haversineKm(lat, lon, it.latitude!!, it.longitude!!) }
                val without = list.filter { it.latitude == null || it.longitude == null }
                val ordered = if (s.sortAscending) withCoords else withCoords.reversed()
                ordered + without
            }
        }
        else -> {
            val sorted = list.sortedBy { it.dateTakenMillis }
            if (s.sortAscending) sorted else sorted.reversed()
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

        /** Called after the media-location permission has been granted: re-read GPS. */
    fun onMediaLocationGranted() {
        viewModelScope.launch {
            refreshCounts()
            repository.resetGeo()
            OcrIndexingService.start(getApplication())
        }
    }

    /** Re-reads GPS for all photos (e.g. after granting media location). */
    fun requestGeoRescan() {
        _state.update { it.copy(showLocationDialog = false) }
        viewModelScope.launch {
            repository.resetGeo()
            OcrIndexingService.start(getApplication())
        }
    }

    // --- OCR indexing ---

    fun onIndexRequested() {
        if (_state.value.indexing) return
        if (consent.ocrEnabled) {
            OcrIndexingService.start(getApplication())
        } else {
            _state.update { it.copy(showConsentDialog = true) }
        }
    }

    fun onConsentAccepted() {
        consent.noticeShown = true
        consent.ocrEnabled = true
        _state.update { it.copy(showConsentDialog = false) }
        OcrIndexingService.start(getApplication())
    }

    fun onConsentDeclined() {
        consent.noticeShown = true
        consent.ocrEnabled = false
        _state.update { it.copy(showConsentDialog = false) }
    }

    // --- Single-photo tag editor ---

    fun openTagEditor(mediaId: Long) {
        _state.update { it.copy(editingPhotoId = mediaId, editingTags = emptyList()) }
        viewModelScope.launch { reloadEditingTags(mediaId) }
    }

    fun addEditingTag(value: String) {
        val mediaId = _state.value.editingPhotoId ?: return
        viewModelScope.launch {
            repository.addTag(mediaId, value)
            reloadEditingTags(mediaId)
            refreshCounts()
            if (_state.value.hasSearched) search()
        }
    }

    fun removeEditingTag(tag: String) {
        val mediaId = _state.value.editingPhotoId ?: return
        viewModelScope.launch {
            repository.removeTag(mediaId, tag)
            reloadEditingTags(mediaId)
            refreshCounts()
            if (_state.value.hasSearched) search()
        }
    }

    fun closeTagEditor() {
        _state.update { it.copy(editingPhotoId = null, editingTags = emptyList()) }
    }

    private suspend fun reloadEditingTags(mediaId: Long) {
        val tags = repository.tagsFor(mediaId)
        _state.update { it.copy(editingTags = tags) }
    }

    // --- Multi-select / batch tagging ---

    fun startSelection(mediaId: Long) {
        _state.update { it.copy(selectionMode = true, selectedIds = setOf(mediaId)) }
    }

    fun toggleSelection(mediaId: Long) {
        _state.update {
            val next = if (it.selectedIds.contains(mediaId)) {
                it.selectedIds - mediaId
            } else {
                it.selectedIds + mediaId
            }
            it.copy(selectedIds = next, selectionMode = next.isNotEmpty())
        }
    }

    fun selectAll() {
        _state.update {
            it.copy(selectionMode = true, selectedIds = it.results.map { p -> p.mediaId }.toSet())
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    /** Removes all marks but stays in selection mode. Only clearSelection (the X) exits the mode. */
    fun deselectAll() {
        _state.update { it.copy(selectedIds = emptySet()) }
    }

    fun requestBatchTag() {
        if (_state.value.selectedIds.isNotEmpty()) {
            _state.update { it.copy(showBatchTagDialog = true) }
        }
    }

    fun cancelBatchTag() {
        _state.update { it.copy(showBatchTagDialog = false) }
    }

    fun confirmBatchTag(value: String) {
        val ids = _state.value.selectedIds
        viewModelScope.launch {
            repository.addTagToMany(ids, value)
            refreshCounts()
            _state.update { it.copy(showBatchTagDialog = false) }
            clearSelection()
            if (_state.value.hasSearched) search()
        }
    }

    // --- Fullscreen viewer ---

    fun openViewer(index: Int) {
        if (index < 0) return
        _state.update { it.copy(viewerIndex = index, viewerTags = emptyList(), viewerPlace = null, showMap = false) }
    }

    fun closeViewer() {
        _state.update { it.copy(viewerIndex = null, viewerTags = emptyList(), viewerPlace = null) }
    }

    fun openMap(lat: Double? = null, lon: Double? = null) {
        _state.update { it.copy(showMap = true, mapCenterLat = lat, mapCenterLon = lon, viewerIndex = null) }
    }

    fun closeMap() {
        _state.update { it.copy(showMap = false) }
    }

    fun setColumns(count: Int) {
        val clamped = count.coerceIn(2, 5)
        if (clamped != _state.value.columns) {
            _state.update { it.copy(columns = clamped) }
        }
    }

    fun openPhotoFromMap(mediaId: Long) {
        val idx = _state.value.results.indexOfFirst { it.mediaId == mediaId }
        if (idx >= 0) {
            _state.update {
                it.copy(showMap = false, viewerIndex = idx, viewerTags = emptyList(), viewerPlace = null)
            }
        }
    }

    fun loadViewerDetails(photo: Photo) {
        viewModelScope.launch {
            val tags = repository.tagsFor(photo.mediaId)
            val place = if (photo.latitude != null && photo.longitude != null) {
                locationResolver.reverseGeocode(photo.latitude, photo.longitude)
            } else {
                null
            }
            _state.update { it.copy(viewerTags = tags, viewerPlace = place) }
        }
    }

    fun onPhotoDeleted(mediaId: Long) {
        viewModelScope.launch {
            repository.deleteFromIndex(mediaId)
            _state.update { st ->
                st.copy(
                    results = st.results.filterNot { it.mediaId == mediaId },
                    selectedIds = st.selectedIds - mediaId,
                    viewerIndex = null
                )
            }
            refreshCounts()
        }
    }

    fun onPhotosDeleted(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        viewModelScope.launch {
            repository.deleteManyFromIndex(idSet.toList())
            _state.update { st ->
                st.copy(
                    results = st.results.filterNot { it.mediaId in idSet },
                    selectedIds = emptySet(),
                    selectionMode = false
                )
            }
            refreshCounts()
        }
    }

    private suspend fun refreshCounts() {
        val suggestions = repository.tagSuggestions()
        val total = repository.totalCount()
        val indexed = repository.indexedCount()
        val coords = repository.coordinatesCount()
        val geoPending = repository.geoPendingCount()
        val mediaLoc = ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.ACCESS_MEDIA_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        _state.update {
            it.copy(
                tagSuggestions = suggestions,
                totalCount = total,
                indexedCount = indexed,
                coordinatesCount = coords,
                geoPending = geoPending,
                mediaLocationGranted = mediaLoc
            )
        }
    }

    companion object {
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        const val CURRENT_LABEL = "__CURRENT__"
        const val MAP_LABEL = "__MAP__"
        const val MSG_NO_LOCATION = "__NO_LOCATION__"
        const val MSG_GEOCODE_FAILED = "__GEOCODE_FAILED__"
    }
}
