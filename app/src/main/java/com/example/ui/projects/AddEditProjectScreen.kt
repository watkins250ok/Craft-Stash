package com.example.ui.projects

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.ProjectEntity
import com.example.data.photo.PhotoManager
import com.example.domain.model.ProjectStatus
import com.example.ui.CraftStashViewModel
import com.example.ui.common.CraftPhotoThumbnail
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProjectScreen(
    projectId: String?,
    viewModel: CraftStashViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var existingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var craftType by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ProjectStatus.PLANNED) }
    var sourceUrl by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("") }
    var finishedMeasurements by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var coverPhotoUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        if (!projectId.isNullOrBlank()) {
            val p = viewModel.repository.getProjectFlow(projectId)
            // collect single
            viewModel.allProjects.value.find { it.project.id == projectId }?.let { item ->
                existingProject = item.project
                title = item.project.title
                craftType = item.project.craftType
                status = item.project.status
                sourceUrl = item.project.sourceUrl.orEmpty()
                sourceName = item.project.sourceName.orEmpty()
                finishedMeasurements = item.project.finishedMeasurements.orEmpty()
                notes = item.project.notes
                coverPhotoUri = item.project.coverPhotoUri
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val saved = PhotoManager.savePhotoFromUri(context, uri)
                if (saved != null) {
                    coverPhotoUri = saved
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (projectId == null) "New Project" else "Edit Project") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val id = existingProject?.id ?: UUID.randomUUID().toString()
                            val proj = (existingProject ?: ProjectEntity(id = id, title = title)).copy(
                                title = title.trim(),
                                craftType = craftType.trim(),
                                status = status,
                                sourceUrl = sourceUrl.trim().ifBlank { null },
                                sourceName = sourceName.trim().ifBlank { null },
                                finishedMeasurements = finishedMeasurements.trim().ifBlank { null },
                                notes = notes.trim(),
                                coverPhotoUri = coverPhotoUri
                            )
                            viewModel.saveProject(proj, emptyList())
                            onNavigateBack()
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("save_project_button")
                    ) {
                        Text("Save")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Photo picker
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CraftPhotoThumbnail(photoUri = coverPhotoUri, modifier = Modifier.size(80.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (coverPhotoUri == null) "Add Cover Photo" else "Change Photo")
                        }
                        if (coverPhotoUri != null) {
                            TextButton(onClick = { coverPhotoUri = null }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title *") },
                    placeholder = { Text("e.g. Honeycomb Quilted Blanket, Cable Knit Beanie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("project_title_input")
                )
            }

            item {
                OutlinedTextField(
                    value = craftType,
                    onValueChange = { craftType = it },
                    label = { Text("Craft Type") },
                    placeholder = { Text("e.g. Quilting, Knitting, Crochet, Embroidery") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Initial Status", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(ProjectStatus.IDEA, ProjectStatus.PLANNED, ProjectStatus.IN_PROGRESS).forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sourceName,
                        onValueChange = { sourceName = it },
                        label = { Text("Pattern / Designer") },
                        placeholder = { Text("e.g. PetiteKnit, Grainline Studio") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        label = { Text("Source Web Link") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = finishedMeasurements,
                    onValueChange = { finishedMeasurements = it },
                    label = { Text("Finished Measurements (Optional)") },
                    placeholder = { Text("e.g. 50 in x 65 in") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Project Notes") },
                    placeholder = { Text("Gauge, adjustments, ideas, gift recipient...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }
        }
    }
}
