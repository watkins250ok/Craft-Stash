package com.example.ui.projects

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.dao.SupplyWithBatches
import com.example.data.entity.BatchEntity
import com.example.data.entity.ProjectAllocationEntity
import com.example.data.entity.ProjectRequirementEntity
import com.example.domain.matching.ProjectMatcher
import com.example.domain.measurement.UnitConverter
import com.example.domain.model.CoverageState
import com.example.domain.model.MatchStatus
import com.example.domain.model.ProjectStatus
import com.example.domain.model.QuantityMode
import com.example.ui.CraftStashViewModel
import com.example.ui.common.CoverageBadge
import com.example.ui.common.CraftPhotoThumbnail
import com.example.ui.common.CraftTagChip
import com.example.ui.common.ProjectStatusBadge
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: CraftStashViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val projectFlow = remember(projectId) { viewModel.repository.getProjectFlow(projectId) }
    val projectWithDetails by projectFlow.collectAsState(initial = null)
    val allSupplies by viewModel.allSupplies.collectAsState()
    val allAllocations by viewModel.allAllocations.collectAsState()
    val consumptionFlow = remember(projectId) { viewModel.repository.getConsumptionRecords(projectId) }
    val consumptionRecords by consumptionFlow.collectAsState(initial = emptyList())

    var showAddRequirementDialog by remember { mutableStateOf(false) }
    var requirementToAllocate by remember { mutableStateOf<ProjectRequirementEntity?>(null) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (projectWithDetails == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val item = projectWithDetails!!
    val project = item.project
    val eval = remember(item, allSupplies, allAllocations) {
        ProjectMatcher.evaluateProject(item, allSupplies, allAllocations)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(project.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToEdit(project.id) },
                        modifier = Modifier.testTag("edit_project_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit project")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_project_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete project")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddRequirementDialog = true },
                        modifier = Modifier.weight(1f).testTag("add_requirement_button")
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Requirement")
                    }

                    if (project.status != ProjectStatus.COMPLETED) {
                        Button(
                            onClick = { showCompletionDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).testTag("complete_project_button")
                        ) {
                            Icon(Icons.Default.DoneAll, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Completed")
                        }
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
            // Header card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CraftPhotoThumbnail(
                        photoUri = project.coverPhotoUri,
                        fallbackIcon = Icons.Default.DesignServices,
                        modifier = Modifier.size(80.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(project.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                        // Phase dropdown
                        var statusExpanded by remember { mutableStateOf(false) }
                        Box {
                            AssistChip(
                                onClick = { statusExpanded = true },
                                label = { Text(project.status.displayName) },
                                leadingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                }
                            )
                            DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                                ProjectStatus.values().forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st.displayName) },
                                        onClick = {
                                            viewModel.updateProjectStatus(project, st)
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (project.craftType.isNotBlank()) {
                            CraftTagChip(project.craftType)
                        }
                    }
                }
            }

            // External Source URL link
            if (!project.sourceUrl.isNullOrBlank()) {
                item {
                    OutlinedCard(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(project.sourceUrl))
                                context.startActivity(browserIntent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.sourceName?.ifBlank { "Pattern / Project Link" } ?: "Pattern / Project Link",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = project.sourceUrl!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Material Coverage Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (eval.isReadyToMake) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (eval.isReadyToMake) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (eval.isReadyToMake) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (eval.isReadyToMake) "Ready to Make" else "Material Coverage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = eval.coverageSummary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Requirements Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Materials & Requirements (${item.requirements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (item.requirements.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No structured requirements entered yet. You can keep this project lightweight, or add specific materials and tools below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(item.requirements, key = { it.id }) { req ->
                    val reqEval = eval.requirementEvaluations.find { it.requirement.id == req.id }
                    val state = reqEval?.coverageState ?: CoverageState.MISSING
                    val reqAllocations = item.allocations.filter { it.requirementId == req.id }

                    RequirementCard(
                        requirement = req,
                        coverageState = state,
                        allocations = reqAllocations,
                        allSupplies = allSupplies,
                        onAllocateClick = { requirementToAllocate = req },
                        onRemoveAllocation = { allocId -> viewModel.removeAllocation(allocId) },
                        onDeleteRequirement = { viewModel.deleteRequirement(req.id) }
                    )
                }
            }

            // Consumption records (usage history)
            if (consumptionRecords.isNotEmpty()) {
                item {
                    Text(
                        text = "Usage History (${consumptionRecords.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(consumptionRecords, key = { it.id }) { rec ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val qtyStr = if (rec.quantityUsed != null) "${UnitConverter.formatQuantity(rec.quantityUsed)} ${rec.unit.orEmpty()}" else "Used"
                                Text("Recorded: $qtyStr", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                if (rec.notes.isNotBlank()) {
                                    Text(rec.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            TextButton(onClick = { viewModel.undoConsumption(rec.id) }) {
                                Text("Undo")
                            }
                        }
                    }
                }
            }

            // Finished Measurements / Notes
            if (!project.finishedMeasurements.isNullOrBlank() || project.notes.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!project.finishedMeasurements.isNullOrBlank()) {
                                Text("Finished Measurements", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(project.finishedMeasurements!!, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (project.notes.isNotBlank()) {
                                Text("Notes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(project.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Requirement Dialog
    if (showAddRequirementDialog) {
        AddRequirementDialog(
            projectId = project.id,
            onDismiss = { showAddRequirementDialog = false },
            onSave = { req ->
                viewModel.addRequirement(req)
                showAddRequirementDialog = false
            }
        )
    }

    // Allocate Supply Dialog
    if (requirementToAllocate != null) {
        AllocateSupplyDialog(
            requirement = requirementToAllocate!!,
            projectId = project.id,
            allSupplies = allSupplies,
            onDismiss = { requirementToAllocate = null },
            onConfirm = { batchId, qty, unit, matchStatus, notes ->
                viewModel.allocateBatch(
                    projectId = project.id,
                    requirementId = requirementToAllocate!!.id,
                    batchId = batchId,
                    reservedQuantity = qty,
                    unit = unit,
                    matchStatus = matchStatus,
                    notes = notes
                )
                requirementToAllocate = null
            }
        )
    }

    // Complete Project Dialog
    if (showCompletionDialog) {
        CompleteProjectDialog(
            project = project,
            allocations = item.allocations,
            allSupplies = allSupplies,
            onDismiss = { showCompletionDialog = false },
            onConfirm = { recordUsageList ->
                viewModel.updateProjectStatus(project, ProjectStatus.COMPLETED)
                for (usage in recordUsageList) {
                    viewModel.recordConsumption(
                        projectId = project.id,
                        batchId = usage.batchId,
                        quantityUsed = usage.quantityUsed,
                        unit = usage.unit,
                        notes = "Completed project: ${project.title}",
                        deductFromBatch = usage.deduct
                    )
                }
                showCompletionDialog = false
            }
        )
    }

    // Delete Project Confirm Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Project?") },
            text = { Text("Are you sure you want to delete \"${project.title}\"? Stash supplies and batches will not be deleted; their reservations will be released.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project)
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
fun RequirementCard(
    requirement: ProjectRequirementEntity,
    coverageState: CoverageState,
    allocations: List<ProjectAllocationEntity>,
    allSupplies: List<SupplyWithBatches>,
    onAllocateClick: () -> Unit,
    onRemoveAllocation: (String) -> Unit,
    onDeleteRequirement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = requirement.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                CoverageBadge(state = coverageState)
            }

            // Specs
            val qtyStr = if (requirement.quantityRequired != null) "${UnitConverter.formatQuantity(requirement.quantityRequired)} ${requirement.unit.orEmpty()}".trim() else ""
            if (qtyStr.isNotBlank() || requirement.isReusableTool) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (qtyStr.isNotBlank()) {
                        CraftTagChip("Needed: $qtyStr")
                    }
                    if (requirement.isReusableTool) {
                        CraftTagChip("Reusable Tool (Not Consumed)")
                    }
                    if (requirement.singleContinuousPieceRequired) {
                        CraftTagChip("Single Continuous Piece")
                    }
                }
            }

            // Linked stash allocations
            if (allocations.isNotEmpty()) {
                Text("Linked Stash Supplies:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                allocations.forEach { alloc ->
                    val supplyItem = allSupplies.find { s -> s.batches.any { it.id == alloc.batchId } }
                    val batch = supplyItem?.batches?.find { it.id == alloc.batchId }
                    val supplyName = supplyItem?.supply?.name ?: "Unknown Supply"
                    val batchIdent = batch?.batchIdentifier?.ifBlank { "Main Piece" } ?: "Piece"
                    val resQtyText = if (alloc.reservedQuantity != null) "${UnitConverter.formatQuantity(alloc.reservedQuantity)} ${alloc.reservedUnit.orEmpty()}".trim() else "Allocated"

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$supplyName ($batchIdent)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("$resQtyText • ${alloc.matchStatus.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                            IconButton(onClick = { onRemoveAllocation(alloc.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Unlink supply", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAllocateClick) {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Link Stash Item")
                }
                IconButton(onClick = onDeleteRequirement) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete requirement", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddRequirementDialog(
    projectId: String,
    onDismiss: () -> Unit,
    onSave: (ProjectRequirementEntity) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var isTool by remember { mutableStateOf(false) }
    var singlePiece by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Project Material Requirement", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Requirement Description *") },
                    placeholder = { Text("e.g. 200g Worsted Cotton Yarn, 1m Lining Fabric") },
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
                        label = { Text("Unit (g, yd, count)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isTool, onCheckedChange = { isTool = it })
                    Text("Reusable Tool (Knitting needles, hook, rotary cutter)", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = singlePiece, onCheckedChange = { singlePiece = it })
                    Text("Requires single continuous piece (e.g. uncut fabric)", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ProjectRequirementEntity(
                            id = UUID.randomUUID().toString(),
                            projectId = projectId,
                            description = description.trim(),
                            quantityRequired = quantityStr.toDoubleOrNull(),
                            unit = unit.trim().ifBlank { null },
                            isReusableTool = isTool,
                            singleContinuousPieceRequired = singlePiece
                        )
                    )
                },
                enabled = description.isNotBlank()
            ) {
                Text("Add")
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
fun AllocateSupplyDialog(
    requirement: ProjectRequirementEntity,
    projectId: String,
    allSupplies: List<SupplyWithBatches>,
    onDismiss: () -> Unit,
    onConfirm: (batchId: String, reservedQty: Double?, unit: String?, matchStatus: MatchStatus, notes: String) -> Unit
) {
    val activeBatches = remember(allSupplies) {
        allSupplies.flatMap { s ->
            s.batches.filter { !it.isDepleted }.map { b -> Pair(s.supply, b) }
        }
    }

    var selectedBatchPair by remember { mutableStateOf<Pair<com.example.data.entity.SupplyEntity, BatchEntity>?>(activeBatches.firstOrNull()) }
    var reservedQtyStr by remember { mutableStateOf(requirement.quantityRequired?.toString().orEmpty()) }
    var reservedUnit by remember { mutableStateOf(requirement.unit.orEmpty()) }
    var matchStatus by remember { mutableStateOf(MatchStatus.EXACT_MATCH) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Stash to \"${requirement.description}\"", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    Text("Select a batch or piece from your stash:", style = MaterialTheme.typography.labelMedium)
                }

                item {
                    var batchExpanded by remember { mutableStateOf(false) }
                    val currentText = selectedBatchPair?.let { "${it.first.name} - ${it.second.batchIdentifier.ifBlank { "Main Piece" }}" } ?: "Select Batch"
                    ExposedDropdownMenuBox(
                        expanded = batchExpanded,
                        onExpandedChange = { batchExpanded = !batchExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Stash Item") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batchExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = batchExpanded,
                            onDismissRequest = { batchExpanded = false }
                        ) {
                            activeBatches.forEach { pair ->
                                val text = "${pair.first.name} - ${pair.second.batchIdentifier.ifBlank { "Piece" }} (${UnitConverter.formatDisplayQuantity(pair.second.quantityValue, pair.second.unit, pair.second.quantityMode)})"
                                DropdownMenuItem(
                                    text = { Text(text) },
                                    onClick = {
                                        selectedBatchPair = pair
                                        batchExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = reservedQtyStr,
                            onValueChange = { reservedQtyStr = it },
                            label = { Text("Reserve Qty") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = reservedUnit,
                            onValueChange = { reservedUnit = it },
                            label = { Text("Unit") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Match Assessment:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MatchStatus.values().forEach { st ->
                            FilterChip(
                                selected = matchStatus == st,
                                onClick = { matchStatus = st },
                                label = { Text(st.displayName) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Allocation Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val batchId = selectedBatchPair?.second?.id ?: return@Button
                    onConfirm(
                        batchId,
                        reservedQtyStr.toDoubleOrNull(),
                        reservedUnit.trim().ifBlank { null },
                        matchStatus,
                        notes.trim()
                    )
                },
                enabled = selectedBatchPair != null
            ) {
                Text("Link & Reserve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class UsageRecordDraft(
    val batchId: String,
    val supplyName: String,
    val batchIdentifier: String,
    var quantityUsed: Double?,
    var unit: String?,
    var deduct: Boolean
)

@Composable
fun CompleteProjectDialog(
    project: com.example.data.entity.ProjectEntity,
    allocations: List<ProjectAllocationEntity>,
    allSupplies: List<SupplyWithBatches>,
    onDismiss: () -> Unit,
    onConfirm: (List<UsageRecordDraft>) -> Unit
) {
    val usageList = remember(allocations) {
        allocations.map { a ->
            val s = allSupplies.find { item -> item.batches.any { it.id == a.batchId } }
            val b = s?.batches?.find { it.id == a.batchId }
            UsageRecordDraft(
                batchId = a.batchId,
                supplyName = s?.supply?.name ?: "Supply",
                batchIdentifier = b?.batchIdentifier?.ifBlank { "Piece" } ?: "Piece",
                quantityUsed = a.reservedQuantity ?: b?.quantityValue,
                unit = a.reservedUnit ?: b?.unit,
                deduct = true
            )
        }.toMutableStateList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Project & Log Usage", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        "Congratulations! Would you like to record confirmed usage from your stash? You can adjust the amounts or keep leftovers.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                items(usageList) { usage ->
                    var qtyText by remember { mutableStateOf(usage.quantityUsed?.toString().orEmpty()) }
                    var deductChecked by remember { mutableStateOf(usage.deduct) }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${usage.supplyName} (${usage.batchIdentifier})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = qtyText,
                                    onValueChange = {
                                        qtyText = it
                                        usage.quantityUsed = it.toDoubleOrNull()
                                    },
                                    label = { Text("Amount Used") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(usage.unit.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = deductChecked,
                                    onCheckedChange = {
                                        deductChecked = it
                                        usage.deduct = it
                                    }
                                )
                                Text("Deduct from physical on-hand quantity", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(usageList) }) {
                Text("Confirm & Complete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
