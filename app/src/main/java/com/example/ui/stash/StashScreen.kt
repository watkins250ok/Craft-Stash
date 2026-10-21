package com.example.ui.stash

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.dao.SupplyWithBatches
import com.example.data.entity.StorageLocationEntity
import com.example.domain.measurement.UnitConverter
import com.example.domain.model.SortOrder
import com.example.domain.model.StashViewMode
import com.example.ui.CraftStashViewModel
import com.example.ui.StashFilterState
import com.example.ui.common.CraftPhotoThumbnail
import com.example.ui.common.CraftTagChip
import com.example.ui.common.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StashScreen(
    viewModel: CraftStashViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToDetailedAdd: () -> Unit,
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val supplies by viewModel.filteredSupplies.collectAsState()
    val allSupplies by viewModel.allSupplies.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val selectedSupplyIds by viewModel.selectedSupplyIds.collectAsState()
    val locations by viewModel.locations.collectAsState()

    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Craft Stash",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        // Scan Barcode
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onNavigateToScanner()
                            },
                            icon = { Icon(Icons.Default.QrCodeScanner, null) },
                            text = { Text("Scan Barcode") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.testTag("fab_scan_barcode")
                        )

                        // Quick Add
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                showQuickAddDialog = true
                            },
                            icon = { Icon(Icons.Default.FlashOn, null) },
                            text = { Text("Quick Add") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.testTag("fab_quick_add")
                        )

                        // Detailed Add
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onNavigateToDetailedAdd()
                            },
                            icon = { Icon(Icons.Default.EditNote, null) },
                            text = { Text("Detailed Add") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.testTag("fab_detailed_add")
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_supply_fab")
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Add craft supply options"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            SearchBarRow(
                query = filterState.query,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )

            // View modes, grid column options (if grid), and filter options on the same line beneath search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View options at a glance (Grid, Card, Minimal)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                            .padding(2.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.setViewMode(StashViewMode.GRID) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (filterState.viewMode == StashViewMode.GRID) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    CircleShape
                                )
                                .testTag("view_mode_grid")
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Grid view",
                                tint = if (filterState.viewMode == StashViewMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setViewMode(StashViewMode.CARDS) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (filterState.viewMode == StashViewMode.CARDS) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    CircleShape
                                )
                                .testTag("view_mode_cards")
                        ) {
                            Icon(
                                Icons.Default.ViewAgenda,
                                contentDescription = "Card view",
                                tint = if (filterState.viewMode == StashViewMode.CARDS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setViewMode(StashViewMode.MINIMAL) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (filterState.viewMode == StashViewMode.MINIMAL) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    CircleShape
                                )
                                .testTag("view_mode_minimal")
                        ) {
                            Icon(
                                Icons.Default.ViewList,
                                contentDescription = "Minimal view",
                                tint = if (filterState.viewMode == StashViewMode.MINIMAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Column options for Grid view (on the same line)
                    if (filterState.viewMode == StashViewMode.GRID) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = filterState.gridColumns == 2,
                                onClick = { viewModel.setGridColumns(2) },
                                label = { Text("2 Col", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = filterState.gridColumns == 3,
                                onClick = { viewModel.setGridColumns(3) },
                                label = { Text("3 Col", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Filter options button
                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.testTag("filter_options_button")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter options")
                }
            }

            // Selection bar for "Get Ideas" when supplies are checked
            AnimatedVisibility(visible = selectedSupplyIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedSupplyIds.size} supply selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("Clear")
                            }
                            Button(
                                onClick = {
                                    viewModel.selectTab(2) // Jump to Ideas tab
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("get_ideas_for_selection_button")
                            ) {
                                Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Get Ideas",
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Active Filters Row (if any)
            if (filterState.selectedCategory != null || filterState.selectedLocationId != null || filterState.selectedColor != null || filterState.selectedMaterial != null || filterState.selectedDimension != null || filterState.selectedTag != null || filterState.favoritesOnly || filterState.availableOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active filters applied",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { viewModel.clearAllFilters() }) {
                        Text("Clear all", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Main Content Area
            if (allSupplies.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Category,
                    title = "Your Craft Stash is Empty",
                    description = "Start cataloging your yarns, fabrics, threads, beads, and tools to rediscover what you own.",
                    actionLabel = "Load Sample Stash (Preview)",
                    onActionClick = { viewModel.loadSampleData() },
                    modifier = Modifier.weight(1f)
                )
            } else if (supplies.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.SearchOff,
                    title = "No Matching Supplies",
                    description = "No items match your active search or filters.",
                    actionLabel = "Clear Filters",
                    onActionClick = { viewModel.clearAllFilters() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    when (filterState.viewMode) {
                        StashViewMode.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(filterState.gridColumns),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(supplies, key = { it.supply.id }) { item ->
                                    SupplyGridCard(
                                        item = item,
                                        isSelected = selectedSupplyIds.contains(item.supply.id),
                                        onSelectToggle = { viewModel.toggleSupplySelection(item.supply.id) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(item.supply) },
                                        onClick = { onNavigateToDetail(item.supply.id) }
                                    )
                                }
                            }
                        }
                        StashViewMode.CARDS -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(supplies, key = { it.supply.id }) { item ->
                                    SupplyDetailCard(
                                        item = item,
                                        isSelected = selectedSupplyIds.contains(item.supply.id),
                                        onSelectToggle = { viewModel.toggleSupplySelection(item.supply.id) },
                                        onFavoriteToggle = { viewModel.toggleFavorite(item.supply) },
                                        onClick = { onNavigateToDetail(item.supply.id) },
                                        locations = locations
                                    )
                                }
                            }
                        }
                        StashViewMode.MINIMAL -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(supplies, key = { it.supply.id }) { item ->
                                    SupplyCompactRow(
                                        item = item,
                                        isSelected = selectedSupplyIds.contains(item.supply.id),
                                        onSelectToggle = { viewModel.toggleSupplySelection(item.supply.id) },
                                        onClick = { onNavigateToDetail(item.supply.id) },
                                        locations = locations
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Add Dialog
    if (showQuickAddDialog) {
        QuickAddDialog(
            locations = locations,
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { name, category, locationId ->
                viewModel.quickAddSupply(name, category, locationId, null)
                showQuickAddDialog = false
            }
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            filterState = filterState,
            locations = locations,
            onDismiss = { showFilterSheet = false },
            onToggleFavorites = { viewModel.toggleFavoritesFilter() },
            onToggleAvailableOnly = { viewModel.toggleAvailableOnlyFilter() },
            onSelectLocation = { viewModel.setLocationFilter(it) },
            onSelectCategory = { viewModel.setCategoryFilter(it) },
            onSelectColor = { viewModel.setColorFilter(it) },
            onSelectMaterial = { viewModel.setMaterialFilter(it) },
            onSelectDimension = { viewModel.setDimensionFilter(it) },
            onSelectTag = { viewModel.setTagFilter(it) },
            onSelectSort = { viewModel.setSortOrder(it) },
            onClearAll = {
                viewModel.clearAllFilters()
                showFilterSheet = false
            }
        )
    }
}

@Composable
fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search stash") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stash_search_input")
        )
    }
}

@Composable
fun SupplyGridCard(
    item: SupplyWithBatches,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("supply_card_${item.supply.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                CraftPhotoThumbnail(
                    photoUri = item.supply.coverPhotoUri,
                    modifier = Modifier.fillMaxSize()
                )

                // Select Checkbox in top left
                IconButton(
                    onClick = onSelectToggle,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Select supply",
                        tint = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Favorite button top right
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (item.supply.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.supply.isFavorite) Color(0xFFE57373) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.supply.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SupplyDetailCard(
    item: SupplyWithBatches,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    locations: List<StorageLocationEntity>,
    modifier: Modifier = Modifier
) {
    val locationName = locations.find { it.id == item.batches.firstOrNull()?.storageLocationId }?.name

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CraftPhotoThumbnail(
                photoUri = item.supply.coverPhotoUri,
                modifier = Modifier.size(76.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.supply.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.supply.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.supply.isFavorite) Color(0xFFE57373) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 2.dp)) {
                    CraftTagChip(item.supply.category)
                    if (item.supply.material.isNotBlank()) {
                        CraftTagChip(item.supply.material)
                    }
                }

                if (!locationName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Batch count & quantity
                val activeBatches = item.batches.filter { !it.isDepleted }
                Text(
                    text = "${activeBatches.size} piece(s) / batch(es) on hand",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SupplyCompactRow(
    item: SupplyWithBatches,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onClick: () -> Unit,
    locations: List<StorageLocationEntity>,
    modifier: Modifier = Modifier
) {
    val locationName = locations.find { it.id == item.batches.firstOrNull()?.storageLocationId }?.name

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CraftPhotoThumbnail(photoUri = item.supply.coverPhotoUri, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.supply.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(item.supply.category, locationName).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Checkbox(checked = isSelected, onCheckedChange = { onSelectToggle() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddDialog(
    locations: List<StorageLocationEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, locationId: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Yarn") }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Yarn", "Fabric", "Paint & Liquid Media", "Beads & Jewelry", "Paper & Vinyl", "Tools", "Ribbon & Trim", "General Craft")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Supply", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter essentials now; all other details and photos can be added later.", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supply Name *") },
                    placeholder = { Text("e.g. Navy Blue Wool Yarn") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_add_name_input")
                )

                // Category selector
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                // Storage location selector
                if (locations.isNotEmpty()) {
                    var locExpanded by remember { mutableStateOf(false) }
                    val currentLocName = locations.find { it.id == selectedLocationId }?.name ?: "No Location Selected"
                    ExposedDropdownMenuBox(
                        expanded = locExpanded,
                        onExpandedChange = { locExpanded = !locExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentLocName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Storage Location") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = locExpanded,
                            onDismissRequest = { locExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedLocationId = null
                                    locExpanded = false
                                }
                            )
                            locations.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc.name) },
                                    onClick = {
                                        selectedLocationId = loc.id
                                        locExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category, selectedLocationId) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("quick_add_confirm_button")
            ) {
                Text("Save to Stash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filterState: StashFilterState,
    locations: List<StorageLocationEntity>,
    onDismiss: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleAvailableOnly: () -> Unit,
    onSelectLocation: (String?) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectColor: (String?) -> Unit,
    onSelectMaterial: (String?) -> Unit,
    onSelectDimension: (String?) -> Unit,
    onSelectTag: (String?) -> Unit,
    onSelectSort: (SortOrder) -> Unit,
    onClearAll: () -> Unit
) {
    var colorInput by remember { mutableStateOf(filterState.selectedColor ?: "") }
    var materialInput by remember { mutableStateOf(filterState.selectedMaterial ?: "") }
    var dimensionInput by remember { mutableStateOf(filterState.selectedDimension ?: "") }
    var tagInput by remember { mutableStateOf(filterState.selectedTag ?: "") }

    val categories = listOf("All", "Yarn", "Fabric", "Paint & Liquid Media", "Beads & Jewelry", "Paper & Vinyl", "Tools", "Ribbon & Trim")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter & Sort Stash", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClearAll) {
                    Text("Clear All")
                }
            }

            // Quick toggles
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = filterState.favoritesOnly,
                    onClick = onToggleFavorites,
                    label = { Text("Favorites Only") },
                    leadingIcon = { Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = filterState.availableOnly,
                    onClick = onToggleAvailableOnly,
                    label = { Text("Available Only") },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                )
            }

            // Categories
            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = if (cat == "All") filterState.selectedCategory == null else filterState.selectedCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectCategory(if (cat == "All") null else if (isSelected) null else cat)
                        },
                        label = { Text(cat) }
                    )
                }
            }

            // Sort order
            Text("Sort By", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SortOrder.values()) { order ->
                    FilterChip(
                        selected = filterState.sortOrder == order,
                        onClick = { onSelectSort(order) },
                        label = { Text(order.displayName) }
                    )
                }
            }

            // Filter by Location
            if (locations.isNotEmpty()) {
                Text("Storage Location", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filterState.selectedLocationId == null,
                            onClick = { onSelectLocation(null) },
                            label = { Text("All Locations") }
                        )
                    }
                    items(locations) { loc ->
                        FilterChip(
                            selected = filterState.selectedLocationId == loc.id,
                            onClick = { onSelectLocation(if (filterState.selectedLocationId == loc.id) null else loc.id) },
                            label = { Text(loc.name) }
                        )
                    }
                }
            }

            // Color input
            OutlinedTextField(
                value = colorInput,
                onValueChange = { colorInput = it; onSelectColor(it.ifBlank { null }) },
                label = { Text("Color") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Material input
            OutlinedTextField(
                value = materialInput,
                onValueChange = { materialInput = it; onSelectMaterial(it.ifBlank { null }) },
                label = { Text("Material") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Dimensions input
            OutlinedTextField(
                value = dimensionInput,
                onValueChange = { dimensionInput = it; onSelectDimension(it.ifBlank { null }) },
                label = { Text("Dimensions (Length or Width)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Tags input
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it; onSelectTag(it.ifBlank { null }) },
                label = { Text("Tags") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply")
            }
        }
    }
}
