package com.example.ui.ideas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.entity.InspirationSourceEntity
import com.example.domain.inspiration.InspirationEngine
import com.example.domain.inspiration.InspirationPrompt
import com.example.domain.matching.ProjectMatchResult
import com.example.ui.CraftStashViewModel
import com.example.ui.common.CraftPhotoThumbnail
import com.example.ui.common.CraftTagChip
import com.example.ui.common.EmptyStateView
import com.example.ui.common.ProjectStatusBadge
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasScreen(
    viewModel: CraftStashViewModel,
    onNavigateToProjectDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val matchedProjects by viewModel.matchedProjects.collectAsState()
    val allSupplies by viewModel.allSupplies.collectAsState()
    val selectedSupplyIds by viewModel.selectedSupplyIds.collectAsState()
    val matchAllSelected by viewModel.matchAllSelected.collectAsState()
    val sources by viewModel.inspirationSources.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0 = Saved Projects Match, 1 = Craft Starters & Search, 2 = Source Directory
    var selectedPromptForSearch by remember { mutableStateOf<InspirationPrompt?>(null) }
    var editableSearchQuery by remember { mutableStateOf("") }
    var showAddSourceDialog by remember { mutableStateOf(false) }

    val selectedSuppliesList = remember(selectedSupplyIds, allSupplies) {
        selectedSupplyIds.mapNotNull { id -> allSupplies.find { it.supply.id == id } }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ideas & Matching", fontWeight = FontWeight.Bold) },
                actions = {
                    if (selectedSupplyIds.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { viewModel.clearSelection() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Clear ${selectedSupplyIds.size} Selected")
                        }
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
            // Selected Stash Banner (if any)
            if (selectedSuppliesList.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Finding ideas for ${selectedSuppliesList.size} selected supply:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(selectedSuppliesList) { s ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CraftPhotoThumbnail(photoUri = s.supply.coverPhotoUri, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(s.supply.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // Match Mode: Any vs All
                        if (selectedSuppliesList.size > 1) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = !matchAllSelected,
                                    onClick = { viewModel.setMatchAllSelected(false) },
                                    label = { Text("Use ANY selected supply") }
                                )
                                FilterChip(
                                    selected = matchAllSelected,
                                    onClick = { viewModel.setMatchAllSelected(true) },
                                    label = { Text("Try to use ALL selected") }
                                )
                            }
                        }
                    }
                }
            }

            // Sub-navigation tabs
            TabRow(selectedTabIndex = activeSubTab) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = { Text("Saved Projects (${matchedProjects.size})") }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = { Text("Craft Starters") }
                )
                Tab(
                    selected = activeSubTab == 2,
                    onClick = { activeSubTab = 2 },
                    text = { Text("Inspiration Directory") }
                )
            }

            // Content per Sub-Tab
            when (activeSubTab) {
                0 -> {
                    // TAB 0: Saved Projects Matching
                    if (matchedProjects.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No Matching Saved Projects",
                            description = if (selectedSupplyIds.isNotEmpty())
                                "None of your current saved projects require these specific selected supplies. Try the 'Craft Starters' tab for idea prompts!"
                            else "No active saved projects in your collection yet.",
                            actionLabel = "Browse Craft Starters",
                            onActionClick = { activeSubTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = if (selectedSupplyIds.isNotEmpty())
                                        "Saved projects that can use your selected stash:"
                                    else "All active saved projects ranked by material readiness:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            items(matchedProjects, key = { it.projectWithDetails.project.id }) { match ->
                                MatchedProjectCard(
                                    match = match,
                                    onClick = { onNavigateToProjectDetail(match.projectWithDetails.project.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: Local Rule-Based Inspiration Starters
                    val prompts = remember(selectedSuppliesList) {
                        InspirationEngine.findPrompts(selectedSuppliesList)
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Locally generated idea starters connecting your stash materials (scraps, remnants, skeins) to project possibilities without AI. Never leaks personal notes or location.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        items(prompts) { prompt ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(prompt.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        CraftTagChip(prompt.craftType)
                                    }

                                    Text(prompt.description, style = MaterialTheme.typography.bodyMedium)

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                        if (prompt.isBeginnerFriendly) CraftTagChip("Beginner")
                                        if (prompt.isScrapFriendly) CraftTagChip("Scrap / Remnant Friendly")
                                        if (!prompt.requiresMachine) CraftTagChip("No Machine Needed")
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                selectedPromptForSearch = prompt
                                                editableSearchQuery = InspirationEngine.generateSearchQuery(prompt, selectedSuppliesList)
                                            }
                                        ) {
                                            Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Search Ideas")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: Inspiration Source Directory
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Inspiration Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row {
                                    TextButton(onClick = { viewModel.restoreDefaultSources() }) {
                                        Text("Restore Defaults")
                                    }
                                    IconButton(onClick = { showAddSourceDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add custom destination")
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Craft Stash does not embed proprietary web scrapers or require logins. You can search these destinations directly in your external browser.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        items(sources, key = { it.id }) { src ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(src.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = src.isEnabled,
                                            onCheckedChange = { viewModel.toggleSourceEnabled(src) }
                                        )
                                    }

                                    Text(src.description, style = MaterialTheme.typography.bodySmall)

                                    if (src.craftTags.isNotBlank()) {
                                        CraftTagChip(src.craftTags)
                                    }

                                    if (src.accountPaymentNote.isNotBlank()) {
                                        Text(
                                            text = "Note: ${src.accountPaymentNote}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(src.baseUrl))
                                                    context.startActivity(browserIntent)
                                                } catch (_: Exception) {}
                                            }
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open Site")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Search Query Generator Dialog
    if (selectedPromptForSearch != null) {
        AlertDialog(
            onDismissRequest = { selectedPromptForSearch = null },
            title = { Text("Search Inspiration", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "You can edit the search phrase before copying or launching in your browser. Craft Stash will never transmit private notes or locations.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = editableSearchQuery,
                        onValueChange = { editableSearchQuery = it },
                        label = { Text("Search Query") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Open destination:", style = MaterialTheme.typography.labelMedium)
                    sources.filter { it.isEnabled }.forEach { src ->
                        OutlinedButton(
                            onClick = {
                                val encoded = URLEncoder.encode(editableSearchQuery, "UTF-8")
                                val targetUrl = if (!src.searchTemplate.isNullOrBlank()) {
                                    src.searchTemplate.replace("%s", encoded)
                                } else {
                                    src.baseUrl
                                }

                                // Also copy query to clipboard for convenience
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Craft Search", editableSearchQuery))

                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}

                                selectedPromptForSearch = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open in ${src.name}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Craft Search", editableSearchQuery))
                        viewModel.showMessage("Query copied to clipboard!")
                        selectedPromptForSearch = null
                    }
                ) {
                    Text("Copy Query")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPromptForSearch = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Custom Destination Dialog
    if (showAddSourceDialog) {
        var srcName by remember { mutableStateOf("") }
        var srcUrl by remember { mutableStateOf("https://") }
        var srcTemplate by remember { mutableStateOf("") }
        var srcTags by remember { mutableStateOf("All") }
        var srcDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSourceDialog = false },
            title = { Text("Add Custom Destination", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = srcName, onValueChange = { srcName = it }, label = { Text("Site Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = srcUrl, onValueChange = { srcUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = srcTemplate, onValueChange = { srcTemplate = it }, label = { Text("Search URL Template (with %s)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = srcTags, onValueChange = { srcTags = it }, label = { Text("Craft Tags") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = srcDesc, onValueChange = { srcDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addInspirationSource(srcName.trim(), srcUrl.trim(), srcTemplate.trim().ifBlank { null }, srcTags.trim(), srcDesc.trim())
                        showAddSourceDialog = false
                    },
                    enabled = srcName.isNotBlank() && srcUrl.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MatchedProjectCard(
    match: ProjectMatchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project = match.projectWithDetails.project

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (match.isReadyToMake) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (match.isReadyToMake) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                ProjectStatusBadge(status = project.status)
            }

            if (project.craftType.isNotBlank()) {
                CraftTagChip(project.craftType)
            }

            // Explanations list
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                match.matchedExplanations.take(4).forEach { expl ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (expl.startsWith("Confirmed")) Icons.Default.CheckCircle
                            else if (expl.startsWith("Missing")) Icons.Default.Cancel
                            else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (expl.startsWith("Confirmed")) Color(0xFF2E7D32)
                            else if (expl.startsWith("Missing")) Color(0xFFC62828)
                            else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(expl, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Open Project →",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
