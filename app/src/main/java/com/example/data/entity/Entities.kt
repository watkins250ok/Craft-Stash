package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.MatchStatus
import com.example.domain.model.ProjectStatus
import com.example.domain.model.QuantityMode

@Entity(
    tableName = "supplies",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"]),
        Index(value = ["barcode"]),
        Index(value = ["isDraft"]),
        Index(value = ["isFavorite"])
    ]
)
data class SupplyEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val craftTypes: String, // comma-separated or JSON list
    val material: String = "",
    val colorFamilies: String = "", // comma-separated
    val manufacturerColor: String = "",
    val appearance: String = "", // Solid, Patterned, Variegated, Metallic, etc.
    val brand: String = "",
    val barcode: String = "", // Preserving leading zeros as text!
    val notes: String = "",
    val tags: String = "", // comma-separated
    val isFavorite: Boolean = false,
    val isDraft: Boolean = false,
    val photoUris: String = "", // JSON list of relative file names
    val coverPhotoUri: String? = null,
    val categoryTemplateJson: String = "", // Category specific attributes
    val customFieldsJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "batches",
    foreignKeys = [
        ForeignKey(
            entity = SupplyEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["supplyId"]),
        Index(value = ["storageLocationId"]),
        Index(value = ["isDepleted"])
    ]
)
data class BatchEntity(
    @PrimaryKey
    val id: String,
    val supplyId: String,
    val batchIdentifier: String = "", // e.g., Dye lot #4902, Skein #2, Bolt piece A
    val quantityMode: QuantityMode = QuantityMode.EXACT,
    val quantityValue: Double? = null,
    val unit: String? = null,
    val measurementMeaning: String = "Quantity",
    val dimensionWidth: Double? = null,
    val dimensionLength: Double? = null,
    val dimensionUnit: String? = null,
    val storageLocationId: String? = null,
    val condition: String = "Sealed", // Sealed, Opened, Remnant, Scraps
    val notes: String = "",
    val photoUri: String? = null,
    val isDepleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "storage_locations",
    indices = [
        Index(value = ["name"]),
        Index(value = ["parentId"])
    ]
)
data class StorageLocationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val parentId: String? = null, // Hierarchical nesting
    val notes: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["status"]),
        Index(value = ["craftType"])
    ]
)
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val status: ProjectStatus = ProjectStatus.PLANNED,
    val craftType: String = "",
    val sourceUrl: String? = null,
    val sourceName: String? = null,
    val notes: String = "",
    val coverPhotoUri: String? = null,
    val progressPhotosJson: String = "", // JSON list of photo paths
    val finishedMeasurements: String? = null,
    val completionDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "project_requirements",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"])
    ]
)
data class ProjectRequirementEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val description: String,
    val category: String? = null,
    val material: String? = null,
    val colorPreference: String? = null,
    val quantityRequired: Double? = null,
    val unit: String? = null,
    val dimensionWidthRequired: Double? = null,
    val dimensionLengthRequired: Double? = null,
    val dimensionUnit: String? = null,
    val singleContinuousPieceRequired: Boolean = false,
    val multipleBatchesAllowed: Boolean = true,
    val dyeLotMixingAllowed: Boolean = true,
    val isReusableTool: Boolean = false,
    val isRequired: Boolean = true
)

@Entity(
    tableName = "project_allocations",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProjectRequirementEntity::class,
            parentColumns = ["id"],
            childColumns = ["requirementId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["requirementId"]),
        Index(value = ["batchId"])
    ]
)
data class ProjectAllocationEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val requirementId: String,
    val batchId: String,
    val reservedQuantity: Double? = null,
    val reservedUnit: String? = null,
    val matchStatus: MatchStatus = MatchStatus.EXACT_MATCH,
    val notes: String = "",
    val allocatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "consumption_records",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["batchId"])
    ]
)
data class ConsumptionRecordEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val batchId: String,
    val quantityUsed: Double? = null,
    val unit: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "inspiration_sources")
data class InspirationSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val baseUrl: String,
    val searchTemplate: String? = null,
    val craftTags: String = "",
    val description: String = "",
    val accountPaymentNote: String = "",
    val isEnabled: Boolean = true,
    val isBundled: Boolean = true
)

@Entity(tableName = "saved_filters")
data class SavedFilterEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val filterJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
