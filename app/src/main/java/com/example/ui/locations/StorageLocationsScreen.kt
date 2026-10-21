package com.example.ui.locations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.StorageLocationEntity
import com.example.ui.CraftStashViewModel
import com.example.ui.common.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageLocationsScreen(
    viewModel: CraftStashViewModel,
    onNavigateBack: () -> Unit,
    onSelectLocationForFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val locations by viewModel.locations.collectAsState()
    val allSupplies by viewModel.allSupplies.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var parentForNewLocation by remember { mutableStateOf<String?>(null) }
    var locationToDelete by remember { mutableStateOf<StorageLocationEntity?>(null) }

    // Map parent locations to their nested sub-locations
    val rootLocations = locations.filter { it.parentId.isNullOrBlank() }
    val childMap = locations.filter { !it.parentId.isNullOrBlank() }.groupBy { it.parentId!! }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Storage Locations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            parentForNewLocation = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_root_location_button")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add root location")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    parentForNewLocation = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_location_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add storage location")
            }
        }
    ) { innerPadding ->
        if (locations.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Inventory2,
                title = "No Storage Locations",
                description = "Organize your craft room into rooms, bins, rolling carts, shelves, and drawers.",
                actionLabel = "Add First Location",
                onActionClick = {
                    parentForNewLocation = null
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Craft Room Hierarchy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                rootLocations.forEach { root ->
                    item(key = root.id) {
                        LocationCard(
                            location = root,
                            itemCount = allSupplies.count { s -> s.batches.any { it.storageLocationId == root.id } },
                            isNested = false,
                            onAddSubLocation = {
                                parentForNewLocation = root.id
                                showAddDialog = true
                            },
                            onDelete = { locationToDelete = root },
                            onFilter = {
                                onSelectLocationForFilter(root.id)
                                onNavigateBack()
                            }
                        )
                    }

                    // Render nested children
                    childMap[root.id]?.forEach { child ->
                        item(key = child.id) {
                            LocationCard(
                                location = child,
                                itemCount = allSupplies.count { s -> s.batches.any { it.storageLocationId == child.id } },
                                isNested = true,
                                onAddSubLocation = {
                                    parentForNewLocation = child.id
                                    showAddDialog = true
                                },
                                onDelete = { locationToDelete = child },
                                onFilter = {
                                    onSelectLocationForFilter(child.id)
                                    onNavigateBack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val parentName = locations.find { it.id == parentForNewLocation }?.name
        AddLocationDialog(
            parentName = parentName,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, notes ->
                viewModel.saveLocation(name, parentForNewLocation, notes)
                showAddDialog = false
            }
        )
    }

    if (locationToDelete != null) {
        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            title = { Text("Delete Location?") },
            text = { Text("Delete \"${locationToDelete?.name}\"? Supplies stored here will simply have their location cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        locationToDelete?.let { viewModel.deleteLocation(it) }
                        locationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { locationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LocationCard(
    location: StorageLocationEntity,
    itemCount: Int,
    isNested: Boolean,
    onAddSubLocation: () -> Unit,
    onDelete: () -> Unit,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNested) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = if (isNested) 28.dp else 0.dp)
            .clickable(onClick = onFilter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isNested) Icons.Default.SubdirectoryArrowRight else Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (location.notes.isNotBlank()) {
                    Text(
                        text = location.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$itemCount item(s) stored here (tap to view)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onAddSubLocation, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add sub-location", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete location", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddLocationDialog(
    parentName: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (parentName != null) "Add Sub-location in \"$parentName\""
                else "Add Storage Location",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name *") },
                    placeholder = { Text("e.g. Rolling Cart, Top Shelf, Basket A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("location_name_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. In craft closet on right") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), notes.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_location_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
