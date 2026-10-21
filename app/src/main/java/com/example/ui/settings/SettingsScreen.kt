package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.domain.backup.BackupValidationResult
import com.example.domain.model.ThemeMode
import com.example.ui.CraftStashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CraftStashViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val currentThemeMode by viewModel.themeMode.collectAsState()

    var validationResult by remember { mutableStateOf<BackupValidationResult?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showSampleDataConfirmDialog by remember { mutableStateOf(false) }
    var showClearSampleDataConfirmDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Backup ZIP exporter launcher
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createBackup(uri) { success, msg ->
                viewModel.showMessage(msg)
            }
        }
    }

    // CSV exporter launcher
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportCsv(uri) { success, msg ->
                viewModel.showMessage(msg)
            }
        }
    }

    // Restore ZIP picker launcher
    val openRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            viewModel.validateBackup(uri) { result ->
                validationResult = result
                showRestoreConfirmDialog = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings & Data") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance & Theme Section
            item {
                Text(
                    text = "Appearance & Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("theme_settings_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (currentThemeMode) {
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Display Theme",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Choose whether Craft Stash follows your system appearance or stays in warm Light or cozy Dark mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.values().forEach { mode ->
                                val isSelected = currentThemeMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(mode.displayName) },
                                    leadingIcon = {
                                        val icon = when (mode) {
                                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                            ThemeMode.LIGHT -> Icons.Default.LightMode
                                            ThemeMode.DARK -> Icons.Default.DarkMode
                                        }
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Data Backup & Export Section
            item {
                Text(
                    text = "Backup, Export & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Full Portable Backup (ZIP)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Exports a complete copy of your supplies, batches, photos, storage locations, projects, requirements, allocations, and consumption history into an archive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    createBackupLauncher.launch("craft_stash_backup_${System.currentTimeMillis()}.zip")
                                },
                                modifier = Modifier.weight(1f).testTag("export_backup_button")
                            ) {
                                Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Backup")
                            }

                            OutlinedButton(
                                onClick = {
                                    openRestoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                },
                                modifier = Modifier.weight(1f).testTag("restore_backup_button")
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Backup")
                            }
                        }
                    }
                }
            }

            // CSV Inventory Export
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Export Inventory to CSV Spreadsheet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Creates a spreadsheet file containing all supplies and individual batches, quantities, locations, and dimensions. Note: CSV is not a full database backup and does not include photos or project links.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                exportCsvLauncher.launch("craft_stash_inventory_${System.currentTimeMillis()}.csv")
                            },
                            modifier = Modifier.fillMaxWidth().testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.TableChart, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Inventory to CSV")
                        }
                    }
                }
            }

            // Sample Data Section
            item {
                Text(
                    text = "Sample Collection Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Explore with Sample Craft Supplies",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Load an optional starter stash (worsted cotton yarn, botanical floral fabric, circular bamboo knitting needles, olive wood buttons, and a market tote bag project) to preview how Craft Stash organizes supplies and matches requirements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showSampleDataConfirmDialog = true },
                                modifier = Modifier.weight(1f).testTag("load_sample_data_button")
                            ) {
                                Text("Load Sample Data")
                            }
                            OutlinedButton(
                                onClick = { showClearSampleDataConfirmDialog = true },
                                modifier = Modifier.weight(1f).testTag("clear_sample_data_button")
                            ) {
                                Text("Clear Sample Data")
                            }
                        }
                    }
                }
            }

            // Privacy & Architecture
            item {
                Text(
                    text = "Privacy & F-Droid Commitment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("100% Offline & Private", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "• All supplies, batches, photos, locations, and projects are stored exclusively on your device in a local Room database.\n• No tracking, analytics, telemetry, or remote servers.\n• Barcode scanning runs 100% offline via FOSS ZXing.\n• Web searches and inspiration sites open in your external browser only upon explicit request.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showPrivacyDialog = true }) {
                            Text("View Complete Privacy Statement")
                        }
                    }
                }
            }

            // About / Version info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Craft Stash v1.0",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Licensed under Apache License 2.0 • Built for Hobby Crafters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // Restore Confirmation Dialog with Pre-validation Preview
    if (showRestoreConfirmDialog && validationResult != null) {
        val res = validationResult!!
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            title = {
                Text(
                    if (res.isValid) "Restore Backup Collection?" else "Invalid Backup Archive",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (res.isValid) {
                        Text("Archive validated successfully (Format v${res.version}):", style = MaterialTheme.typography.bodySmall)
                        Text("• ${res.supplyCount} supplies", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("• ${res.batchCount} batches / physical pieces", style = MaterialTheme.typography.bodyMedium)
                        Text("• ${res.projectCount} projects", style = MaterialTheme.typography.bodyMedium)
                        Text("• ${res.locationCount} storage locations", style = MaterialTheme.typography.bodyMedium)
                        Text("• ${res.photoCount} photos", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "WARNING: Restoring will replace your current collection with the contents of this archive. Make sure you have exported a safety backup of your existing data first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "The selected file could not be validated as a Craft Stash backup:\n${res.errorMessage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (res.isValid) {
                    Button(
                        onClick = {
                            showRestoreConfirmDialog = false
                            pendingRestoreUri?.let { uri ->
                                viewModel.restoreBackup(uri) { success, msg ->
                                    viewModel.showMessage(msg)
                                }
                            }
                            pendingRestoreUri = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm Replace & Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingRestoreUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Load Sample Data Dialog
    if (showSampleDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSampleDataConfirmDialog = false },
            title = { Text("Load Sample Craft Stash?") },
            text = { Text("This will add sample cotton yarn, botanical fabric, knitting needles, buttons, and a tote bag project to your collection. You can clear them anytime.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.loadSampleData()
                    showSampleDataConfirmDialog = false
                }) {
                    Text("Load Sample Stash")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSampleDataConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Sample Data Dialog
    if (showClearSampleDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearSampleDataConfirmDialog = false },
            title = { Text("Clear Sample Data?") },
            text = { Text("This will remove all demo sample records, leaving your own custom supplies and projects untouched.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearSampleData()
                        showClearSampleDataConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Demo Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSampleDataConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Statement Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Data Ownership", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1. Local Storage Only: All items, quantities, measurements, notes, and photos reside solely within the application's private sandbox on your device.\n\n" +
                                "2. Zero Tracking: Craft Stash contains no analytics, telemetry, user tracking, or advertising SDKs.\n\n" +
                                "3. External Searches: When you choose to search inspiration on sites like Pinterest, Ravelry, or DuckDuckGo, Craft Stash hands the query off to your device's external browser. No private notes, locations, or inventory lists are sent.\n\n" +
                                "4. Exported Backups: When you export a ZIP backup, the file contains your inventory database and photos. Keep your backup files secure.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
