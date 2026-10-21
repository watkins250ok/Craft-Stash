package com.example.data.repository

import android.content.Context
import com.example.data.dao.ProjectWithDetails
import com.example.data.dao.SupplyWithBatches
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.domain.model.MatchStatus
import com.example.domain.model.ProjectStatus
import com.example.domain.model.QuantityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class CraftStashRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val supplyDao = database.supplyDao()
    private val batchDao = database.batchDao()
    private val locationDao = database.storageLocationDao()
    private val projectDao = database.projectDao()
    private val reqDao = database.projectRequirementDao()
    private val allocDao = database.projectAllocationDao()
    private val consumptionDao = database.consumptionDao()
    private val sourceDao = database.inspirationSourceDao()
    private val filterDao = database.savedFilterDao()
    private val settingsDao = database.appSettingsDao()

    val suppliesWithBatchesFlow: Flow<List<SupplyWithBatches>> = supplyDao.getAllSuppliesWithBatches()
    val draftSuppliesFlow: Flow<List<SupplyWithBatches>> = supplyDao.getDraftSuppliesWithBatches()
    val locationsFlow: Flow<List<StorageLocationEntity>> = locationDao.getAllLocationsFlow()
    val projectsWithDetailsFlow: Flow<List<ProjectWithDetails>> = projectDao.getAllProjectsWithDetailsFlow()
    val allAllocationsFlow: Flow<List<ProjectAllocationEntity>> = allocDao.getAllAllocationsFlow()
    val inspirationSourcesFlow: Flow<List<InspirationSourceEntity>> = sourceDao.getAllSourcesFlow()
    val savedFiltersFlow: Flow<List<SavedFilterEntity>> = filterDao.getAllSavedFiltersFlow()

    fun getSupplyFlow(supplyId: String): Flow<SupplyWithBatches?> = supplyDao.getSupplyWithBatchesFlow(supplyId)
    fun getProjectFlow(projectId: String): Flow<ProjectWithDetails?> = projectDao.getProjectWithDetailsFlow(projectId)

    suspend fun getSupplyById(id: String): SupplyWithBatches? = supplyDao.getSupplyWithBatches(id)
    suspend fun findSupplyByBarcode(barcode: String): SupplyWithBatches? = supplyDao.findSupplyByBarcode(barcode)

    suspend fun saveSupply(
        supply: SupplyEntity,
        initialBatch: BatchEntity? = null
    ) = withContext(Dispatchers.IO) {
        supplyDao.insertSupply(supply)
        if (initialBatch != null) {
            batchDao.insertBatch(initialBatch)
        }
    }

    suspend fun updateSupply(supply: SupplyEntity) = withContext(Dispatchers.IO) {
        supplyDao.updateSupply(supply.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSupply(supply: SupplyEntity) = withContext(Dispatchers.IO) {
        supplyDao.deleteSupply(supply)
    }

    suspend fun deleteDraft(draftId: String) = withContext(Dispatchers.IO) {
        supplyDao.deleteDraft(draftId)
    }

    /**
     * Duplicate an existing supply:
     * Copies product attributes, but creates a fresh independent record
     * WITHOUT copying project allocations, reservations, or usage history!
     */
    suspend fun duplicateSupply(
        sourceSupplyId: String,
        newBatchIdentifier: String,
        newQuantityValue: Double?,
        newQuantityMode: QuantityMode,
        newUnit: String?,
        newLocationId: String?
    ): String = withContext(Dispatchers.IO) {
        val original = supplyDao.getSupplyWithBatches(sourceSupplyId)
            ?: throw IllegalArgumentException("Source supply not found")

        val newSupplyId = UUID.randomUUID().toString()
        val newSupply = original.supply.copy(
            id = newSupplyId,
            name = "${original.supply.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newBatch = BatchEntity(
            id = UUID.randomUUID().toString(),
            supplyId = newSupplyId,
            batchIdentifier = newBatchIdentifier,
            quantityMode = newQuantityMode,
            quantityValue = newQuantityValue,
            unit = newUnit,
            storageLocationId = newLocationId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        supplyDao.insertSupply(newSupply)
        batchDao.insertBatch(newBatch)
        newSupplyId
    }

    suspend fun saveBatch(batch: BatchEntity) = withContext(Dispatchers.IO) {
        batchDao.insertBatch(batch)
    }

    suspend fun updateBatch(batch: BatchEntity) = withContext(Dispatchers.IO) {
        batchDao.updateBatch(batch.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteBatch(batch: BatchEntity) = withContext(Dispatchers.IO) {
        batchDao.deleteBatch(batch)
    }

    // Storage Locations
    suspend fun saveLocation(location: StorageLocationEntity) = withContext(Dispatchers.IO) {
        locationDao.insertLocation(location)
    }

    suspend fun deleteLocation(location: StorageLocationEntity) = withContext(Dispatchers.IO) {
        locationDao.deleteLocation(location)
    }

    // Projects
    suspend fun saveProject(
        project: ProjectEntity,
        requirements: List<ProjectRequirementEntity> = emptyList()
    ) = withContext(Dispatchers.IO) {
        projectDao.insertProject(project)
        if (requirements.isNotEmpty()) {
            reqDao.insertRequirements(requirements)
        }
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        // Explicitly clear allocations without deleting physical batches
        allocDao.deleteAllocationsForProject(project.id)
        projectDao.deleteProject(project)
    }

    suspend fun saveRequirement(requirement: ProjectRequirementEntity) = withContext(Dispatchers.IO) {
        reqDao.insertRequirement(requirement)
    }

    suspend fun deleteRequirement(requirementId: String) = withContext(Dispatchers.IO) {
        reqDao.deleteRequirementById(requirementId)
    }

    suspend fun allocateBatch(
        allocation: ProjectAllocationEntity
    ) = withContext(Dispatchers.IO) {
        allocDao.insertAllocation(allocation)
    }

    suspend fun removeAllocation(allocationId: String) = withContext(Dispatchers.IO) {
        allocDao.deleteAllocationById(allocationId)
    }

    /**
     * Records confirmed usage (consumption) on completing or advancing a project.
     * Optionally deducts the consumed amount from the physical batch's on-hand quantity atomically.
     */
    suspend fun recordConsumption(
        projectId: String,
        batchId: String,
        quantityUsed: Double?,
        unit: String?,
        notes: String,
        deductFromBatch: Boolean
    ): String = withContext(Dispatchers.IO) {
        val recordId = UUID.randomUUID().toString()
        val record = ConsumptionRecordEntity(
            id = recordId,
            projectId = projectId,
            batchId = batchId,
            quantityUsed = quantityUsed,
            unit = unit,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )

        consumptionDao.insertConsumption(record)

        if (deductFromBatch && quantityUsed != null && quantityUsed > 0) {
            val batch = batchDao.getBatchById(batchId)
            if (batch != null && batch.quantityValue != null && batch.quantityMode == QuantityMode.EXACT) {
                val newQty = maxOf(0.0, batch.quantityValue - quantityUsed)
                val isDepleted = newQty <= 0.0
                batchDao.updateBatch(batch.copy(quantityValue = newQty, isDepleted = isDepleted))
            }
        }

        recordId
    }

    /**
     * Undoes a consumption record, restoring deducted quantities reliably.
     */
    suspend fun undoConsumption(recordId: String) = withContext(Dispatchers.IO) {
        val record = consumptionDao.getConsumptionRecordById(recordId) ?: return@withContext
        if (record.quantityUsed != null && record.quantityUsed > 0) {
            val batch = batchDao.getBatchById(record.batchId)
            if (batch != null && batch.quantityValue != null && batch.quantityMode == QuantityMode.EXACT) {
                val restoredQty = batch.quantityValue + record.quantityUsed
                batchDao.updateBatch(batch.copy(quantityValue = restoredQty, isDepleted = false))
            }
        }
        consumptionDao.deleteConsumption(record)
    }

    fun getConsumptionRecords(projectId: String): Flow<List<ConsumptionRecordEntity>> {
        return consumptionDao.getConsumptionRecordsForProject(projectId)
    }

    // Inspiration sources
    suspend fun saveInspirationSource(source: InspirationSourceEntity) = withContext(Dispatchers.IO) {
        sourceDao.insertSource(source)
    }

    suspend fun updateInspirationSource(source: InspirationSourceEntity) = withContext(Dispatchers.IO) {
        sourceDao.updateSource(source)
    }

    suspend fun deleteInspirationSource(source: InspirationSourceEntity) = withContext(Dispatchers.IO) {
        sourceDao.deleteSource(source)
    }

    suspend fun restoreDefaultInspirationSources() = withContext(Dispatchers.IO) {
        sourceDao.deleteBundledSources()
        sourceDao.insertSources(AppDatabase.DEFAULT_INSPIRATION_SOURCES)
    }

    // Saved filters
    suspend fun saveFilter(name: String, filterJson: String) = withContext(Dispatchers.IO) {
        filterDao.insertFilter(
            SavedFilterEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                filterJson = filterJson
            )
        )
    }

    suspend fun deleteFilter(filter: SavedFilterEntity) = withContext(Dispatchers.IO) {
        filterDao.deleteFilter(filter)
    }

    // Sample Data Preview
    suspend fun loadSampleData() = withContext(Dispatchers.IO) {
        val locStudio = StorageLocationEntity(
            id = "loc_studio",
            name = "Studio Rolling Cart",
            notes = "Top 3 drawers near the window"
        )
        val locDrawer = StorageLocationEntity(
            id = "loc_drawer",
            name = "Drawer 1 - Cotton & Floss",
            parentId = "loc_studio",
            notes = "Spools, embroidery floss, and small cut pieces"
        )
        val locYarnBin = StorageLocationEntity(
            id = "loc_bin",
            name = "Yarn Basket Under Table",
            notes = "Large woven wicker basket"
        )
        locationDao.insertLocation(locStudio)
        locationDao.insertLocation(locDrawer)
        locationDao.insertLocation(locYarnBin)

        // Sample Supply 1: Cotton Yarn
        val s1 = SupplyEntity(
            id = "sample_yarn_1",
            name = "Kona Worsted Cotton",
            category = "Yarn",
            craftTypes = "Knitting, Crochet",
            material = "100% Mercerized Cotton",
            colorFamilies = "Blue, Teal",
            manufacturerColor = "Aegean #312",
            appearance = "Solid",
            brand = "Artisan Craft Co.",
            barcode = "012345678905",
            notes = "Very soft with nice stitch definition. Great for washcloths, summer tops, and bags.",
            tags = "cotton, worsted, summer",
            isFavorite = true
        )
        val b1 = BatchEntity(
            id = "sample_batch_1",
            supplyId = "sample_yarn_1",
            batchIdentifier = "Dye Lot 402",
            quantityMode = QuantityMode.EXACT,
            quantityValue = 300.0,
            unit = "g",
            measurementMeaning = "Weight",
            storageLocationId = "loc_bin",
            condition = "Sealed",
            notes = "3 full 100g skeins"
        )
        val b1Remnant = BatchEntity(
            id = "sample_batch_1_remnant",
            supplyId = "sample_yarn_1",
            batchIdentifier = "Partial Ball",
            quantityMode = QuantityMode.APPROXIMATE,
            quantityValue = 45.0,
            unit = "g",
            storageLocationId = "loc_drawer",
            condition = "Opened / Remnant",
            notes = "Leftover from headband project"
        )

        // Sample Supply 2: Quilt Cotton Fabric
        val s2 = SupplyEntity(
            id = "sample_fabric_1",
            name = "Botanical Garden Floral Fabric",
            category = "Fabric",
            craftTypes = "Sewing, Quilting",
            material = "100% Cotton Poplin",
            colorFamilies = "Olive, Rust, Cream",
            manufacturerColor = "Wildflower Meadow",
            appearance = "Patterned",
            brand = "Modern Maker Cottons",
            notes = "Medium weight woven. Pre-washed.",
            tags = "floral, quilting, remnants",
            isFavorite = true
        )
        val b2PieceA = BatchEntity(
            id = "sample_batch_2_a",
            supplyId = "sample_fabric_1",
            batchIdentifier = "Bolt Cut Piece",
            quantityMode = QuantityMode.EXACT,
            quantityValue = 1.5,
            unit = "yards",
            measurementMeaning = "Length",
            dimensionWidth = 44.0,
            dimensionLength = 54.0,
            dimensionUnit = "inches",
            storageLocationId = "loc_drawer",
            condition = "Sealed"
        )
        val b2PieceB = BatchEntity(
            id = "sample_batch_2_b",
            supplyId = "sample_fabric_1",
            batchIdentifier = "Scrap Fat Quarter",
            quantityMode = QuantityMode.EXACT,
            quantityValue = 1.0,
            unit = "pieces",
            measurementMeaning = "Count",
            dimensionWidth = 18.0,
            dimensionLength = 22.0,
            dimensionUnit = "inches",
            storageLocationId = "loc_drawer",
            condition = "Scraps / Remnants"
        )

        // Sample Supply 3: Bamboo Knitting Needles (Reusable Tool)
        val s3 = SupplyEntity(
            id = "sample_tool_1",
            name = "ChiaoGoo Bamboo Circular Needles 5mm",
            category = "Tools",
            craftTypes = "Knitting",
            material = "Bamboo & Stainless Cable",
            brand = "ChiaoGoo",
            notes = "US 8 / 5.0mm, 32 inch circular cord.",
            tags = "tools, needles, 5mm"
        )
        val b3 = BatchEntity(
            id = "sample_batch_3",
            supplyId = "sample_tool_1",
            batchIdentifier = "Pair 1",
            quantityMode = QuantityMode.EXACT,
            quantityValue = 1.0,
            unit = "pair",
            measurementMeaning = "Count",
            storageLocationId = "loc_drawer",
            condition = "Reusable Tool"
        )

        // Sample Supply 4: Wooden Craft Buttons
        val s4 = SupplyEntity(
            id = "sample_buttons_1",
            name = "Olive Wood 4-Hole Buttons 20mm",
            category = "Beads & Jewelry",
            craftTypes = "Sewing, Knitting, Woodworking",
            material = "Olive Wood",
            colorFamilies = "Neutral, Earthy",
            appearance = "Natural Wood Grain",
            notes = "20mm diameter, smooth satin natural wax finish.",
            tags = "buttons, wood, notions"
        )
        val b4 = BatchEntity(
            id = "sample_batch_4",
            supplyId = "sample_buttons_1",
            batchIdentifier = "Box of 24",
            quantityMode = QuantityMode.EXACT,
            quantityValue = 24.0,
            unit = "pieces",
            measurementMeaning = "Count",
            storageLocationId = "loc_drawer",
            condition = "Sealed"
        )

        supplyDao.insertSupply(s1)
        batchDao.insertBatch(b1)
        batchDao.insertBatch(b1Remnant)

        supplyDao.insertSupply(s2)
        batchDao.insertBatch(b2PieceA)
        batchDao.insertBatch(b2PieceB)

        supplyDao.insertSupply(s3)
        batchDao.insertBatch(b3)

        supplyDao.insertSupply(s4)
        batchDao.insertBatch(b4)

        // Sample Project 1: Summer Market Bag
        val proj1 = ProjectEntity(
            id = "sample_proj_1",
            title = "Coastal Market Tote Bag",
            status = ProjectStatus.PLANNED,
            craftType = "Crochet",
            sourceUrl = "https://www.ravelry.com",
            sourceName = "Ravelry",
            notes = "Sturdy filet mesh market bag with reinforced shoulder straps.",
            finishedMeasurements = "14 in wide x 16 in tall"
        )
        val req1 = ProjectRequirementEntity(
            id = "sample_req_1",
            projectId = "sample_proj_1",
            description = "Worsted cotton yarn",
            category = "Yarn",
            material = "Cotton",
            quantityRequired = 200.0,
            unit = "g",
            isRequired = true
        )
        val req2 = ProjectRequirementEntity(
            id = "sample_req_2",
            projectId = "sample_proj_1",
            description = "5.0mm Crochet Hook or Needles",
            category = "Tools",
            isReusableTool = true,
            isRequired = true
        )

        projectDao.insertProject(proj1)
        reqDao.insertRequirement(req1)
        reqDao.insertRequirement(req2)

        // Allocate batch 1 to requirement 1
        val alloc1 = ProjectAllocationEntity(
            id = "sample_alloc_1",
            projectId = "sample_proj_1",
            requirementId = "sample_req_1",
            batchId = "sample_batch_1",
            reservedQuantity = 200.0,
            reservedUnit = "g",
            matchStatus = MatchStatus.EXACT_MATCH,
            notes = "Reserved 2 full skeins from Dye Lot 402"
        )
        allocDao.insertAllocation(alloc1)
    }

    suspend fun clearSampleData() = withContext(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase
        db.execSQL("DELETE FROM project_allocations WHERE id LIKE 'sample_%'")
        db.execSQL("DELETE FROM project_requirements WHERE id LIKE 'sample_%'")
        db.execSQL("DELETE FROM projects WHERE id LIKE 'sample_%'")
        db.execSQL("DELETE FROM batches WHERE id LIKE 'sample_%'")
        db.execSQL("DELETE FROM supplies WHERE id LIKE 'sample_%'")
        db.execSQL("DELETE FROM storage_locations WHERE id LIKE 'sample_%'")
    }

    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
        settingsDao.getValue(key)
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        settingsDao.setValue(AppSettingsEntity(key, value))
    }
}
