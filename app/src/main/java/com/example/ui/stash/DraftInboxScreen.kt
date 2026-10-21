package com.example.ui.stash

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.dao.SupplyWithBatches
import com.example.data.entity.BatchEntity
import com.example.data.entity.SupplyEntity
import com.example.data.photo.PhotoManager
import com.example.domain.model.QuantityMode
import com.example.ui.CraftStashViewModel
import com.example.ui.common.CraftPhotoThumbnail
import com.example.ui.common.EmptyStateView
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftInboxScreen(
    viewModel: CraftStashViewModel,
    onNavigateBack: () -> Unit,
    onOrganizeDraft: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drafts by viewModel.drafts.collectAsState()
    val locations by viewModel.locations.collectAsState()

    var organizingDraft by remember { mutableStateOf<SupplyEntity?>(null) }

    // Multi-photo picker for rapid draft intake
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                for (uri in uris) {
                    val saved = PhotoManager.savePhotoFromUri(context, uri)
                    if (saved != null) {
                        viewModel.addDraftPhoto(saved)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Draft Inbox (Capture Now)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("capture_photos_button")
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Photos")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Snap or select multiple photos of your supplies now. You can organize, name, and categorize them into your stash whenever you are ready.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (drafts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.CameraAlt,
                    title = "No Pending Drafts",
                    description = "Take photos of your craft room or new hauls to quickly drop them here.",
                    actionLabel = "Import Photos to Drafts",
                    onActionClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(drafts, key = { it.supply.id }) { draftItem ->
                        DraftCard(
                            item = draftItem,
                            onOrganize = { organizingDraft = draftItem.supply },
                            onDelete = { viewModel.deleteDraft(draftItem.supply.id) }
                        )
                    }
                }
            }
        }
    }

    if (organizingDraft != null) {
        OrganizeDraftDialog(
            draft = organizingDraft!!,
            locations = locations,
            onDismiss = { organizingDraft = null },
            onConfirm = { name, category, locationId ->
                val draft = organizingDraft!!
                val updatedSupply = draft.copy(
                    name = name.trim(),
                    category = category,
                    isDraft = false
                )
                val batch = BatchEntity(
                    id = UUID.randomUUID().toString(),
                    supplyId = draft.id,
                    batchIdentifier = "Main Piece",
                    quantityMode = QuantityMode.PRESENCE_ONLY,
                    storageLocationId = locationId
                )
                viewModel.saveDetailedSupply(updatedSupply, batch)
                organizingDraft = null
            }
        )
    }
}

@Composable
fun DraftCard(
    item: SupplyWithBatches,
    onOrganize: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
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

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete draft",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Completeness Cue Chip
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Needs name & details",
                        color = Color(0xFFE65100),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Button(
                    onClick = onOrganize,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("Organize", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeDraftDialog(
    draft: SupplyEntity,
    locations: List<com.example.data.entity.StorageLocationEntity>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, locationId: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Yarn") }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Yarn", "Fabric", "Paint & Liquid Media", "Beads & Jewelry", "Paper & Vinyl", "Tools", "Ribbon & Trim", "General Craft")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move Draft to Stash", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Give this supply a name to move it from drafts into your main inventory.", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supply Name *") },
                    placeholder = { Text("e.g. Indigo Dyed Linen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; catExpanded = false }
                            )
                        }
                    }
                }

                if (locations.isNotEmpty()) {
                    var locExpanded by remember { mutableStateOf(false) }
                    val currentLocName = locations.find { it.id == selectedLocationId }?.name ?: "None"
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
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category, selectedLocationId) },
                enabled = name.isNotBlank()
            ) {
                Text("Move to Stash")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
