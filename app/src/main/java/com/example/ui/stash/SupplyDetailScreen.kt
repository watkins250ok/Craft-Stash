package com.example.ui.stash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.dao.SupplyWithBatches
import com.example.data.entity.BatchEntity
import com.example.data.entity.StorageLocationEntity
import com.example.domain.measurement.UnitConverter
import com.example.domain.model.QuantityMode
import com.example.ui.CraftStashViewModel
import com.example.ui.common.CraftPhotoThumbnail
import com.example.ui.common.CraftTagChip
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyDetailScreen(
    supplyId: String,
    viewModel: CraftStashViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToIdeasWithSupply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val supplyFlow = remember(supplyId) { viewModel.repository.getSupplyFlow(supplyId) }
    val supplyWithBatches by supplyFlow.collectAsState(initial = null)
    val locations by viewModel.locations.collectAsState()

    var showAddBatchDialog by remember { mutableStateOf(false) }
    var editingBatch by remember { mutableStateOf<BatchEntity?>(null) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (supplyWithBatches == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val item = supplyWithBatches!!

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(item.supply.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(item.supply) }
                    ) {
                        Icon(
                            imageVector = if (item.supply.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.supply.isFavorite) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showDuplicateDialog = true },
                        modifier = Modifier.testTag("duplicate_supply_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate supply")
                    }
                    IconButton(
                        onClick = { onNavigateToEdit(item.supply.id) },
                        modifier = Modifier.testTag("edit_supply_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit supply")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_supply_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete supply")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddBatchDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_batch_button")
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Piece / Batch")
                    }

                    Button(
                        onClick = { onNavigateToIdeasWithSupply(item.supply.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("get_ideas_button")
                    ) {
                        Icon(Icons.Default.Lightbulb, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Ideas")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photos banner / gallery
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    CraftPhotoThumbnail(
                        photoUri = item.supply.coverPhotoUri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Supply identity info
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.supply.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CraftTagChip(item.supply.category)
                        if (item.supply.craftTypes.isNotBlank()) {
                            CraftTagChip(item.supply.craftTypes)
                        }
                    }

                    // Key attributes
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.supply.brand.isNotBlank()) {
                            AttributeRow(label = "Brand / Line", value = item.supply.brand)
                        }
                        if (item.supply.material.isNotBlank()) {
                            AttributeRow(label = "Material / Fiber", value = item.supply.material)
                        }
                        if (item.supply.manufacturerColor.isNotBlank() || item.supply.colorFamilies.isNotBlank()) {
                            val colorText = listOfNotNull(
                                item.supply.manufacturerColor.ifBlank { null },
                                if (item.supply.colorFamilies.isNotBlank()) "(${item.supply.colorFamilies})" else null
                            ).joinToString(" ")
                            AttributeRow(label = "Colorway", value = colorText)
                        }
                        if (item.supply.appearance.isNotBlank()) {
                            AttributeRow(label = "Appearance", value = item.supply.appearance)
                        }
                        if (item.supply.barcode.isNotBlank()) {
                            AttributeRow(label = "Barcode", value = item.supply.barcode)
                        }
                    }
                }
            }

            // Notes
            if (item.supply.notes.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.supply.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Batches / Physical Pieces Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Physical Batches & Pieces (${item.batches.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (item.batches.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No distinct pieces or batches logged yet. Tap 'Add Piece / Batch' to record skeins, bolts, cut pieces, or remnants.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(item.batches, key = { it.id }) { batch ->
                    BatchItemCard(
                        batch = batch,
                        locations = locations,
                        onEdit = { editingBatch = batch },
                        onToggleDepleted = { viewModel.toggleDepleted(batch) },
                        onDelete = { viewModel.deleteBatch(batch) }
                    )
                }
            }
        }
    }

    // Add Batch Dialog
    if (showAddBatchDialog) {
        BatchEditDialog(
            batch = null,
            supplyId = item.supply.id,
            locations = locations,
            onDismiss = { showAddBatchDialog = false },
            onSave = { newBatch ->
                viewModel.saveBatch(newBatch)
                showAddBatchDialog = false
            }
        )
    }

    // Edit Batch Dialog
    if (editingBatch != null) {
        BatchEditDialog(
            batch = editingBatch,
            supplyId = item.supply.id,
            locations = locations,
            onDismiss = { editingBatch = null },
            onSave = { updatedBatch ->
                viewModel.updateBatch(updatedBatch)
                editingBatch = null
            }
        )
    }

    // Duplicate Dialog
    if (showDuplicateDialog) {
        DuplicateSupplyDialog(
            supply = item.supply,
            locations = locations,
            onDismiss = { showDuplicateDialog = false },
            onConfirm = { batchIdent, qty, mode, unit, locId ->
                viewModel.duplicateSupply(
                    sourceSupplyId = item.supply.id,
                    batchIdentifier = batchIdent,
                    qty = qty,
                    mode = mode,
                    unit = unit,
                    locationId = locId,
                    onSuccess = { newId ->
                        showDuplicateDialog = false
                        onNavigateToEdit(newId)
                    }
                )
            }
        )
    }

    // Confirm Delete Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Supply?") },
            text = { Text("Are you sure you want to delete \"${item.supply.name}\" and all its physical pieces? Linked project records will be kept intact without allocated stock.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSupply(item.supply)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BatchItemCard(
    batch: BatchEntity,
    locations: List<StorageLocationEntity>,
    onEdit: () -> Unit,
    onToggleDepleted: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locationName = locations.find { it.id == batch.storageLocationId }?.name

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (batch.isDepleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (batch.isDepleted) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = batch.batchIdentifier.ifBlank { "Main Piece" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = if (batch.isDepleted) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (batch.isDepleted) "Depleted" else "Active",
                        color = if (batch.isDepleted) Color(0xFFC62828) else Color(0xFF2E7D32),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Quantity & Units
            Text(
                text = UnitConverter.formatDisplayQuantity(batch.quantityValue, batch.unit, batch.quantityMode),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Dimensions if tracked
            if (batch.dimensionWidth != null && batch.dimensionLength != null) {
                Text(
                    text = "Dimensions: ${UnitConverter.formatQuantity(batch.dimensionWidth)} x ${UnitConverter.formatQuantity(batch.dimensionLength)} ${batch.dimensionUnit.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Location & Condition
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!locationName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(locationName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                if (batch.condition.isNotBlank()) {
                    Text("• ${batch.condition}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Batch notes
            if (batch.notes.isNotBlank()) {
                Text(batch.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleDepleted) {
                    Text(if (batch.isDepleted) "Reactivate" else "Mark Depleted")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit batch", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete batch", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditDialog(
    batch: BatchEntity?,
    supplyId: String,
    locations: List<StorageLocationEntity>,
    onDismiss: () -> Unit,
    onSave: (BatchEntity) -> Unit
) {
    var identifier by remember { mutableStateOf(batch?.batchIdentifier.orEmpty()) }
    var quantityMode by remember { mutableStateOf(batch?.quantityMode ?: QuantityMode.EXACT) }
    var quantityStr by remember { mutableStateOf(batch?.quantityValue?.toString().orEmpty()) }
    var unit by remember { mutableStateOf(batch?.unit.orEmpty()) }
    var condition by remember { mutableStateOf(batch?.condition ?: "Sealed") }
    var selectedLocationId by remember { mutableStateOf(batch?.storageLocationId) }
    var widthStr by remember { mutableStateOf(batch?.dimensionWidth?.toString().orEmpty()) }
    var lengthStr by remember { mutableStateOf(batch?.dimensionLength?.toString().orEmpty()) }
    var dimUnit by remember { mutableStateOf(batch?.dimensionUnit ?: "inches") }
    var notes by remember { mutableStateOf(batch?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (batch == null) "Add Piece or Batch" else "Edit Piece or Batch", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text("Identifier (e.g. Dye lot #402, Piece A)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quantity tracking mode selector
                item {
                    Text("Quantity Tracking Mode", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QuantityMode.values().forEach { mode ->
                            FilterChip(
                                selected = quantityMode == mode,
                                onClick = { quantityMode = mode },
                                label = { Text(mode.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (quantityMode != QuantityMode.PRESENCE_ONLY) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quantityStr,
                                onValueChange = { quantityStr = it },
                                label = { Text("Quantity") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Unit (g, yd, skein)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Dimensions for fabric / paper pieces
                item {
                    Text("Dimensions (Optional - for cut pieces)", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = widthStr,
                            onValueChange = { widthStr = it },
                            label = { Text("Width") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lengthStr,
                            onValueChange = { lengthStr = it },
                            label = { Text("Length") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dimUnit,
                            onValueChange = { dimUnit = it },
                            label = { Text("Dim Unit") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Location selector
                if (locations.isNotEmpty()) {
                    item {
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
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = locExpanded,
                                onDismissRequest = { locExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = { selectedLocationId = null; locExpanded = false }
                                )
                                locations.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc.name) },
                                        onClick = { selectedLocationId = loc.id; locExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // Condition
                item {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = { condition = it },
                        label = { Text("Condition (Sealed, Opened, Remnants, Scraps)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Batch Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qVal = quantityStr.toDoubleOrNull()
                    val wVal = widthStr.toDoubleOrNull()
                    val lVal = lengthStr.toDoubleOrNull()
                    val result = (batch ?: BatchEntity(
                        id = UUID.randomUUID().toString(),
                        supplyId = supplyId
                    )).copy(
                        batchIdentifier = identifier.trim(),
                        quantityMode = quantityMode,
                        quantityValue = qVal,
                        unit = unit.trim().ifBlank { null },
                        dimensionWidth = wVal,
                        dimensionLength = lVal,
                        dimensionUnit = dimUnit.trim().ifBlank { null },
                        storageLocationId = selectedLocationId,
                        condition = condition.trim(),
                        notes = notes.trim()
                    )
                    onSave(result)
                }
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

@Composable
fun DuplicateSupplyDialog(
    supply: com.example.data.entity.SupplyEntity,
    locations: List<StorageLocationEntity>,
    onDismiss: () -> Unit,
    onConfirm: (batchIdent: String, qty: Double?, mode: QuantityMode, unit: String?, locationId: String?) -> Unit
) {
    var identifier by remember { mutableStateOf("New Batch") }
    var quantityStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var quantityMode by remember { mutableStateOf(QuantityMode.EXACT) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplicate \"${supply.name}\"", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Copies all product attributes (material, category, craft types, brand) into a new supply record. Existing reservations and usage history will not be copied.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("New Batch Identifier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        identifier.trim(),
                        quantityStr.toDoubleOrNull(),
                        quantityMode,
                        unit.trim().ifBlank { null },
                        selectedLocationId
                    )
                }
            ) {
                Text("Duplicate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
