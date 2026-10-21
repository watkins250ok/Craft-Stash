package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

data class SupplyWithBatches(
    @Embedded val supply: SupplyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "supplyId"
    )
    val batches: List<BatchEntity>
)

data class ProjectWithDetails(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val requirements: List<ProjectRequirementEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val allocations: List<ProjectAllocationEntity>
)

@Dao
interface SupplyDao {
    @Transaction
    @Query("SELECT * FROM supplies WHERE isDraft = 0 ORDER BY updatedAt DESC")
    fun getAllSuppliesWithBatches(): Flow<List<SupplyWithBatches>>

    @Transaction
    @Query("SELECT * FROM supplies WHERE isDraft = 1 ORDER BY createdAt DESC")
    fun getDraftSuppliesWithBatches(): Flow<List<SupplyWithBatches>>

    @Transaction
    @Query("SELECT * FROM supplies WHERE id = :supplyId LIMIT 1")
    fun getSupplyWithBatchesFlow(supplyId: String): Flow<SupplyWithBatches?>

    @Transaction
    @Query("SELECT * FROM supplies WHERE id = :supplyId LIMIT 1")
    suspend fun getSupplyWithBatches(supplyId: String): SupplyWithBatches?

    @Transaction
    @Query("SELECT * FROM supplies WHERE barcode = :barcode LIMIT 1")
    suspend fun findSupplyByBarcode(barcode: String): SupplyWithBatches?

    @Query("SELECT * FROM supplies WHERE id = :supplyId LIMIT 1")
    suspend fun getSupplyById(supplyId: String): SupplyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupply(supply: SupplyEntity)

    @Update
    suspend fun updateSupply(supply: SupplyEntity)

    @Delete
    suspend fun deleteSupply(supply: SupplyEntity)

    @Query("DELETE FROM supplies WHERE isDraft = 1 AND id = :id")
    suspend fun deleteDraft(id: String)
}

@Dao
interface BatchDao {
    @Query("SELECT * FROM batches WHERE supplyId = :supplyId")
    suspend fun getBatchesForSupply(supplyId: String): List<BatchEntity>

    @Query("SELECT * FROM batches WHERE id = :batchId LIMIT 1")
    suspend fun getBatchById(batchId: String): BatchEntity?

    @Query("SELECT * FROM batches")
    fun getAllBatchesFlow(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches")
    suspend fun getAllBatches(): List<BatchEntity>

    @Query("SELECT * FROM batches WHERE storageLocationId = :locationId")
    fun getBatchesByLocation(locationId: String): Flow<List<BatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: BatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatches(batches: List<BatchEntity>)

    @Update
    suspend fun updateBatch(batch: BatchEntity)

    @Delete
    suspend fun deleteBatch(batch: BatchEntity)
}

@Dao
interface StorageLocationDao {
    @Query("SELECT * FROM storage_locations ORDER BY name ASC")
    fun getAllLocationsFlow(): Flow<List<StorageLocationEntity>>

    @Query("SELECT * FROM storage_locations")
    suspend fun getAllLocations(): List<StorageLocationEntity>

    @Query("SELECT * FROM storage_locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): StorageLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: StorageLocationEntity)

    @Update
    suspend fun updateLocation(location: StorageLocationEntity)

    @Delete
    suspend fun deleteLocation(location: StorageLocationEntity)
}

@Dao
interface ProjectDao {
    @Transaction
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjectsWithDetailsFlow(): Flow<List<ProjectWithDetails>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    fun getProjectWithDetailsFlow(projectId: String): Flow<ProjectWithDetails?>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectWithDetails(projectId: String): ProjectWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface ProjectRequirementDao {
    @Query("SELECT * FROM project_requirements WHERE projectId = :projectId")
    suspend fun getRequirementsForProject(projectId: String): List<ProjectRequirementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequirement(requirement: ProjectRequirementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequirements(requirements: List<ProjectRequirementEntity>)

    @Update
    suspend fun updateRequirement(requirement: ProjectRequirementEntity)

    @Delete
    suspend fun deleteRequirement(requirement: ProjectRequirementEntity)

    @Query("DELETE FROM project_requirements WHERE id = :id")
    suspend fun deleteRequirementById(id: String)
}

@Dao
interface ProjectAllocationDao {
    @Query("SELECT * FROM project_allocations")
    fun getAllAllocationsFlow(): Flow<List<ProjectAllocationEntity>>

    @Query("SELECT * FROM project_allocations")
    suspend fun getAllAllocations(): List<ProjectAllocationEntity>

    @Query("SELECT * FROM project_allocations WHERE batchId = :batchId")
    suspend fun getAllocationsForBatch(batchId: String): List<ProjectAllocationEntity>

    @Query("SELECT * FROM project_allocations WHERE projectId = :projectId")
    suspend fun getAllocationsForProject(projectId: String): List<ProjectAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: ProjectAllocationEntity)

    @Delete
    suspend fun deleteAllocation(allocation: ProjectAllocationEntity)

    @Query("DELETE FROM project_allocations WHERE projectId = :projectId")
    suspend fun deleteAllocationsForProject(projectId: String)

    @Query("DELETE FROM project_allocations WHERE id = :id")
    suspend fun deleteAllocationById(id: String)
}

@Dao
interface ConsumptionDao {
    @Query("SELECT * FROM consumption_records WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getConsumptionRecordsForProject(projectId: String): Flow<List<ConsumptionRecordEntity>>

    @Query("SELECT * FROM consumption_records WHERE id = :id LIMIT 1")
    suspend fun getConsumptionRecordById(id: String): ConsumptionRecordEntity?

    @Query("SELECT * FROM consumption_records")
    suspend fun getAllConsumptionRecords(): List<ConsumptionRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumption(record: ConsumptionRecordEntity)

    @Delete
    suspend fun deleteConsumption(record: ConsumptionRecordEntity)
}

@Dao
interface InspirationSourceDao {
    @Query("SELECT * FROM inspiration_sources ORDER BY isBundled DESC, name ASC")
    fun getAllSourcesFlow(): Flow<List<InspirationSourceEntity>>

    @Query("SELECT * FROM inspiration_sources")
    suspend fun getAllSources(): List<InspirationSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSources(sources: List<InspirationSourceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: InspirationSourceEntity)

    @Update
    suspend fun updateSource(source: InspirationSourceEntity)

    @Delete
    suspend fun deleteSource(source: InspirationSourceEntity)

    @Query("DELETE FROM inspiration_sources WHERE isBundled = 1")
    suspend fun deleteBundledSources()
}

@Dao
interface InspirationGalleryDao {
    @Query("SELECT * FROM inspiration_gallery ORDER BY createdAt DESC")
    fun getAllGalleryItemsFlow(): Flow<List<InspirationGalleryItemEntity>>

    @Query("SELECT * FROM inspiration_gallery")
    suspend fun getAllGalleryItems(): List<InspirationGalleryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGalleryItem(item: InspirationGalleryItemEntity)

    @Delete
    suspend fun deleteGalleryItem(item: InspirationGalleryItemEntity)
}

@Dao
interface SavedFilterDao {
    @Query("SELECT * FROM saved_filters ORDER BY createdAt DESC")
    fun getAllSavedFiltersFlow(): Flow<List<SavedFilterEntity>>

    @Query("SELECT * FROM saved_filters")
    suspend fun getAllSavedFilters(): List<SavedFilterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: SavedFilterEntity)

    @Delete
    suspend fun deleteFilter(filter: SavedFilterEntity)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettings(): List<AppSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(setting: AppSettingsEntity)
}
