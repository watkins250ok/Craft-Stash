package com.example.ui.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.dao.ProjectWithDetails
import com.example.domain.matching.ProjectMatcher
import com.example.domain.model.ProjectStatus
import com.example.ui.CraftStashViewModel
import com.example.ui.common.CraftPhotoThumbnail
import com.example.ui.common.CraftTagChip
import com.example.ui.common.EmptyStateView
import com.example.ui.common.ProjectStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: CraftStashViewModel,
    onNavigateToProjectDetail: (String) -> Unit,
    onNavigateToAddProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val allSupplies by viewModel.allSupplies.collectAsState()
    val allAllocations by viewModel.allAllocations.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf<ProjectStatus?>(null) }
    var showArchived by remember { mutableStateOf(false) }

    val filteredProjects = remember(projects, selectedStatusFilter, showArchived) {
        projects.filter { p ->
            if (!showArchived && p.project.status == ProjectStatus.ARCHIVED) return@filter false
            if (selectedStatusFilter != null) {
                p.project.status == selectedStatusFilter
            } else true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Craft Projects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = !showArchived },
                        label = { Text("Archived") },
                        leadingIcon = if (showArchived) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProject,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add project")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status filters row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All Active") }
                    )
                }
                items(ProjectStatus.values().filter { it != ProjectStatus.ARCHIVED }) { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == status) null else status
                        },
                        label = { Text(status.displayName) }
                    )
                }
            }

            if (filteredProjects.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.DesignServices,
                    title = "No Projects Yet",
                    description = "Plan your sewing garments, knit shawls, crochet bags, and craft ideas. Match them with supplies you already have in stash!",
                    actionLabel = "Create Project",
                    onActionClick = onNavigateToAddProject,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProjects, key = { it.project.id }) { projectWithDetails ->
                        val eval = remember(projectWithDetails, allSupplies, allAllocations) {
                            ProjectMatcher.evaluateProject(projectWithDetails, allSupplies, allAllocations)
                        }

                        ProjectCard(
                            projectWithDetails = projectWithDetails,
                            coverageSummary = eval.coverageSummary,
                            isReadyToMake = eval.isReadyToMake,
                            onClick = { onNavigateToProjectDetail(projectWithDetails.project.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    projectWithDetails: ProjectWithDetails,
    coverageSummary: String,
    isReadyToMake: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project = projectWithDetails.project

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}")
    ) {
        Column {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CraftPhotoThumbnail(
                    photoUri = project.coverPhotoUri,
                    fallbackIcon = Icons.Default.DesignServices,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        ProjectStatusBadge(status = project.status)
                    }

                    if (project.craftType.isNotBlank()) {
                        CraftTagChip(project.craftType)
                    }

                    if (!project.sourceName.isNullOrBlank()) {
                        Text(
                            text = "Source: ${project.sourceName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Material coverage banner
            Surface(
                color = if (isReadyToMake) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isReadyToMake) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isReadyToMake) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = coverageSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isReadyToMake) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
