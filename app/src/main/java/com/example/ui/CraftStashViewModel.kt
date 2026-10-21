package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.ProjectWithDetails
import com.example.data.dao.SupplyWithBatches
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.photo.PhotoManager
import com.example.data.repository.CraftStashRepository
import com.example.domain.backup.BackupManager
import com.example.domain.backup.BackupValidationResult
import com.example.domain.backup.CsvExporter
import com.example.domain.inspiration.InspirationEngine
import com.example.domain.inspiration.InspirationPrompt
import com.example.domain.matching.ProjectMatchResult
import com.example.domain.matching.ProjectMatcher
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class StashFilterState(
    val query: String = "",
    val selectedCategory: String? = null,
    val selectedCraftType: String? = null,
    val selectedLocationId: String? = null,
    val selectedColorFamily: String? = null,
    val favoritesOnly: Boolean = false,
    val availableOnly: Boolean = false,
    val sortOrder: SortOrder = SortOrder.UPDATED_DESC,
    val viewMode: StashViewMode = StashViewMode.GRID
)

class CraftStashViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = CraftStashRepository(database, application)

    // Theme Mode: SYSTEM, LIGHT, DARK
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            val savedTheme = repository.getSetting("theme_mode")
            if (savedTheme != null) {
                try {
                    _themeMode.value = ThemeMode.valueOf(savedTheme)
                } catch (_: Exception) {}
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            repository.setSetting("theme_mode", mode.name)
        }
    }

    // Filter and Search State
    private val _filterState = MutableStateFlow(StashFilterState())
    val filterState: StateFlow<StashFilterState> = _filterState.asStateFlow()

    // Multi-selection state for "Get Ideas" or batch operations
    private val _selectedSupplyIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSupplyIds: StateFlow<Set<String>> = _selectedSupplyIds.asStateFlow()

    // Ideas mode: matchAny (false) vs matchAll (true)
    private val _matchAllSelected = MutableStateFlow(false)
    val matchAllSelected: StateFlow<Boolean> = _matchAllSelected.asStateFlow()

    // Active Tab Navigation: 0 = Stash, 1 = Projects, 2 = Ideas
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Draft Inbox Supplies
    val drafts: StateFlow<List<SupplyWithBatches>> = repository.draftSuppliesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Storage Locations
    val locations: StateFlow<List<StorageLocationEntity>> = repository.locationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Supplies with Batches
    val allSupplies: StateFlow<List<SupplyWithBatches>> = repository.suppliesWithBatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Projects
    val allProjects: StateFlow<List<ProjectWithDetails>> = repository.projectsWithDetailsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Allocations
    val allAllocations: StateFlow<List<ProjectAllocationEntity>> = repository.allAllocationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Inspiration Sources Directory
    val inspirationSources: StateFlow<List<InspirationSourceEntity>> = repository.inspirationSourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Supplies Flow
    val filteredSupplies: StateFlow<List<SupplyWithBatches>> = combine(
        allSupplies,
        _filterState
    ) { supplies, filters ->
        var list = supplies

        // Search query
        if (filters.query.isNotBlank()) {
            val q = filters.query.trim().lowercase()
            list = list.filter { item ->
                item.supply.name.lowercase().contains(q) ||
                        item.supply.brand.lowercase().contains(q) ||
                        item.supply.tags.lowercase().contains(q) ||
                        item.supply.material.lowercase().contains(q) ||
                        item.supply.manufacturerColor.lowercase().contains(q) ||
                        item.supply.barcode.contains(q) ||
                        item.supply.notes.lowercase().contains(q) ||
                        item.batches.any { it.notes.lowercase().contains(q) || it.batchIdentifier.lowercase().contains(q) }
            }
        }

        // Category filter
        if (!filters.selectedCategory.isNullOrBlank()) {
            list = list.filter { it.supply.category.equals(filters.selectedCategory, ignoreCase = true) }
        }

        // Craft Type filter
        if (!filters.selectedCraftType.isNullOrBlank()) {
            list = list.filter { it.supply.craftTypes.contains(filters.selectedCraftType, ignoreCase = true) }
        }

        // Location filter
        if (!filters.selectedLocationId.isNullOrBlank()) {
            list = list.filter { item ->
                item.batches.any { it.storageLocationId == filters.selectedLocationId }
            }
        }

        // Color Family filter
        if (!filters.selectedColorFamily.isNullOrBlank()) {
            list = list.filter { it.supply.colorFamilies.contains(filters.selectedColorFamily, ignoreCase = true) }
        }

        // Favorites only
        if (filters.favoritesOnly) {
            list = list.filter { it.supply.isFavorite }
        }

        // Available only (not depleted)
        if (filters.availableOnly) {
            list = list.filter { item ->
                item.batches.isEmpty() || item.batches.any { !it.isDepleted }
            }
        }

        // Sorting
        when (filters.sortOrder) {
            SortOrder.UPDATED_DESC -> list.sortedByDescending { it.supply.updatedAt }
            SortOrder.NAME_ASC -> list.sortedBy { it.supply.name.lowercase() }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.supply.name.lowercase() }
            SortOrder.CATEGORY -> list.sortedBy { it.supply.category.lowercase() }
            SortOrder.LOCATION -> list.sortedBy { it.batches.firstOrNull()?.storageLocationId.orEmpty() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Matched Saved Projects for Ideas screen
    val matchedProjects: StateFlow<List<ProjectMatchResult>> = combine(
        _selectedSupplyIds,
        allProjects,
        allSupplies,
        allAllocations,
        _matchAllSelected
    ) { selectedIds, projects, supplies, allocations, matchAll ->
        ProjectMatcher.matchProjectsForSupplies(
            selectedSupplyIds = selectedIds,
            allProjects = projects,
            allSuppliesWithBatches = supplies,
            allAllocations = allocations,
            matchAll = matchAll,
            includeCompletedOrArchived = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback state (Snackbars / Status messages)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun clearMessage() {
        _message.value = null
    }

    fun showMessage(msg: String) {
        _message.value = msg
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun setCategoryFilter(category: String?) {
        _filterState.update { it.copy(selectedCategory = category) }
    }

    fun setCraftFilter(craft: String?) {
        _filterState.update { it.copy(selectedCraftType = craft) }
    }

    fun setLocationFilter(locId: String?) {
        _filterState.update { it.copy(selectedLocationId = locId) }
    }

    fun toggleFavoritesFilter() {
        _filterState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    fun toggleAvailableOnlyFilter() {
        _filterState.update { it.copy(availableOnly = !it.availableOnly) }
    }

    fun setSortOrder(order: SortOrder) {
        _filterState.update { it.copy(sortOrder = order) }
    }

    fun setViewMode(mode: StashViewMode) {
        _filterState.update { it.copy(viewMode = mode) }
    }

    fun clearAllFilters() {
        _filterState.value = StashFilterState()
    }

    fun toggleSupplySelection(supplyId: String) {
        _selectedSupplyIds.update { current ->
            if (current.contains(supplyId)) current - supplyId else current + supplyId
        }
    }

    fun clearSelection() {
        _selectedSupplyIds.value = emptySet()
    }

    suspend fun findSupplyByBarcode(barcode: String): SupplyWithBatches? {
        return repository.findSupplyByBarcode(barcode)
    }

    fun setMatchAllSelected(matchAll: Boolean) {
        _matchAllSelected.value = matchAll
    }

    // Supply Management
    fun quickAddSupply(name: String, category: String, locationId: String?, photoUri: String?) {
        viewModelScope.launch {
            val supplyId = UUID.randomUUID().toString()
            val supply = SupplyEntity(
                id = supplyId,
                name = name.trim(),
                category = category.ifBlank { "General Craft" },
                craftTypes = "General",
                photoUris = if (photoUri != null) photoUri else "",
                coverPhotoUri = photoUri
            )
            val batch = BatchEntity(
                id = UUID.randomUUID().toString(),
                supplyId = supplyId,
                batchIdentifier = "Main Piece",
                quantityMode = QuantityMode.PRESENCE_ONLY,
                storageLocationId = locationId
            )
            repository.saveSupply(supply, batch)
            _message.value = "Saved \"$name\" to your stash!"
        }
    }

    fun saveDetailedSupply(
        supply: SupplyEntity,
        batch: BatchEntity?
    ) {
        viewModelScope.launch {
            repository.saveSupply(supply, batch)
            _message.value = "Saved \"${supply.name}\"!"
        }
    }

    fun toggleFavorite(supply: SupplyEntity) {
        viewModelScope.launch {
            repository.updateSupply(supply.copy(isFavorite = !supply.isFavorite))
        }
    }

    fun deleteSupply(supply: SupplyEntity) {
        viewModelScope.launch {
            repository.deleteSupply(supply)
            _message.value = "Removed \"${supply.name}\""
        }
    }

    fun duplicateSupply(
        sourceSupplyId: String,
        batchIdentifier: String,
        qty: Double?,
        mode: QuantityMode,
        unit: String?,
        locationId: String?,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val newId = repository.duplicateSupply(
                    sourceSupplyId = sourceSupplyId,
                    newBatchIdentifier = batchIdentifier,
                    newQuantityValue = qty,
                    newQuantityMode = mode,
                    newUnit = unit,
                    newLocationId = locationId
                )
                _message.value = "Supply duplicated successfully!"
                onSuccess(newId)
            } catch (e: Exception) {
                _message.value = "Failed to duplicate: ${e.message}"
            }
        }
    }

    // Draft Inbox: Capture Now, Organize Later
    fun addDraftPhoto(photoUri: String) {
        viewModelScope.launch {
            val draftId = UUID.randomUUID().toString()
            val draft = SupplyEntity(
                id = draftId,
                name = "Draft Item",
                category = "",
                craftTypes = "",
                isDraft = true,
                photoUris = photoUri,
                coverPhotoUri = photoUri
            )
            repository.saveSupply(draft)
            _message.value = "Photo saved to Draft Inbox!"
        }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            repository.deleteDraft(draftId)
            _message.value = "Draft discarded."
        }
    }

    // Batch Management
    fun saveBatch(batch: BatchEntity) {
        viewModelScope.launch {
            repository.saveBatch(batch)
            _message.value = "Batch saved."
        }
    }

    fun updateBatch(batch: BatchEntity) {
        viewModelScope.launch {
            repository.updateBatch(batch)
        }
    }

    fun toggleDepleted(batch: BatchEntity) {
        viewModelScope.launch {
            repository.updateBatch(batch.copy(isDepleted = !batch.isDepleted))
            _message.value = if (!batch.isDepleted) "Batch marked as depleted" else "Batch marked as active"
        }
    }

    fun deleteBatch(batch: BatchEntity) {
        viewModelScope.launch {
            repository.deleteBatch(batch)
            _message.value = "Batch deleted."
        }
    }

    // Locations
    fun saveLocation(name: String, parentId: String?, notes: String) {
        viewModelScope.launch {
            val loc = StorageLocationEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                parentId = parentId,
                notes = notes
            )
            repository.saveLocation(loc)
            _message.value = "Location \"$name\" added!"
        }
    }

    fun deleteLocation(location: StorageLocationEntity) {
        viewModelScope.launch {
            repository.deleteLocation(location)
            _message.value = "Location deleted."
        }
    }

    // Projects
    fun saveProject(
        project: ProjectEntity,
        requirements: List<ProjectRequirementEntity>
    ) {
        viewModelScope.launch {
            repository.saveProject(project, requirements)
            _message.value = "Project \"${project.title}\" saved!"
        }
    }

    fun updateProjectStatus(project: ProjectEntity, newStatus: ProjectStatus) {
        viewModelScope.launch {
            repository.updateProject(project.copy(status = newStatus))
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            _message.value = "Project deleted."
        }
    }

    fun addRequirement(req: ProjectRequirementEntity) {
        viewModelScope.launch {
            repository.saveRequirement(req)
        }
    }

    fun deleteRequirement(reqId: String) {
        viewModelScope.launch {
            repository.deleteRequirement(reqId)
        }
    }

    fun allocateBatch(
        projectId: String,
        requirementId: String,
        batchId: String,
        reservedQuantity: Double?,
        unit: String?,
        matchStatus: MatchStatus,
        notes: String
    ) {
        viewModelScope.launch {
            val alloc = ProjectAllocationEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                requirementId = requirementId,
                batchId = batchId,
                reservedQuantity = reservedQuantity,
                reservedUnit = unit,
                matchStatus = matchStatus,
                notes = notes
            )
            repository.allocateBatch(alloc)
            _message.value = "Supplies linked to project requirement!"
        }
    }

    fun removeAllocation(allocationId: String) {
        viewModelScope.launch {
            repository.removeAllocation(allocationId)
            _message.value = "Allocation removed."
        }
    }

    fun recordConsumption(
        projectId: String,
        batchId: String,
        quantityUsed: Double?,
        unit: String?,
        notes: String,
        deductFromBatch: Boolean
    ) {
        viewModelScope.launch {
            repository.recordConsumption(
                projectId = projectId,
                batchId = batchId,
                quantityUsed = quantityUsed,
                unit = unit,
                notes = notes,
                deductFromBatch = deductFromBatch
            )
            _message.value = "Confirmed usage recorded."
        }
    }

    fun undoConsumption(recordId: String) {
        viewModelScope.launch {
            repository.undoConsumption(recordId)
            _message.value = "Usage record reversed and stock restored."
        }
    }

    // Inspiration sources
    fun addInspirationSource(name: String, baseUrl: String, searchTemplate: String?, craftTags: String, description: String) {
        viewModelScope.launch {
            val src = InspirationSourceEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                baseUrl = baseUrl,
                searchTemplate = searchTemplate,
                craftTags = craftTags,
                description = description,
                isEnabled = true,
                isBundled = false
            )
            repository.saveInspirationSource(src)
            _message.value = "Added destination \"$name\"."
        }
    }

    fun toggleSourceEnabled(source: InspirationSourceEntity) {
        viewModelScope.launch {
            repository.updateInspirationSource(source.copy(isEnabled = !source.isEnabled))
        }
    }

    fun restoreDefaultSources() {
        viewModelScope.launch {
            repository.restoreDefaultInspirationSources()
            _message.value = "Default inspiration sources restored."
        }
    }

    // Backup & Restore
    fun createBackup(outputUri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.createBackupZip(getApplication(), database, outputUri)
            result.fold(
                onSuccess = { count ->
                    onComplete(true, "Successfully exported backup with $count items and photos.")
                },
                onFailure = { error ->
                    onComplete(false, "Backup failed: ${error.message}")
                }
            )
        }
    }

    fun validateBackup(inputUri: Uri, onResult: (BackupValidationResult) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.validateBackupArchive(getApplication(), inputUri)
            onResult(result)
        }
    }

    fun restoreBackup(inputUri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = BackupManager.restoreBackup(getApplication(), database, inputUri)
            result.fold(
                onSuccess = {
                    onComplete(true, "Collection restored successfully!")
                },
                onFailure = { error ->
                    onComplete(false, "Restore failed: ${error.message}")
                }
            )
        }
    }

    fun exportCsv(outputUri: Uri, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = CsvExporter.exportInventoryToCsv(getApplication(), database, outputUri)
            result.fold(
                onSuccess = { count ->
                    onComplete(true, "Exported $count records to CSV.")
                },
                onFailure = { error ->
                    onComplete(false, "CSV export failed: ${error.message}")
                }
            )
        }
    }

    // Sample Data
    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleData()
            _message.value = "Sample craft stash loaded! You can clear it anytime from Settings."
        }
    }

    fun clearSampleData() {
        viewModelScope.launch {
            repository.clearSampleData()
            _message.value = "Sample data cleared."
        }
    }
}
