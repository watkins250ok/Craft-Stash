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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.common.CraftPhotoThumbnail
import com.example.ui.common.CraftTagChip
import com.example.ui.common.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StashScreen(
    viewModel: CraftStashViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToDetailedAdd: () -> Unit,
    onNavigateToDraftInbox: () -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val supplies by viewModel.filteredSupplies.collectAsState()
    val allSupplies by viewModel.allSupplies.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val selectedSupplyIds by viewModel.selectedSupplyIds.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val locations by viewModel.locations.collectAsState()

    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val categories = listOf("All", "Yarn", "Fabric", "Paint & Liquid Media", "Beads & Jewelry", "Paper & Vinyl", "Tools", "Ribbon & Trim")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Craft Stash",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Draft inbox icon with badge
                    BadgedBox(
                        badge = {
                            if (drafts.isNotEmpty()) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("${drafts.size}")
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onNavigateToDraftInbox,
                            modifier = Modifier.testTag("draft_inbox_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Draft photo inbox")
                        }
                    }

                    // Storage Locations button
                    IconButton(
                        onClick = onNavigateToLocations,
                        modifier = Modifier.testTag("storage_locations_button")
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Storage locations")
                    }

                    // Settings
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings and backup")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Search Bar & Filter Controls
            SearchBarRow(
                query = filterState.query,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                viewMode = filterState.viewMode,
                onToggleViewMode = {
                    val next = when (filterState.viewMode) {
                        StashViewMode.GRID -> StashViewMode.CARDS
                        StashViewMode.CARDS -> StashViewMode.COMPACT_LIST
                        StashViewMode.COMPACT_LIST -> StashViewMode.GRID
                    }
                    viewModel.setViewMode(next)
                },
                onOpenFilters = { showFilterSheet = true }
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = if (cat == "All") filterState.selectedCategory == null else filterState.selectedCategory.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (cat == "All") viewModel.setCategoryFilter(null)
                            else viewModel.setCategoryFilter(if (isSelected) null else cat)
                        },
                        label = { Text(cat) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
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
                                Icon(Icons.Default.Lightbulb, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Get Ideas")
                            }
                        }
                    }
                }
            }

            // Active Filters Row (if any)
            if (filterState.selectedCraftType != null || filterState.selectedLocationId != null || filterState.favoritesOnly || filterState.availableOnly) {
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
                                columns = GridCells.Adaptive(minSize = 160.dp),
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
                                        onClick = { onNavigateToDetail(item.supply.id) },
                                        locations = locations
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
                        StashViewMode.COMPACT_LIST -> {
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
            onSelectCraft = { viewModel.setCraftFilter(it) },
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
    viewMode: StashViewMode,
    onToggleViewMode: () -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search name, brand, material, color, barcode...") },
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
                .weight(1f)
                .testTag("stash_search_input")
        )

        // View Mode toggle icon
        IconButton(
            onClick = onToggleViewMode,
            modifier = Modifier.testTag("toggle_view_mode_button")
        ) {
            val icon = when (viewMode) {
                StashViewMode.GRID -> Icons.Default.GridView
                StashViewMode.CARDS -> Icons.Default.ViewAgenda
                StashViewMode.COMPACT_LIST -> Icons.Default.ViewList
            }
            Icon(icon, contentDescription = "Toggle view mode")
        }

        // Filter button
        IconButton(
            onClick = onOpenFilters,
            modifier = Modifier.testTag("filter_options_button")
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Filter options")
        }
    }
}

@Composable
fun SupplyGridCard(
    item: SupplyWithBatches,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    locations: List<StorageLocationEntity>,
    modifier: Modifier = Modifier
) {
    val totalQty = item.batches.filter { !it.isDepleted && it.quantityValue != null }.sumOf { it.quantityValue ?: 0.0 }
    val primaryUnit = item.batches.firstOrNull()?.unit.orEmpty()
    val locationName = locations.find { it.id == item.batches.firstOrNull()?.storageLocationId }?.name

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
                    .height(130.dp)
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

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.supply.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.supply.category.isNotBlank()) {
                    Text(
                        text = item.supply.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Quantity / Batches info
                val qtyText = if (item.batches.isEmpty()) {
                    "No pieces logged"
                } else {
                    val activeBatches = item.batches.filter { !it.isDepleted }
                    if (activeBatches.isEmpty()) "Depleted"
                    else "${activeBatches.size} batch - ${UnitConverter.formatDisplayQuantity(totalQty, primaryUnit, item.batches.first().quantityMode)}"
                }
                Text(
                    text = qtyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!locationName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
    filterState: com.example.ui.StashFilterState,
    locations: List<StorageLocationEntity>,
    onDismiss: () -> Unit,
    onToggleFavorites: () -> Unit,
    onToggleAvailableOnly: () -> Unit,
    onSelectLocation: (String?) -> Unit,
    onSelectCraft: (String?) -> Unit,
    onSelectSort: (SortOrder) -> Unit,
    onClearAll: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                Text("Filter by Location", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply")
            }
        }
    }
}
