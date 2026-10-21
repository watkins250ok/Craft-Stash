package com.example.ui.stash

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.BatchEntity
import com.example.data.entity.StorageLocationEntity
import com.example.data.entity.SupplyEntity
import com.example.data.photo.PhotoManager
import com.example.domain.model.QuantityMode
import com.example.ui.CraftStashViewModel
import com.example.ui.common.CraftPhotoThumbnail
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSupplyScreen(
    supplyId: String?,
    prefilledBarcode: String? = null,
    viewModel: CraftStashViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locations by viewModel.locations.collectAsState()

    var existingSupply by remember { mutableStateOf<SupplyEntity?>(null) }
    var existingBatch by remember { mutableStateOf<BatchEntity?>(null) }
    var isLoading by remember { mutableStateOf(supplyId != null) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Yarn") }
    var craftTypes by remember { mutableStateOf("") }
    var material by remember { mutableStateOf("") }
    var colorFamilies by remember { mutableStateOf("") }
    var manufacturerColor by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf(prefilledBarcode.orEmpty()) }
    var notes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }

    // Initial batch fields
    var batchIdentifier by remember { mutableStateOf("Main Piece") }
    var quantityMode by remember { mutableStateOf(QuantityMode.EXACT) }
    var quantityStr by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    var widthStr by remember { mutableStateOf("") }
    var lengthStr by remember { mutableStateOf("") }
    var dimUnit by remember { mutableStateOf("inches") }
    var condition by remember { mutableStateOf("Sealed") }

    // Category Template specifics
    var yarnWeight by remember { mutableStateOf("") }
    var fabricStretch by remember { mutableStateOf("") }
    var paintMedium by remember { mutableStateOf("") }
    var beadShape by remember { mutableStateOf("") }
    var beadHoleSize by remember { mutableStateOf("") }

    // Load existing supply if editing
    LaunchedEffect(supplyId) {
        if (!supplyId.isNullOrBlank()) {
            val s = viewModel.repository.getSupplyById(supplyId)
            if (s != null) {
                existingSupply = s.supply
                name = s.supply.name
                category = s.supply.category
                craftTypes = s.supply.craftTypes
                material = s.supply.material
                colorFamilies = s.supply.colorFamilies
                manufacturerColor = s.supply.manufacturerColor
                appearance = s.supply.appearance
                brand = s.supply.brand
                barcode = s.supply.barcode
                notes = s.supply.notes
                tags = s.supply.tags
                photoUri = s.supply.coverPhotoUri

                val firstBatch = s.batches.firstOrNull()
                if (firstBatch != null) {
                    existingBatch = firstBatch
                    batchIdentifier = firstBatch.batchIdentifier
                    quantityMode = firstBatch.quantityMode
                    quantityStr = firstBatch.quantityValue?.toString().orEmpty()
                    unit = firstBatch.unit.orEmpty()
                    selectedLocationId = firstBatch.storageLocationId
                    widthStr = firstBatch.dimensionWidth?.toString().orEmpty()
                    lengthStr = firstBatch.dimensionLength?.toString().orEmpty()
                    dimUnit = firstBatch.dimensionUnit ?: "inches"
                    condition = firstBatch.condition
                }
            }
            isLoading = false
        }
    }

    // Photo picker launcher (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = PhotoManager.savePhotoFromUri(context, uri)
                if (savedPath != null) {
                    photoUri = savedPath
                }
            }
        }
    }

    val categories = listOf("Yarn", "Fabric", "Paint & Liquid Media", "Beads & Jewelry", "Paper & Vinyl", "Tools", "Ribbon & Trim", "General Craft")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(if (supplyId == null) "Add Craft Supply" else "Edit Supply") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val id = existingSupply?.id ?: UUID.randomUUID().toString()
                            val supply = (existingSupply ?: SupplyEntity(id = id, name = name, category = category, craftTypes = craftTypes)).copy(
                                name = name.trim(),
                                category = category,
                                craftTypes = craftTypes.trim(),
                                material = material.trim(),
                                colorFamilies = colorFamilies.trim(),
                                manufacturerColor = manufacturerColor.trim(),
                                appearance = appearance.trim(),
                                brand = brand.trim(),
                                barcode = barcode.trim(),
                                notes = notes.trim(),
                                tags = tags.trim(),
                                coverPhotoUri = photoUri,
                                isDraft = false
                            )

                            val batch = (existingBatch ?: BatchEntity(
                                id = UUID.randomUUID().toString(),
                                supplyId = id
                            )).copy(
                                batchIdentifier = batchIdentifier.trim(),
                                quantityMode = quantityMode,
                                quantityValue = quantityStr.toDoubleOrNull(),
                                unit = unit.trim().ifBlank { null },
                                dimensionWidth = widthStr.toDoubleOrNull(),
                                dimensionLength = lengthStr.toDoubleOrNull(),
                                dimensionUnit = dimUnit.trim().ifBlank { null },
                                storageLocationId = selectedLocationId,
                                condition = condition.trim()
                            )

                            viewModel.saveDetailedSupply(supply, batch)
                            onNavigateBack()
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("save_supply_button")
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CraftPhotoThumbnail(
                        photoUri = photoUri,
                        modifier = Modifier.size(90.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("choose_photo_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (photoUri == null) "Add Photo" else "Change Photo")
                        }
                        if (photoUri != null) {
                            TextButton(onClick = { photoUri = null }) {
                                Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Core Identity
            item {
                SectionHeader("Basic Details")
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supply Name *") },
                    placeholder = { Text("e.g. Malabrigo Rios Merino Yarn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supply_name_input")
                )
            }

            // Category Dropdown
            item {
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
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
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = craftTypes,
                    onValueChange = { craftTypes = it },
                    label = { Text("Craft Types (e.g. Knitting, Crochet, Sewing)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand / Manufacturer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Progressive Fields
            when (category) {
                "Yarn" -> {
                    item { SectionHeader("Yarn Specifics") }
                    item {
                        OutlinedTextField(
                            value = yarnWeight,
                            onValueChange = { yarnWeight = it },
                            label = { Text("Yarn Weight (Worsted, Fingering, DK, Bulky)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                "Fabric" -> {
                    item { SectionHeader("Fabric Specifics") }
                    item {
                        OutlinedTextField(
                            value = fabricStretch,
                            onValueChange = { fabricStretch = it },
                            label = { Text("Stretch / Weave (Woven, 2-way knit, 4-way knit)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                "Paint & Liquid Media" -> {
                    item { SectionHeader("Paint Specifics") }
                    item {
                        OutlinedTextField(
                            value = paintMedium,
                            onValueChange = { paintMedium = it },
                            label = { Text("Medium (Acrylic, Watercolor, Gouache, Oil)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                "Beads & Jewelry" -> {
                    item { SectionHeader("Beads Specifics") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = beadShape,
                                onValueChange = { beadShape = it },
                                label = { Text("Shape (Round, Bicone, Rondelle)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = beadHoleSize,
                                onValueChange = { beadHoleSize = it },
                                label = { Text("Hole Size (e.g. 1mm)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Material, Color, Appearance
            item {
                SectionHeader("Material & Appearance")
            }

            item {
                OutlinedTextField(
                    value = material,
                    onValueChange = { material = it },
                    label = { Text("Material / Fiber (e.g. 100% Cotton, Merino)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = colorFamilies,
                        onValueChange = { colorFamilies = it },
                        label = { Text("Color Family") },
                        placeholder = { Text("Teal, Blue") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = manufacturerColor,
                        onValueChange = { manufacturerColor = it },
                        label = { Text("Color Name / Code") },
                        placeholder = { Text("Indigo #402") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = appearance,
                    onValueChange = { appearance = it },
                    label = { Text("Appearance (Solid, Patterned, Variegated, Metallic)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode (UPC/EAN text preserving zeros)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Initial Batch / Physical Piece
            item {
                SectionHeader("Physical Piece / Quantity")
            }

            item {
                OutlinedTextField(
                    value = batchIdentifier,
                    onValueChange = { batchIdentifier = it },
                    label = { Text("Batch / Piece Identifier (e.g. Dye lot 402, Piece A)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Tracking Precision Mode", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            label = { Text("Unit (g, yd, skein)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Cut piece dimensions
            item {
                Text("Dimensions (Optional - for fabrics, papers, rolls)", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Location
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

            item {
                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("Condition (Sealed, Opened, Remnants, Scraps)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma-separated, e.g. summer, floral, cotton)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}
