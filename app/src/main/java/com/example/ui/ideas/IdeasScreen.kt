package com.example.ui.ideas

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.InspirationGalleryItemEntity
import com.example.data.entity.InspirationSourceEntity
import com.example.data.photo.PhotoManager
import com.example.ui.CraftStashViewModel
import com.example.ui.common.CraftPhotoThumbnail
import com.example.ui.common.CraftTagChip
import com.example.ui.common.EmptyStateView
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IdeasScreen(
    viewModel: CraftStashViewModel,
    onNavigateToAddProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val galleryItems by viewModel.inspirationGalleryItems.collectAsState()
    val sources by viewModel.inspirationSources.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Inspiration Gallery, 1 = Inspiration Directory
    var galleryViewMode by remember { mutableStateOf(GalleryViewMode.THUMBNAIL_GRID) }
    var selectedGalleryTagFilter by remember { mutableStateOf<String?>(null) }

    // Directory tag filter & expand/collapse
    var isTagFilterExpanded by remember { mutableStateOf(false) }
    var selectedDirectoryTagFilters by remember { mutableStateOf(setOf<String>()) }

    var showAddGalleryDialog by remember { mutableStateOf(false) }
    var newGalleryPhotoUri by remember { mutableStateOf<String?>(null) }
    var newGallerySourceUrl by remember { mutableStateOf("") }
    var newGalleryNotes by remember { mutableStateOf("") }
    var newGallerySelectedTags by remember { mutableStateOf(setOf<String>()) }
    var newGalleryNewTagInput by remember { mutableStateOf("") }

    // Inspiration Source Editor & View Dialogs
    var showSourceEditorDialog by remember { mutableStateOf(false) }
    var editingSource by remember { mutableStateOf<InspirationSourceEntity?>(null) }
    var viewingSource by remember { mutableStateOf<InspirationSourceEntity?>(null) }

    var sourceNameInput by remember { mutableStateOf("") }
    var sourceUrlInput by remember { mutableStateOf("") }
    var sourceNotesInput by remember { mutableStateOf("") }
    var sourceSelectedTags by remember { mutableStateOf(setOf<String>()) }
    var sourceNewTagInput by remember { mutableStateOf("") }

    val galleryImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val saved = PhotoManager.savePhotoFromUri(context, uri)
                if (saved != null) {
                    newGalleryPhotoUri = saved
                    newGallerySourceUrl = ""
                    newGalleryNotes = ""
                    newGallerySelectedTags = emptySet()
                    newGalleryNewTagInput = ""
                    showAddGalleryDialog = true
                }
            }
        }
    }

    // All unique gallery tags
    val allGalleryTags = remember(galleryItems) {
        galleryItems.flatMap { item ->
            item.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }.distinct().sorted()
    }

    // Filtered gallery items
    val filteredGalleryItems = remember(galleryItems, selectedGalleryTagFilter) {
        if (selectedGalleryTagFilter == null) galleryItems
        else galleryItems.filter { item ->
            item.tags.split(",").map { it.trim() }.any { it.equals(selectedGalleryTagFilter, ignoreCase = true) }
        }
    }

    // All unique directory tags
    val allDirectoryTags = remember(sources) {
        sources.flatMap { src ->
            src.craftTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }.distinct().sorted()
    }

    // Filtered sources (always in alphabetical order)
    val filteredSources = remember(sources, selectedDirectoryTagFilters) {
        val sorted = sources.sortedBy { it.name.lowercase() }
        if (selectedDirectoryTagFilters.isEmpty()) sorted
        else sorted.filter { src ->
            val srcTags = src.craftTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            srcTags.any { t -> selectedDirectoryTagFilters.any { it.equals(t, ignoreCase = true) } }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Inspiration & Ideas", fontWeight = FontWeight.Bold) },
                actions = {
                    if (activeSubTab == 0) {
                        IconButton(
                            onClick = {
                                galleryViewMode = if (galleryViewMode == GalleryViewMode.THUMBNAIL_GRID)
                                    GalleryViewMode.MASONRY_GRID else GalleryViewMode.THUMBNAIL_GRID
                            },
                            modifier = Modifier.testTag("gallery_view_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (galleryViewMode == GalleryViewMode.THUMBNAIL_GRID) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = "Toggle gallery view"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeSubTab == 0) {
                FloatingActionButton(
                    onClick = {
                        galleryImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_gallery_item_fab")
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Upload inspiration image")
                }
            } else if (activeSubTab == 1) {
                FloatingActionButton(
                    onClick = {
                        editingSource = null
                        sourceNameInput = ""
                        sourceUrlInput = ""
                        sourceNotesInput = ""
                        sourceSelectedTags = emptySet()
                        sourceNewTagInput = ""
                        showSourceEditorDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_inspiration_source_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add inspiration destination")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sub-navigation tabs
            TabRow(selectedTabIndex = activeSubTab) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = { Text("Inspiration Gallery") }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = { Text("Inspiration Directory") }
                )
            }

            when (activeSubTab) {
                0 -> {
                    // TAB 0: Inspiration Gallery
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (allGalleryTags.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedGalleryTagFilter == null,
                                        onClick = { selectedGalleryTagFilter = null },
                                        label = { Text("All Tags") }
                                    )
                                }
                                items(allGalleryTags) { tag ->
                                    FilterChip(
                                        selected = selectedGalleryTagFilter.equals(tag, ignoreCase = true),
                                        onClick = {
                                            selectedGalleryTagFilter = if (selectedGalleryTagFilter.equals(tag, ignoreCase = true)) null else tag
                                        },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }

                        if (filteredGalleryItems.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.PhotoLibrary,
                                title = "Inspiration Gallery is Empty",
                                description = "Upload photos of mood boards, color palettes, patterns, or items that inspire your crafting.",
                                actionLabel = "Upload Image",
                                onActionClick = {
                                    galleryImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                when (galleryViewMode) {
                                    GalleryViewMode.THUMBNAIL_GRID -> {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredGalleryItems, key = { it.id }) { item ->
                                                GalleryThumbnailCard(
                                                    item = item,
                                                    onConvertToProject = {
                                                        viewModel.prepareProjectFromGallery(item)
                                                        onNavigateToAddProject()
                                                    },
                                                    onDelete = { viewModel.deleteGalleryItem(item) }
                                                )
                                            }
                                        }
                                    }
                                    GalleryViewMode.MASONRY_GRID -> {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(2),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredGalleryItems, key = { it.id }) { item ->
                                                GalleryMasonryCard(
                                                    item = item,
                                                    onConvertToProject = {
                                                        viewModel.prepareProjectFromGallery(item)
                                                        onNavigateToAddProject()
                                                    },
                                                    onDelete = { viewModel.deleteGalleryItem(item) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: Inspiration Directory
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Expandable / collapsible multi-select tag filter menu
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isTagFilterExpanded = !isTagFilterExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedDirectoryTagFilters.isEmpty()) "Filter by Tags (All)"
                                        else "Filter by Tags (${selectedDirectoryTagFilters.size} selected)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (selectedDirectoryTagFilters.isNotEmpty()) {
                                            TextButton(onClick = { selectedDirectoryTagFilters = emptySet() }) {
                                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isTagFilterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isTagFilterExpanded) "Collapse tag filter" else "Expand tag filter"
                                        )
                                    }
                                }

                                if (isTagFilterExpanded && allDirectoryTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        allDirectoryTags.forEach { tag ->
                                            val isSelected = selectedDirectoryTagFilters.contains(tag)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedDirectoryTagFilters = if (isSelected) {
                                                        selectedDirectoryTagFilters - tag
                                                    } else {
                                                        selectedDirectoryTagFilters + tag
                                                    }
                                                },
                                                label = { Text(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredSources.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.BookmarkBorder,
                                title = "No Inspiration Destinations",
                                description = "Add your favorite craft blogs, pattern stores, and maker galleries.",
                                actionLabel = "Add Destination",
                                onActionClick = {
                                    editingSource = null
                                    sourceNameInput = ""
                                    sourceUrlInput = ""
                                    sourceNotesInput = ""
                                    sourceSelectedTags = emptySet()
                                    sourceNewTagInput = ""
                                    showSourceEditorDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredSources, key = { it.id }) { source ->
                                    InspirationSourceCard(
                                        source = source,
                                        onVisit = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.baseUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        onView = { viewingSource = source },
                                        onDelete = { viewModel.deleteInspirationSource(source) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Gallery Item Dialog
    if (showAddGalleryDialog) {
        val existingTags = allGalleryTags
        AlertDialog(
            onDismissRequest = { showAddGalleryDialog = false },
            title = { Text("Add Inspiration Image", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (newGalleryPhotoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            CraftPhotoThumbnail(photoUri = newGalleryPhotoUri, modifier = Modifier.fillMaxSize())
                        }
                    }

                    OutlinedTextField(
                        value = newGallerySourceUrl,
                        onValueChange = { newGallerySourceUrl = it },
                        label = { Text("Source URL (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newGalleryNotes,
                        onValueChange = { newGalleryNotes = it },
                        label = { Text("Notes / Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    if (existingTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            existingTags.forEach { tag ->
                                val isSelected = newGallerySelectedTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        newGallerySelectedTags = if (isSelected) newGallerySelectedTags - tag else newGallerySelectedTags + tag
                                    },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newGalleryNewTagInput,
                            onValueChange = { newGalleryNewTagInput = it },
                            label = { Text("New Tag") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newGalleryNewTagInput.isNotBlank()) {
                                    val t = newGalleryNewTagInput.trim()
                                    newGallerySelectedTags = newGallerySelectedTags + t
                                    newGalleryNewTagInput = ""
                                }
                            },
                            enabled = newGalleryNewTagInput.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        newGalleryPhotoUri?.let { uri ->
                            viewModel.addGalleryItem(
                                photoUri = uri,
                                sourceUrl = newGallerySourceUrl,
                                notes = newGalleryNotes,
                                tags = newGallerySelectedTags.toList()
                            )
                        }
                        showAddGalleryDialog = false
                    },
                    enabled = newGalleryPhotoUri != null
                ) {
                    Text("Save to Gallery")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGalleryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View Mode Dialog for Inspiration Destination
    if (viewingSource != null) {
        val src = viewingSource!!
        AlertDialog(
            onDismissRequest = { viewingSource = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(src.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        viewingSource = null
                        editingSource = src
                        sourceNameInput = src.name
                        sourceUrlInput = src.baseUrl
                        sourceNotesInput = src.description
                        sourceSelectedTags = src.craftTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                        sourceNewTagInput = ""
                        showSourceEditorDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit destination")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("URL: ${src.baseUrl}", style = MaterialTheme.typography.bodyMedium)
                    if (src.description.isNotBlank()) {
                        Text("Notes: ${src.description}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (src.craftTags.isNotBlank()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            src.craftTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                                CraftTagChip(tag)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewingSource = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Destination Editor Dialog (Adding or Editing)
    if (showSourceEditorDialog) {
        val existingTags = allDirectoryTags
        AlertDialog(
            onDismissRequest = { showSourceEditorDialog = false },
            title = { Text(if (editingSource == null) "Add Destination" else "Edit Destination", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = sourceNameInput,
                        onValueChange = { sourceNameInput = it },
                        label = { Text("Destination Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sourceUrlInput,
                        onValueChange = { sourceUrlInput = it },
                        label = { Text("URL *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sourceNotesInput,
                        onValueChange = { sourceNotesInput = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    if (existingTags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            existingTags.forEach { tag ->
                                val isSelected = sourceSelectedTags.contains(tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        sourceSelectedTags = if (isSelected) sourceSelectedTags - tag else sourceSelectedTags + tag
                                    },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = sourceNewTagInput,
                            onValueChange = { sourceNewTagInput = it },
                            label = { Text("New Tag") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (sourceNewTagInput.isNotBlank()) {
                                    val t = sourceNewTagInput.trim()
                                    sourceSelectedTags = sourceSelectedTags + t
                                    sourceNewTagInput = ""
                                }
                            },
                            enabled = sourceNewTagInput.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sourceNameInput.isNotBlank() && sourceUrlInput.isNotBlank()) {
                            val tagsStr = sourceSelectedTags.joinToString(",")
                            if (editingSource == null) {
                                viewModel.addInspirationSource(
                                    name = sourceNameInput.trim(),
                                    baseUrl = sourceUrlInput.trim(),
                                    craftTags = tagsStr,
                                    description = sourceNotesInput.trim()
                                )
                            } else {
                                val updated = editingSource!!.copy(
                                    name = sourceNameInput.trim(),
                                    baseUrl = sourceUrlInput.trim(),
                                    craftTags = tagsStr,
                                    description = sourceNotesInput.trim()
                                )
                                viewModel.updateInspirationSource(updated)
                            }
                            showSourceEditorDialog = false
                            editingSource = null
                        }
                    },
                    enabled = sourceNameInput.isNotBlank() && sourceUrlInput.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceEditorDialog = false
                    editingSource = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

enum class GalleryViewMode {
    THUMBNAIL_GRID,
    MASONRY_GRID
}

@Composable
fun GalleryThumbnailCard(
    item: InspirationGalleryItemEntity,
    onConvertToProject: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CraftPhotoThumbnail(photoUri = item.photoUri, modifier = Modifier.fillMaxSize())

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Button(
                onClick = onConvertToProject,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp)
            ) {
                Text("To Project", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryMasonryCard(
    item: InspirationGalleryItemEntity,
    onConvertToProject: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                CraftPhotoThumbnail(photoUri = item.photoUri, modifier = Modifier.fillMaxSize())
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.sourceUrl.isNotBlank()) {
                    Text(
                        text = item.sourceUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.tags.isNotBlank()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                            CraftTagChip(tag)
                        }
                    }
                }

                Button(
                    onClick = onConvertToProject,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.DesignServices, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Convert to Project", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InspirationSourceCard(
    source: InspirationSourceEntity,
    onVisit: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Actions: Visit (link icon), View (eye icon), Delete (trashcan icon). No toggle.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVisit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Link, contentDescription = "Visit destination", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onView, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Visibility, contentDescription = "View details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete destination", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (source.description.isNotBlank()) {
                Text(
                    text = source.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (source.craftTags.isNotBlank()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    source.craftTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                        CraftTagChip(tag)
                    }
                }
            }
        }
    }
}
