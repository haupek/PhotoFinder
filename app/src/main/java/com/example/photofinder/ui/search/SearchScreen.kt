package com.example.photofinder.ui.search

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.photofinder.R
import com.example.photofinder.domain.model.LocationMode
import com.example.photofinder.domain.model.Photo
import com.example.photofinder.domain.model.SortField
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenPhoto: (Photo) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val focusManager = LocalFocusManager.current

    var menuOpen by remember { mutableStateOf(false) }
    var densityMenuOpen by remember { mutableStateOf(false) }
    val selectionContext = LocalContext.current
    var pendingDeleteIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    val selectionDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingDeleteIds.isNotEmpty()) {
            viewModel.onPhotosDeleted(pendingDeleteIds)
        }
        pendingDeleteIds = emptyList()
    }
    var optionsExpanded by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.selection_close))
                        }
                    },
                    title = {
                        Text(
                            text = state.selectedIds.size.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val uris = ArrayList<Uri>(
                                    state.results
                                        .filter { it.mediaId in state.selectedIds }
                                        .map { Uri.parse(it.uri) }
                                )
                                if (uris.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "image/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    selectionContext.startActivity(Intent.createChooser(intent, null))
                                }
                            },
                            enabled = state.selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share))
                        }
                        IconButton(
                            onClick = {
                                val ids = state.selectedIds.toList()
                                val uris = state.results
                                    .filter { it.mediaId in state.selectedIds }
                                    .map { Uri.parse(it.uri) }
                                if (uris.isNotEmpty()) {
                                    pendingDeleteIds = ids
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        val pi = MediaStore.createDeleteRequest(
                                            selectionContext.contentResolver, uris
                                        )
                                        selectionDeleteLauncher.launch(
                                            IntentSenderRequest.Builder(pi.intentSender).build()
                                        )
                                    } else {
                                        runCatching {
                                            uris.forEach { selectionContext.contentResolver.delete(it, null, null) }
                                        }.onSuccess { viewModel.onPhotosDeleted(ids) }
                                        pendingDeleteIds = emptyList()
                                    }
                                }
                            },
                            enabled = state.selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                        // Checkbox with check = select all
                        IconButton(onClick = viewModel::selectAll) {
                            Icon(Icons.Filled.CheckBox, contentDescription = stringResource(R.string.select_all))
                        }
                        // Empty checkbox = deselect all (stays in selection mode)
                        IconButton(
                            onClick = viewModel::deselectAll,
                            enabled = state.selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.CheckBoxOutlineBlank, contentDescription = stringResource(R.string.selection_deselect))
                        }
                        IconButton(
                            onClick = viewModel::requestBatchTag,
                            enabled = state.selectedIds.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.LocalOffer, contentDescription = stringResource(R.string.selection_tag))
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.search_title)) },
                    actions = {
                        Box {
                            IconButton(onClick = { densityMenuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Filled.GridView,
                                    contentDescription = stringResource(R.string.grid_density)
                                )
                            }
                            DropdownMenu(
                                expanded = densityMenuOpen,
                                onDismissRequest = { densityMenuOpen = false }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.grid_density_value, state.columns),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Slider(
                                        value = state.columns.toFloat(),
                                        onValueChange = { viewModel.setColumns(it.roundToInt()) },
                                        valueRange = 2f..5f,
                                        steps = 2
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.openMap() }) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = stringResource(R.string.map_all_title)
                            )
                        }
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.sort_title))
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_index_now)) },
                                onClick = {
                                    menuOpen = false
                                    viewModel.onIndexRequested()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_reset)) },
                                onClick = {
                                    menuOpen = false
                                    focusManager.clearFocus()
                                    viewModel.reset()
                                }
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val anyFilterActive = state.tag.isNotBlank() ||
                state.fromMillis != null || state.toMillis != null ||
                state.locationMode != LocationMode.NONE

            // Prominent search bar for the in-image (OCR) text query
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::onTextChange,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.text.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearText() }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_field))
                            }
                        }
                        IconButton(onClick = { optionsExpanded = !optionsExpanded }) {
                            Icon(
                                imageVector = if (optionsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = stringResource(R.string.options_toggle),
                                tint = if (optionsExpanded || anyFilterActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.search()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // Collapsible (default collapsed): text options + the filter button row.
            // Toggled via the chevron in the search bar.
            AnimatedVisibility(visible = optionsExpanded) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = state.textWholeWord,
                            onClick = viewModel::toggleWholeWord,
                            label = { Text(stringResource(R.string.opt_whole_word)) }
                        )
                        FilterChip(
                            selected = state.textCaseSensitive,
                            onClick = viewModel::toggleCaseSensitive,
                            label = { Text(stringResource(R.string.opt_case_sensitive)) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = viewModel::openFilterSheet,
                            label = { Text(stringResource(R.string.filter_title)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        if (state.tag.isNotBlank()) {
                            ActiveFilterIcon(Icons.Filled.LocalOffer, stringResource(R.string.field_tag))
                        }
                        if (state.fromMillis != null || state.toMillis != null) {
                            ActiveFilterIcon(Icons.Filled.CalendarMonth, stringResource(R.string.section_date))
                        }
                        if (state.locationMode != LocationMode.NONE) {
                            ActiveFilterIcon(Icons.Filled.Place, stringResource(R.string.loc_filter_button))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val pendingCount = (state.totalCount - state.indexedCount).coerceAtLeast(0)
            AnimatedVisibility(
                visible = state.indexing || (state.totalCount > 0 && pendingCount > 0)
            ) {
                IndexBanner(
                    total = state.totalCount,
                    indexed = state.indexedCount,
                    indexing = state.indexing,
                    onIndexNow = viewModel::onIndexRequested
                )
            }

            if (state.hasSearched) {
                Text(
                    text = stringResource(R.string.results_count, state.results.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.isSearching && state.results.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                            .background(MaterialTheme.colorScheme.background),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.loading_photos),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Crossfade(
                    targetState = state.hasSearched && state.results.isEmpty(),
                    label = "results"
                ) { showEmpty ->
                    if (showEmpty) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ImageSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val gridState = rememberLazyGridState()
                        val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
                        val unknownFolder = stringResource(R.string.group_unknown_folder)
                        val noLocation = stringResource(R.string.group_no_location)
                        val bandLabels = listOf(
                            stringResource(R.string.dist_band_lt1),
                            stringResource(R.string.dist_band_1_5),
                            stringResource(R.string.dist_band_5_25),
                            stringResource(R.string.dist_band_25_100),
                            stringResource(R.string.dist_band_gt100)
                        )
                        val sections = remember(
                            state.results, state.sortField, state.sortAscending,
                            state.centerLat, state.centerLon, state.sortCenterLat, state.sortCenterLon
                        ) {
                            buildSections(state, monthFormat, unknownFolder, noLocation, bandLabels)
                        }
                        val ranges = remember(sections) {
                            val list = ArrayList<Triple<Int, Int, String>>()
                            var idx = 0
                            for (sec in sections) {
                                val start = idx
                                idx += 1 + sec.photos.size
                                list.add(Triple(start, idx, sec.title))
                            }
                            list
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(state.columns),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                sections.forEach { section ->
                                    item(
                                        key = "h_" + section.photos.first().mediaId,
                                        span = { GridItemSpan(maxLineSpan) },
                                        contentType = "header"
                                    ) {
                                        SectionHeader(title = section.title)
                                    }
                                    items(
                                        section.photos,
                                        key = { it.mediaId },
                                        contentType = { "photo" }
                                    ) { photo ->
                                        PhotoCell(
                                            photo = photo,
                                            selectionMode = state.selectionMode,
                                            selected = state.selectedIds.contains(photo.mediaId),
                                            columns = state.columns,
                                            onOpen = { viewModel.openViewer(state.results.indexOf(photo)) },
                                            onToggle = { viewModel.toggleSelection(photo.mediaId) },
                                            onLongPress = { viewModel.startSelection(photo.mediaId) },
                                            onEditTags = { viewModel.openTagEditor(photo.mediaId) }
                                        )
                                    }
                                }
                            }
                            val pinnedTitle by remember(ranges) {
                                derivedStateOf {
                                    val i = gridState.firstVisibleItemIndex
                                    ranges.firstOrNull { i >= it.first && i < it.second }?.third
                                }
                            }
                            pinnedTitle?.let { title ->
                                SectionHeader(
                                    title = title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(start = 8.dp)
                                )
                            }

                            FastScrollbar(
                                gridState = gridState,
                                sectionTitleAt = { idx ->
                                    ranges.firstOrNull { idx >= it.first && idx < it.second }?.third
                                },
                                columns = state.columns
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            SortSheetContent(state = state, viewModel = viewModel)
        }
    }

    if (state.showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeFilterSheet,
            sheetState = sheetState
        ) {
            FilterSheetContent(
                state = state,
                viewModel = viewModel,
                dateFormat = dateFormat,
                onPickFrom = { showFromPicker = true },
                onPickTo = { showToPicker = true },
                onSearch = {
                    viewModel.closeFilterSheet()
                    focusManager.clearFocus()
                    viewModel.search()
                }
            )
        }
    }

    if (showFromPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.fromMillis)
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onFromChange(pickerState.selectedDateMillis)
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) {
                    Text(stringResource(R.string.tag_dialog_cancel))
                }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (showToPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.toMillis)
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onToChange(pickerState.selectedDateMillis)
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) {
                    Text(stringResource(R.string.tag_dialog_cancel))
                }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (state.showMapPicker) {
        MapPickerDialog(
            initialLat = state.centerLat,
            initialLon = state.centerLon,
            onConfirm = { lat, lon -> viewModel.setMapCenter(lat, lon) },
            onDismiss = viewModel::closeMapPicker
        )
    }

    if (state.showConsentDialog) {
        ConsentDialog(
            onAccept = viewModel::onConsentAccepted,
            onDecline = viewModel::onConsentDeclined
        )
    }

    if (state.editingPhotoId != null) {
        TagEditorDialog(
            existingTags = state.editingTags,
            onAdd = viewModel::addEditingTag,
            onRemove = viewModel::removeEditingTag,
            onDismiss = viewModel::closeTagEditor
        )
    }

    if (state.showBatchTagDialog) {
        BatchTagDialog(
            count = state.selectedIds.size,
            onSave = viewModel::confirmBatchTag,
            onDismiss = viewModel::cancelBatchTag
        )
    }

        if (state.showMap) {
            MapScreen(
                photos = state.results,
                centerLat = state.mapCenterLat,
                centerLon = state.mapCenterLon,
                onClose = viewModel::closeMap,
                onOpenPhoto = { mediaId -> viewModel.openPhotoFromMap(mediaId) }
            )
        }

        if (state.viewerIndex != null) {
            PhotoViewer(
                photos = state.results,
                initialIndex = state.viewerIndex!!,
                tags = state.viewerTags,
                place = state.viewerPlace,
                dateFormat = dateFormat,
                onClose = viewModel::closeViewer,
                onPageChanged = { p -> viewModel.loadViewerDetails(p) },
                onEditTags = { mediaId -> viewModel.openTagEditor(mediaId) },
                onDeleted = { mediaId -> viewModel.onPhotoDeleted(mediaId) },
                onOpenMap = { p -> viewModel.openMap(p.latitude, p.longitude) }
            )
        }
    }
}

@Composable
private fun centerLabelText(label: String): String = when {
    label == SearchViewModel.CURRENT_LABEL -> stringResource(R.string.loc_use_current)
    label == SearchViewModel.MAP_LABEL -> stringResource(R.string.loc_map_label)
    label.isNotBlank() -> label
    else -> stringResource(R.string.loc_center_none)
}

@Composable
private fun ActiveFilterIcon(icon: ImageVector, label: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun IndexBanner(
    total: Int,
    indexed: Int,
    indexing: Boolean,
    onIndexNow: () -> Unit
) {
    val pending = (total - indexed).coerceAtLeast(0)
    if (indexing) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.index_running, indexed, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(6.dp))
                val fraction = if (total > 0) indexed.toFloat() / total.toFloat() else 0f
                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else if (total > 0 && pending > 0) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.banner_pending, pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onIndexNow) {
                    Text(stringResource(R.string.banner_now))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheetContent(
    state: SearchUiState,
    viewModel: SearchViewModel,
    dateFormat: SimpleDateFormat,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
    onSearch: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            viewModel.useCurrentLocation()
        }
    }
    val mediaLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        if (ok) viewModel.onMediaLocationGranted()
    }

    val radii = listOf(1, 5, 25, 50)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = stringResource(R.string.filter_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))

        // Keyword
        OutlinedTextField(
            value = state.tag,
            onValueChange = viewModel::onTagChange,
            label = { Text(stringResource(R.string.field_tag)) },
            singleLine = true,
            trailingIcon = {
                if (state.tag.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onTagChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_field))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Date range
        Text(stringResource(R.string.section_date), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPickFrom, modifier = Modifier.weight(1f)) {
                val label = state.fromMillis?.let { dateFormat.format(Date(it)) }
                    ?: stringResource(R.string.date_any)
                Text(stringResource(R.string.date_from) + ": " + label)
            }
            Spacer(Modifier.padding(horizontal = 4.dp))
            OutlinedButton(onClick = onPickTo, modifier = Modifier.weight(1f)) {
                val label = state.toMillis?.let { dateFormat.format(Date(it)) }
                    ?: stringResource(R.string.date_any)
                Text(stringResource(R.string.date_to) + ": " + label)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Location
        Text(stringResource(R.string.loc_filter_button), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (state.mediaLocationGranted) R.string.diag_perm_granted else R.string.diag_perm_missing
            ),
            style = MaterialTheme.typography.bodySmall
        )
        if (!state.mediaLocationGranted) {
            TextButton(
                onClick = { mediaLocationLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) }
            ) { Text(stringResource(R.string.loc_request_perm)) }
        }
        Spacer(Modifier.height(4.dp))

        LocationModeRow(LocationMode.NONE, state.locationMode, R.string.loc_mode_any, viewModel::setLocationMode)
        LocationModeRow(LocationMode.WITH, state.locationMode, R.string.loc_mode_with, viewModel::setLocationMode)
        LocationModeRow(LocationMode.WITHOUT, state.locationMode, R.string.loc_mode_without, viewModel::setLocationMode)
        LocationModeRow(LocationMode.RADIUS, state.locationMode, R.string.loc_mode_radius, viewModel::setLocationMode)

        if (state.locationMode == LocationMode.RADIUS) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val fineGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val coarseGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (fineGranted || coarseGranted) {
                        viewModel.useCurrentLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.loc_use_current)) }

            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = viewModel::openMapPicker,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.loc_pick_on_map)) }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.placeInput,
                    onValueChange = viewModel::onPlaceInputChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.loc_place_hint)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                TextButton(onClick = viewModel::resolvePlace) {
                    Text(stringResource(R.string.loc_place_set))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.loc_radius_label), style = MaterialTheme.typography.labelMedium)
            Row {
                radii.forEach { km ->
                    FilterChip(
                        selected = state.radiusKm == km,
                        onClick = { viewModel.setRadiusKm(km) },
                        label = { Text("$km km") },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(text = centerLabelText(state.centerLabel), style = MaterialTheme.typography.bodySmall)

            val message = when (state.locationMessage) {
                SearchViewModel.MSG_NO_LOCATION -> stringResource(R.string.loc_no_location)
                SearchViewModel.MSG_GEOCODE_FAILED -> stringResource(R.string.loc_geocode_failed)
                else -> null
            }
            if (message != null) {
                Spacer(Modifier.height(4.dp))
                Text(text = message, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = viewModel::requestGeoRescan,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.loc_rescan)) }
        Text(
            text = stringResource(R.string.loc_rescan_hint),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))
        Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_search))
        }
    }
}

@Composable
private fun MapPickerDialog(
    initialLat: Double?,
    initialLon: Double?,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    remember {
        val cfg = Configuration.getInstance()
        cfg.userAgentValue = "Fotofinder/1.0 (Android)"
        val base = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
        Unit
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(fotofinderTileSource())
            setMultiTouchControls(true)
            controller.setZoom(if (initialLat != null && initialLon != null) 13.0 else 4.5)
            controller.setCenter(GeoPoint(initialLat ?: 51.0, initialLon ?: 10.0))
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.map_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.tag_dialog_cancel))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val center = mapView.mapCenter
                        onConfirm(center.latitude, center.longitude)
                    }) { Text(stringResource(R.string.map_confirm)) }
                }
            }
        }
    }
}

@Composable
private fun LocationModeRow(
    mode: LocationMode,
    current: LocationMode,
    labelRes: Int,
    onSelect: (LocationMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = current == mode, onClick = { onSelect(mode) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == mode, onClick = { onSelect(mode) })
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(stringResource(labelRes))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCell(
    photo: Photo,
    selectionMode: Boolean,
    selected: Boolean,
    columns: Int,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onEditTags: () -> Unit
) {
    val tagIconSize = when {
        columns <= 2 -> 20.dp
        columns == 3 -> 16.dp
        columns == 4 -> 13.dp
        else -> 11.dp
    }
    val tagInnerPadding = when {
        columns <= 2 -> 6.dp
        columns == 3 -> 5.dp
        columns == 4 -> 4.dp
        else -> 3.dp
    }
    val tagOuterPadding = if (columns >= 4) 4.dp else 6.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (selectionMode && selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = { if (selectionMode) onToggle() else onOpen() },
                onLongClick = onLongPress
            )
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (selectionMode && selected) {
                        Modifier
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                    } else {
                        Modifier
                    }
                )
        )
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color.Black.copy(alpha = 0.30f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(if (selected) 24.dp else 20.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(tagOuterPadding)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onEditTags)
                    .padding(tagInnerPadding)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalOffer,
                    contentDescription = stringResource(R.string.tag_add),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(tagIconSize)
                )
            }
        }
    }
}

@Composable
private fun ConsentDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(stringResource(R.string.consent_title)) },
        text = { Text(stringResource(R.string.consent_message)) },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(R.string.consent_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(stringResource(R.string.consent_decline)) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditorDialog(
    existingTags: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.tag_existing),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                if (existingTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.tag_none_yet),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingTags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = { onRemove(tag) },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.tag_remove_cd),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.field_tag)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    TextButton(
                        onClick = {
                            if (value.text.isNotBlank()) {
                                onAdd(value.text)
                                value = TextFieldValue("")
                            }
                        }
                    ) { Text(stringResource(R.string.tag_add_action)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tag_dialog_close)) }
        }
    )
}

@Composable
private fun BatchTagDialog(
    count: Int,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue("")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.batch_tag_title, count)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text(stringResource(R.string.field_tag)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.text) }) {
                Text(stringResource(R.string.tag_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tag_dialog_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheetContent(
    state: SearchUiState,
    viewModel: SearchViewModel
) {
    val context = LocalContext.current
    val sortPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            viewModel.selectSortField(SortField.DISTANCE)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(stringResource(R.string.sort_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        SortFieldRow(SortField.DATE, state.sortField, R.string.sort_date) {
            viewModel.selectSortField(SortField.DATE)
        }
        SortFieldRow(SortField.NAME, state.sortField, R.string.sort_name) {
            viewModel.selectSortField(SortField.NAME)
        }
        SortFieldRow(SortField.DISTANCE, state.sortField, R.string.sort_distance) {
            val fine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (fine || coarse) {
                viewModel.selectSortField(SortField.DISTANCE)
            } else {
                sortPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        if (state.sortField == SortField.DISTANCE) {
            val refText = when {
                state.centerLat != null && state.centerLon != null ->
                    stringResource(R.string.sort_ref_filter)
                state.sortCenterLat != null && state.sortCenterLon != null ->
                    stringResource(R.string.sort_ref_current)
                else -> null
            }
            if (refText != null) {
                Text(
                    text = refText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
        }

        if (state.resolvingSort) {
            Text(
                text = stringResource(R.string.sort_resolving),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (state.sortMessage == SearchViewModel.MSG_NO_LOCATION) {
            Text(
                text = stringResource(R.string.loc_no_location),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.sort_order), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = state.sortAscending,
                onClick = { viewModel.setSortAscending(true) },
                label = { Text(stringResource(R.string.sort_asc)) },
                modifier = Modifier.padding(end = 6.dp)
            )
            FilterChip(
                selected = !state.sortAscending,
                onClick = { viewModel.setSortAscending(false) },
                label = { Text(stringResource(R.string.sort_desc)) }
            )
        }
    }
}

@Composable
private fun SortFieldRow(
    field: SortField,
    current: SortField,
    labelRes: Int,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = current == field, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == field, onClick = onSelect)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(stringResource(labelRes))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PhotoViewer(
    photos: List<Photo>,
    initialIndex: Int,
    tags: List<String>,
    place: String?,
    dateFormat: SimpleDateFormat,
    onClose: () -> Unit,
    onPageChanged: (Photo) -> Unit,
    onEditTags: (Long) -> Unit,
    onDeleted: (Long) -> Unit,
    onOpenMap: (Photo) -> Unit
) {
    if (photos.isEmpty()) {
        onClose()
        return
    }
    val context = LocalContext.current
    val dateTimeFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.lastIndex)
    ) { photos.size }
    var chromeVisible by remember { mutableStateOf(true) }
    var showInfo by remember { mutableStateOf(false) }
    val scale = remember { mutableStateOf(1f) }
    val offset = remember { mutableStateOf(Offset.Zero) }
    var pendingDeleteId by remember { mutableStateOf(-1L) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingDeleteId >= 0L) {
            onDeleted(pendingDeleteId)
        }
    }

    val current = photos[pagerState.currentPage.coerceIn(0, photos.lastIndex)]

    BackHandler { onClose() }

    LaunchedEffect(pagerState.currentPage) {
        scale.value = 1f
        offset.value = Offset.Zero
        val idx = pagerState.currentPage.coerceIn(0, photos.lastIndex)
        onPageChanged(photos[idx])
    }
    LaunchedEffect(showInfo) {
        if (showInfo) onPageChanged(current)
    }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = scale.value == 1f,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val isCurrent = page == pagerState.currentPage
                val s = if (isCurrent) scale.value else 1f
                val ox = if (isCurrent) offset.value.x else 0f
                val oy = if (isCurrent) offset.value.y else 0f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { chromeVisible = !chromeVisible },
                                onDoubleTap = {
                                    if (scale.value > 1f) {
                                        scale.value = 1f
                                        offset.value = Offset.Zero
                                    } else {
                                        scale.value = 2.5f
                                    }
                                }
                            )
                        }
                        .then(
                            if (isCurrent) {
                                Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        do {
                                            val event = awaitPointerEvent()
                                            val pressedCount = event.changes.count { it.pressed }
                                            if (pressedCount >= 2) {
                                                val zoom = event.calculateZoom()
                                                val pan = event.calculatePan()
                                                val newScale = (scale.value * zoom).coerceIn(1f, 5f)
                                                scale.value = newScale
                                                offset.value = if (newScale > 1f) offset.value + pan else Offset.Zero
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            } else if (scale.value > 1f) {
                                                val pan = event.calculatePan()
                                                offset.value = offset.value + pan
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photos[page].uri,
                        contentDescription = photos[page].displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = s
                                scaleY = s
                                translationX = ox
                                translationY = oy
                            }
                    )
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(current.dateTakenMillis)),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(current.uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_share), tint = Color.White)
                    }
                    IconButton(onClick = { onEditTags(current.mediaId) }) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = stringResource(R.string.tag_add), tint = Color.White)
                    }
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.action_info), tint = Color.White)
                    }
                    IconButton(onClick = {
                        pendingDeleteId = current.mediaId
                        val uri = Uri.parse(current.uri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                            deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                        } else {
                            runCatching { context.contentResolver.delete(uri, null, null) }
                                .onSuccess { onDeleted(current.mediaId) }
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = Color.White)
                    }
                }
            }
        }
    }

    if (showInfo) {
        ModalBottomSheet(
            onDismissRequest = { showInfo = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            ViewerInfoContent(
                photo = current,
                tags = tags,
                place = place,
                dateTimeFormat = dateTimeFormat,
                onEditTags = { onEditTags(current.mediaId) },
                onOpenMap = { onOpenMap(current) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ViewerInfoContent(
    photo: Photo,
    tags: List<String>,
    place: String?,
    dateTimeFormat: SimpleDateFormat,
    onEditTags: () -> Unit,
    onOpenMap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(stringResource(R.string.viewer_info_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.padding(horizontal = 5.dp))
            Text(dateTimeFormat.format(Date(photo.dateTakenMillis)), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))

        Text(stringResource(R.string.viewer_location), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        if (photo.latitude != null && photo.longitude != null) {
            if (!place.isNullOrBlank()) {
                Text(place, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
            }
            ViewerMap(lat = photo.latitude, lon = photo.longitude)
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onOpenMap) {
                Text(stringResource(R.string.map_open))
            }
        } else {
            Text(
                text = stringResource(R.string.viewer_no_location),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(14.dp))

        Text(stringResource(R.string.viewer_ocr), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        val ocr = photo.ocrText?.trim().orEmpty()
        Text(
            text = ocr.ifEmpty { stringResource(R.string.viewer_no_ocr) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        Text(stringResource(R.string.viewer_tags), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { t ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(t, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable(onClick = onEditTags)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(stringResource(R.string.tag_add), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ViewerMap(lat: Double, lon: Double) {
    val context = LocalContext.current
    remember {
        val cfg = Configuration.getInstance()
        cfg.userAgentValue = "Fotofinder/1.0 (Android)"
        val base = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
        Unit
    }
    val mapView = remember(lat, lon) {
        MapView(context).apply {
            setTileSource(fotofinderTileSource())
            // Static preview: no zoom buttons, no pan/zoom gestures, so the
            // centered marker always stays exactly on the capture location and
            // the map never intercepts taps meant for the sheet.
            setMultiTouchControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isClickable = false
            setOnTouchListener { _, _ -> true }
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(lat, lon))
        }
    }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Icon(
            imageVector = Icons.Filled.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
        )
    }
}

@Composable
private fun MapScreen(
    photos: List<Photo>,
    centerLat: Double?,
    centerLon: Double?,
    onClose: () -> Unit,
    onOpenPhoto: (Long) -> Unit
) {
    val context = LocalContext.current
    BackHandler { onClose() }
    remember {
        val cfg = Configuration.getInstance()
        cfg.userAgentValue = "Fotofinder/1.0 (Android)"
        val base = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
        Unit
    }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(fotofinderTileSource())
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        }
    }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }
    LaunchedEffect(photos, centerLat, centerLon) {
        mapView.overlays.clear()
        val points = mutableListOf<GeoPoint>()
        photos.forEach { p ->
            val lat = p.latitude
            val lon = p.longitude
            if (lat != null && lon != null) {
                val point = GeoPoint(lat, lon)
                points.add(point)
                val marker = Marker(mapView).apply {
                    position = point
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = p.displayName
                    setOnMarkerClickListener { _, _ ->
                        onOpenPhoto(p.mediaId)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
        }
        when {
            centerLat != null && centerLon != null -> {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(GeoPoint(centerLat, centerLon))
            }
            points.size == 1 -> {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(points.first())
            }
            points.size > 1 -> {
                val bbox = BoundingBox.fromGeoPoints(points)
                mapView.post { runCatching { mapView.zoomToBoundingBox(bbox, false, 80) } }
            }
            else -> {
                mapView.controller.setZoom(4.0)
                mapView.controller.setCenter(GeoPoint(51.0, 10.0))
            }
        }
        mapView.invalidate()
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(R.string.map_all_title),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private data class PhotoSection(val title: String, val photos: List<Photo>)

private fun buildSections(
    state: SearchUiState,
    monthFormat: SimpleDateFormat,
    unknownFolder: String,
    noLocation: String,
    bandLabels: List<String>
): List<PhotoSection> {
    val results = state.results
    if (results.isEmpty()) return emptyList()
    return when (state.sortField) {
        SortField.NAME -> groupConsecutive(results) { photo ->
            photo.folder?.takeIf { it.isNotBlank() } ?: unknownFolder
        }
        SortField.DISTANCE -> {
            val useFilter = state.centerLat != null && state.centerLon != null
            val lat = if (useFilter) state.centerLat else state.sortCenterLat
            val lon = if (useFilter) state.centerLon else state.sortCenterLon
            if (lat == null || lon == null) {
                groupConsecutive(results) { photo -> monthLabel(photo, monthFormat) }
            } else {
                groupConsecutive(results) { photo ->
                    val plat = photo.latitude
                    val plon = photo.longitude
                    if (plat == null || plon == null) {
                        noLocation
                    } else {
                        distanceBandLabel(haversineKmUi(lat, lon, plat, plon), bandLabels)
                    }
                }
            }
        }
        else -> groupConsecutive(results) { photo -> monthLabel(photo, monthFormat) }
    }
}

private fun monthLabel(photo: Photo, monthFormat: SimpleDateFormat): String =
    monthFormat.format(Date(photo.dateTakenMillis)).replaceFirstChar { it.uppercase() }

private fun groupConsecutive(items: List<Photo>, key: (Photo) -> String): List<PhotoSection> {
    val out = ArrayList<PhotoSection>()
    var currentKey: String? = null
    var current = ArrayList<Photo>()
    for (photo in items) {
        val k = key(photo)
        if (k != currentKey) {
            val existingKey = currentKey
            if (current.isNotEmpty() && existingKey != null) {
                out.add(PhotoSection(existingKey, current))
            }
            current = ArrayList()
            currentKey = k
        }
        current.add(photo)
    }
    val lastKey = currentKey
    if (current.isNotEmpty() && lastKey != null) {
        out.add(PhotoSection(lastKey, current))
    }
    return out
}

private fun distanceBandLabel(km: Double, labels: List<String>): String = when {
    km < 1.0 -> labels[0]
    km < 5.0 -> labels[1]
    km < 25.0 -> labels[2]
    km < 100.0 -> labels[3]
    else -> labels[4]
}

private fun haversineKmUi(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)
    return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

@Composable
private fun BoxScope.FastScrollbar(
    gridState: LazyGridState,
    sectionTitleAt: (Int) -> String?,
    columns: Int
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeight = 56.dp
    val thumbHeightPx = with(density) { thumbHeight.toPx() }
    var trackHeightPx by remember { mutableStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }

    val totalItems by remember { derivedStateOf { gridState.layoutInfo.totalItemsCount } }
    val visibleItems by remember { derivedStateOf { gridState.layoutInfo.visibleItemsInfo.size } }
    val progress by remember {
        derivedStateOf {
            val denom = (gridState.layoutInfo.totalItemsCount - 1).coerceAtLeast(1)
            (gridState.firstVisibleItemIndex.toFloat() / denom).coerceIn(0f, 1f)
        }
    }

    val show = totalItems > 0 && totalItems > visibleItems
    val fraction = if (dragging) dragFraction else progress
    val maxOffsetPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)

    if (show) {
        // While dragging, show a lightweight placeholder grid instead of moving
        // the real (image-loading) grid. The real jump happens once, on release.
        if (dragging) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                userScrollEnabled = false,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(count = columns * 12) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(28.dp)
                .onSizeChanged { trackHeightPx = it.height.toFloat() }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, (fraction * maxOffsetPx).roundToInt()) }
                    .width(28.dp)
                    .height(thumbHeight)
                    .pointerInput(maxOffsetPx, totalItems) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                dragFraction = progress
                            },
                            onDragEnd = {
                                val target = (dragFraction * (totalItems - 1))
                                    .roundToInt()
                                    .coerceIn(0, totalItems - 1)
                                scope.launch {
                                    gridState.scrollToItem(target)
                                    dragging = false
                                }
                            },
                            onDragCancel = { dragging = false }
                        ) { change, dragAmount ->
                            change.consume()
                            if (maxOffsetPx > 0f) {
                                dragFraction = (dragFraction + dragAmount / maxOffsetPx)
                                    .coerceIn(0f, 1f)
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .width(12.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = if (dragging) 0.95f else 0.6f
                            )
                        )
                )
            }
        }

        if (dragging) {
            val target = (dragFraction * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
            val title = sectionTitleAt(target)
            if (title != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tile source for the maps. Uses CARTO's OpenStreetMap-based raster basemap
 * (key-free, reasonable free usage) instead of OpenStreetMap's own tile servers,
 * which block non-browser apps (HTTP 403 "Access blocked"). The distinct source
 * name also means the previously cached "blocked" tiles are no longer reused.
 */
private fun fotofinderTileSource(): XYTileSource =
    XYTileSource(
        "CartoVoyager",
        0,
        20,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
        )
    )
