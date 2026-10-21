package com.example.domain.backup

import android.content.Context
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.photo.PhotoManager
import com.example.domain.model.MatchStatus
import com.example.domain.model.ProjectStatus
import com.example.domain.model.QuantityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupValidationResult(
    val isValid: Boolean,
    val version: Int = 1,
    val supplyCount: Int = 0,
    val batchCount: Int = 0,
    val locationCount: Int = 0,
    val projectCount: Int = 0,
    val photoCount: Int = 0,
    val warningMessage: String? = null,
    val errorMessage: String? = null
)

object BackupManager {
    const val CURRENT_BACKUP_VERSION = 1
    private const val MANIFEST_FILE_NAME = "craft_stash_backup.json"
    private const val PHOTOS_FOLDER = "photos/"

    suspend fun createBackupZip(context: Context, database: AppDatabase, outputUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val supplies = database.supplyDao().getSupplyById("dummy") // check connection
            val allSupplies = mutableListOf<SupplyEntity>()
            // Query all supplies
            val supplyWithBatches = database.supplyDao().getSupplyById("") // just check
            val allSuppliesList = mutableListOf<SupplyEntity>()
            // We can read supplies directly or using raw queries or DAOs
            // Let's read all tables
            val locations = database.storageLocationDao().getAllLocations()
            val batches = database.batchDao().getAllBatches()
            val projects = mutableListOf<ProjectEntity>()
            val requirements = mutableListOf<ProjectRequirementEntity>()
            val allocations = database.projectAllocationDao().getAllAllocations()
            val consumptions = database.consumptionDao().getAllConsumptionRecords()
            val sources = database.inspirationSourceDao().getAllSources()
            val filters = database.savedFilterDao().getAllSavedFilters()
            val settings = database.appSettingsDao().getAllSettings()

            // Fetch supplies through supplyDao
            // Let's get all supplies through a direct list
            val db = database.openHelper.readableDatabase
            val supplyCursor = db.query("SELECT * FROM supplies")
            while (supplyCursor.moveToNext()) {
                allSuppliesList.add(
                    SupplyEntity(
                        id = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("id")),
                        name = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("name")),
                        category = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("category")),
                        craftTypes = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("craftTypes")),
                        material = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("material")),
                        colorFamilies = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("colorFamilies")),
                        manufacturerColor = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("manufacturerColor")),
                        appearance = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("appearance")),
                        brand = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("brand")),
                        barcode = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("barcode")),
                        notes = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("notes")),
                        tags = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("tags")),
                        isFavorite = supplyCursor.getInt(supplyCursor.getColumnIndexOrThrow("isFavorite")) == 1,
                        isDraft = supplyCursor.getInt(supplyCursor.getColumnIndexOrThrow("isDraft")) == 1,
                        photoUris = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("photoUris")),
                        coverPhotoUri = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("coverPhotoUri")),
                        categoryTemplateJson = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("categoryTemplateJson")),
                        customFieldsJson = supplyCursor.getString(supplyCursor.getColumnIndexOrThrow("customFieldsJson")),
                        createdAt = supplyCursor.getLong(supplyCursor.getColumnIndexOrThrow("createdAt")),
                        updatedAt = supplyCursor.getLong(supplyCursor.getColumnIndexOrThrow("updatedAt"))
                    )
                )
            }
            supplyCursor.close()

            val projectCursor = db.query("SELECT * FROM projects")
            while (projectCursor.moveToNext()) {
                projects.add(
                    ProjectEntity(
                        id = projectCursor.getString(projectCursor.getColumnIndexOrThrow("id")),
                        title = projectCursor.getString(projectCursor.getColumnIndexOrThrow("title")),
                        status = try { ProjectStatus.valueOf(projectCursor.getString(projectCursor.getColumnIndexOrThrow("status"))) } catch(e: Exception) { ProjectStatus.PLANNED },
                        craftType = projectCursor.getString(projectCursor.getColumnIndexOrThrow("craftType")),
                        sourceUrl = projectCursor.getString(projectCursor.getColumnIndexOrThrow("sourceUrl")),
                        sourceName = projectCursor.getString(projectCursor.getColumnIndexOrThrow("sourceName")),
                        notes = projectCursor.getString(projectCursor.getColumnIndexOrThrow("notes")),
                        coverPhotoUri = projectCursor.getString(projectCursor.getColumnIndexOrThrow("coverPhotoUri")),
                        progressPhotosJson = projectCursor.getString(projectCursor.getColumnIndexOrThrow("progressPhotosJson")),
                        finishedMeasurements = projectCursor.getString(projectCursor.getColumnIndexOrThrow("finishedMeasurements")),
                        completionDate = if (projectCursor.isNull(projectCursor.getColumnIndexOrThrow("completionDate"))) null else projectCursor.getLong(projectCursor.getColumnIndexOrThrow("completionDate")),
                        createdAt = projectCursor.getLong(projectCursor.getColumnIndexOrThrow("createdAt")),
                        updatedAt = projectCursor.getLong(projectCursor.getColumnIndexOrThrow("updatedAt"))
                    )
                )
            }
            projectCursor.close()

            val reqCursor = db.query("SELECT * FROM project_requirements")
            while (reqCursor.moveToNext()) {
                requirements.add(
                    ProjectRequirementEntity(
                        id = reqCursor.getString(reqCursor.getColumnIndexOrThrow("id")),
                        projectId = reqCursor.getString(reqCursor.getColumnIndexOrThrow("projectId")),
                        description = reqCursor.getString(reqCursor.getColumnIndexOrThrow("description")),
                        category = reqCursor.getString(reqCursor.getColumnIndexOrThrow("category")),
                        material = reqCursor.getString(reqCursor.getColumnIndexOrThrow("material")),
                        colorPreference = reqCursor.getString(reqCursor.getColumnIndexOrThrow("colorPreference")),
                        quantityRequired = if (reqCursor.isNull(reqCursor.getColumnIndexOrThrow("quantityRequired"))) null else reqCursor.getDouble(reqCursor.getColumnIndexOrThrow("quantityRequired")),
                        unit = reqCursor.getString(reqCursor.getColumnIndexOrThrow("unit")),
                        dimensionWidthRequired = if (reqCursor.isNull(reqCursor.getColumnIndexOrThrow("dimensionWidthRequired"))) null else reqCursor.getDouble(reqCursor.getColumnIndexOrThrow("dimensionWidthRequired")),
                        dimensionLengthRequired = if (reqCursor.isNull(reqCursor.getColumnIndexOrThrow("dimensionLengthRequired"))) null else reqCursor.getDouble(reqCursor.getColumnIndexOrThrow("dimensionLengthRequired")),
                        dimensionUnit = reqCursor.getString(reqCursor.getColumnIndexOrThrow("dimensionUnit")),
                        singleContinuousPieceRequired = reqCursor.getInt(reqCursor.getColumnIndexOrThrow("singleContinuousPieceRequired")) == 1,
                        multipleBatchesAllowed = reqCursor.getInt(reqCursor.getColumnIndexOrThrow("multipleBatchesAllowed")) == 1,
                        dyeLotMixingAllowed = reqCursor.getInt(reqCursor.getColumnIndexOrThrow("dyeLotMixingAllowed")) == 1,
                        isReusableTool = reqCursor.getInt(reqCursor.getColumnIndexOrThrow("isReusableTool")) == 1,
                        isRequired = reqCursor.getInt(reqCursor.getColumnIndexOrThrow("isRequired")) == 1
                    )
                )
            }
            reqCursor.close()

            val rootJson = JSONObject().apply {
                put("format", "CraftStashBackup")
                put("version", CURRENT_BACKUP_VERSION)
                put("exportTimestamp", System.currentTimeMillis())

                // Supplies array
                val sArray = JSONArray()
                allSuppliesList.forEach { s ->
                    sArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("category", s.category)
                        put("craftTypes", s.craftTypes)
                        put("material", s.material)
                        put("colorFamilies", s.colorFamilies)
                        put("manufacturerColor", s.manufacturerColor)
                        put("appearance", s.appearance)
                        put("brand", s.brand)
                        put("barcode", s.barcode)
                        put("notes", s.notes)
                        put("tags", s.tags)
                        put("isFavorite", s.isFavorite)
                        put("isDraft", s.isDraft)
                        put("photoUris", s.photoUris)
                        put("coverPhotoUri", s.coverPhotoUri ?: "")
                        put("categoryTemplateJson", s.categoryTemplateJson)
                        put("customFieldsJson", s.customFieldsJson)
                        put("createdAt", s.createdAt)
                        put("updatedAt", s.updatedAt)
                    })
                }
                put("supplies", sArray)

                // Batches array
                val bArray = JSONArray()
                batches.forEach { b ->
                    bArray.put(JSONObject().apply {
                        put("id", b.id)
                        put("supplyId", b.supplyId)
                        put("batchIdentifier", b.batchIdentifier)
                        put("quantityMode", b.quantityMode.name)
                        put("quantityValue", b.quantityValue ?: JSONObject.NULL)
                        put("unit", b.unit ?: "")
                        put("measurementMeaning", b.measurementMeaning)
                        put("dimensionWidth", b.dimensionWidth ?: JSONObject.NULL)
                        put("dimensionLength", b.dimensionLength ?: JSONObject.NULL)
                        put("dimensionUnit", b.dimensionUnit ?: "")
                        put("storageLocationId", b.storageLocationId ?: "")
                        put("condition", b.condition)
                        put("notes", b.notes)
                        put("photoUri", b.photoUri ?: "")
                        put("isDepleted", b.isDepleted)
                        put("createdAt", b.createdAt)
                        put("updatedAt", b.updatedAt)
                    })
                }
                put("batches", bArray)

                // Locations
                val lArray = JSONArray()
                locations.forEach { l ->
                    lArray.put(JSONObject().apply {
                        put("id", l.id)
                        put("name", l.name)
                        put("parentId", l.parentId ?: "")
                        put("notes", l.notes)
                        put("photoUri", l.photoUri ?: "")
                        put("createdAt", l.createdAt)
                    })
                }
                put("storageLocations", lArray)

                // Projects
                val pArray = JSONArray()
                projects.forEach { p ->
                    pArray.put(JSONObject().apply {
                        put("id", p.id)
                        put("title", p.title)
                        put("status", p.status.name)
                        put("craftType", p.craftType)
                        put("sourceUrl", p.sourceUrl ?: "")
                        put("sourceName", p.sourceName ?: "")
                        put("notes", p.notes)
                        put("coverPhotoUri", p.coverPhotoUri ?: "")
                        put("progressPhotosJson", p.progressPhotosJson)
                        put("finishedMeasurements", p.finishedMeasurements ?: "")
                        put("completionDate", p.completionDate ?: JSONObject.NULL)
                        put("createdAt", p.createdAt)
                        put("updatedAt", p.updatedAt)
                    })
                }
                put("projects", pArray)

                // Requirements
                val rArray = JSONArray()
                requirements.forEach { r ->
                    rArray.put(JSONObject().apply {
                        put("id", r.id)
                        put("projectId", r.projectId)
                        put("description", r.description)
                        put("category", r.category ?: "")
                        put("material", r.material ?: "")
                        put("colorPreference", r.colorPreference ?: "")
                        put("quantityRequired", r.quantityRequired ?: JSONObject.NULL)
                        put("unit", r.unit ?: "")
                        put("dimensionWidthRequired", r.dimensionWidthRequired ?: JSONObject.NULL)
                        put("dimensionLengthRequired", r.dimensionLengthRequired ?: JSONObject.NULL)
                        put("dimensionUnit", r.dimensionUnit ?: "")
                        put("singleContinuousPieceRequired", r.singleContinuousPieceRequired)
                        put("multipleBatchesAllowed", r.multipleBatchesAllowed)
                        put("dyeLotMixingAllowed", r.dyeLotMixingAllowed)
                        put("isReusableTool", r.isReusableTool)
                        put("isRequired", r.isRequired)
                    })
                }
                put("projectRequirements", rArray)

                // Allocations
                val aArray = JSONArray()
                allocations.forEach { a ->
                    aArray.put(JSONObject().apply {
                        put("id", a.id)
                        put("projectId", a.projectId)
                        put("requirementId", a.requirementId)
                        put("batchId", a.batchId)
                        put("reservedQuantity", a.reservedQuantity ?: JSONObject.NULL)
                        put("reservedUnit", a.reservedUnit ?: "")
                        put("matchStatus", a.matchStatus.name)
                        put("notes", a.notes)
                        put("allocatedAt", a.allocatedAt)
                    })
                }
                put("projectAllocations", aArray)

                // Consumption
                val cArray = JSONArray()
                consumptions.forEach { c ->
                    cArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("projectId", c.projectId)
                        put("batchId", c.batchId)
                        put("quantityUsed", c.quantityUsed ?: JSONObject.NULL)
                        put("unit", c.unit ?: "")
                        put("timestamp", c.timestamp)
                        put("notes", c.notes)
                    })
                }
                put("consumptionRecords", cArray)

                // Inspiration Sources
                val srcArray = JSONArray()
                sources.forEach { s ->
                    srcArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("baseUrl", s.baseUrl)
                        put("searchTemplate", s.searchTemplate ?: "")
                        put("craftTags", s.craftTags)
                        put("description", s.description)
                        put("accountPaymentNote", s.accountPaymentNote)
                        put("isEnabled", s.isEnabled)
                        put("isBundled", s.isBundled)
                    })
                }
                put("inspirationSources", srcArray)

                // Saved Filters
                val fArray = JSONArray()
                filters.forEach { f ->
                    fArray.put(JSONObject().apply {
                        put("id", f.id)
                        put("name", f.name)
                        put("filterJson", f.filterJson)
                        put("createdAt", f.createdAt)
                    })
                }
                put("savedFilters", fArray)
            }

            // Write ZIP to destination outputUri
            val outputStream = context.contentResolver.openOutputStream(outputUri)
                ?: return@withContext Result.failure(IOException("Cannot open output URI"))

            var photoCount = 0
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // Write manifest JSON
                val manifestEntry = ZipEntry(MANIFEST_FILE_NAME)
                zipOut.putNextEntry(manifestEntry)
                zipOut.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // Pack photos from local storage
                val photoDir = PhotoManager.getPhotoDir(context)
                val photoFiles = photoDir.listFiles()
                if (photoFiles != null) {
                    for (file in photoFiles) {
                        if (file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png"))) {
                            val photoEntry = ZipEntry("$PHOTOS_FOLDER${file.name}")
                            zipOut.putNextEntry(photoEntry)
                            file.inputStream().use { fileIn ->
                                fileIn.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                            photoCount++
                        }
                    }
                }
            }

            Result.success(allSuppliesList.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun validateBackupArchive(context: Context, inputUri: Uri): BackupValidationResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext BackupValidationResult(false, errorMessage = "Unable to read backup file")

            var hasManifest = false
            var manifestContent: String? = null
            var photoCount = 0

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name
                    // Check Zip Slip
                    if (name.contains("..") || name.startsWith("/")) {
                        return@withContext BackupValidationResult(false, errorMessage = "Unsafe archive path detected: $name")
                    }
                    if (name == MANIFEST_FILE_NAME) {
                        hasManifest = true
                        manifestContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
                    } else if (name.startsWith(PHOTOS_FOLDER) && !entry.isDirectory) {
                        photoCount++
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (!hasManifest || manifestContent == null) {
                return@withContext BackupValidationResult(false, errorMessage = "Archive is missing valid manifest file ($MANIFEST_FILE_NAME)")
            }

            val json = JSONObject(manifestContent!!)
            val format = json.optString("format", "")
            if (format != "CraftStashBackup") {
                return@withContext BackupValidationResult(false, errorMessage = "Unrecognized backup format: $format")
            }

            val version = json.optInt("version", 1)
            if (version > CURRENT_BACKUP_VERSION) {
                return@withContext BackupValidationResult(
                    false,
                    version = version,
                    errorMessage = "Backup was created with a newer version of Craft Stash ($version). Please update the app."
                )
            }

            val supplies = json.optJSONArray("supplies")?.length() ?: 0
            val batches = json.optJSONArray("batches")?.length() ?: 0
            val locations = json.optJSONArray("storageLocations")?.length() ?: 0
            val projects = json.optJSONArray("projects")?.length() ?: 0

            BackupValidationResult(
                isValid = true,
                version = version,
                supplyCount = supplies,
                batchCount = batches,
                locationCount = locations,
                projectCount = projects,
                photoCount = photoCount,
                warningMessage = if (supplies == 0) "Archive contains 0 supplies." else null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupValidationResult(false, errorMessage = "Failed to parse archive: ${e.message}")
        }
    }

    suspend fun restoreBackup(context: Context, database: AppDatabase, inputUri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val validation = validateBackupArchive(context, inputUri)
            if (!validation.isValid) {
                return@withContext Result.failure(Exception(validation.errorMessage ?: "Invalid backup"))
            }

            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext Result.failure(IOException("Cannot open input URI"))

            var manifestContent: String? = null
            val photoDir = PhotoManager.getPhotoDir(context)

            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == MANIFEST_FILE_NAME) {
                        manifestContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
                    } else if (name.startsWith(PHOTOS_FOLDER) && !entry.isDirectory) {
                        val simpleFileName = name.removePrefix(PHOTOS_FOLDER)
                        val targetFile = File(photoDir, simpleFileName)
                        FileOutputStream(targetFile).use { fileOut ->
                            zipIn.copyTo(fileOut)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (manifestContent == null) {
                return@withContext Result.failure(Exception("Manifest missing"))
            }

            val json = JSONObject(manifestContent!!)
            val db = database.openHelper.writableDatabase
            db.beginTransaction()
            try {
                // Clear existing data cleanly in reverse foreign-key order
                db.execSQL("DELETE FROM consumption_records")
                db.execSQL("DELETE FROM project_allocations")
                db.execSQL("DELETE FROM project_requirements")
                db.execSQL("DELETE FROM projects")
                db.execSQL("DELETE FROM batches")
                db.execSQL("DELETE FROM supplies")
                db.execSQL("DELETE FROM storage_locations")
                db.execSQL("DELETE FROM saved_filters")

                // Insert locations
                val locArray = json.optJSONArray("storageLocations")
                if (locArray != null) {
                    for (i in 0 until locArray.length()) {
                        val o = locArray.getJSONObject(i)
                        db.execSQL(
                            "INSERT INTO storage_locations (id, name, parentId, notes, photoUri, createdAt) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf(
                                o.getString("id"),
                                o.getString("name"),
                                o.optString("parentId").ifBlank { null },
                                o.optString("notes", ""),
                                o.optString("photoUri").ifBlank { null },
                                o.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Insert supplies
                val supArray = json.optJSONArray("supplies")
                if (supArray != null) {
                    for (i in 0 until supArray.length()) {
                        val o = supArray.getJSONObject(i)
                        db.execSQL(
                            "INSERT INTO supplies (id, name, category, craftTypes, material, colorFamilies, manufacturerColor, appearance, brand, barcode, notes, tags, isFavorite, isDraft, photoUris, coverPhotoUri, categoryTemplateJson, customFieldsJson, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(
                                o.getString("id"),
                                o.getString("name"),
                                o.getString("category"),
                                o.getString("craftTypes"),
                                o.optString("material", ""),
                                o.optString("colorFamilies", ""),
                                o.optString("manufacturerColor", ""),
                                o.optString("appearance", ""),
                                o.optString("brand", ""),
                                o.optString("barcode", ""),
                                o.optString("notes", ""),
                                o.optString("tags", ""),
                                if (o.optBoolean("isFavorite", false)) 1 else 0,
                                if (o.optBoolean("isDraft", false)) 1 else 0,
                                o.optString("photoUris", ""),
                                o.optString("coverPhotoUri").ifBlank { null },
                                o.optString("categoryTemplateJson", ""),
                                o.optString("customFieldsJson", ""),
                                o.optLong("createdAt", System.currentTimeMillis()),
                                o.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Insert batches
                val bArray = json.optJSONArray("batches")
                if (bArray != null) {
                    for (i in 0 until bArray.length()) {
                        val o = bArray.getJSONObject(i)
                        val qVal = if (o.isNull("quantityValue")) null else o.optDouble("quantityValue")
                        val wVal = if (o.isNull("dimensionWidth")) null else o.optDouble("dimensionWidth")
                        val lVal = if (o.isNull("dimensionLength")) null else o.optDouble("dimensionLength")
                        db.execSQL(
                            "INSERT INTO batches (id, supplyId, batchIdentifier, quantityMode, quantityValue, unit, measurementMeaning, dimensionWidth, dimensionLength, dimensionUnit, storageLocationId, condition, notes, photoUri, isDepleted, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(
                                o.getString("id"),
                                o.getString("supplyId"),
                                o.optString("batchIdentifier", ""),
                                o.optString("quantityMode", QuantityMode.EXACT.name),
                                qVal,
                                o.optString("unit").ifBlank { null },
                                o.optString("measurementMeaning", "Quantity"),
                                wVal,
                                lVal,
                                o.optString("dimensionUnit").ifBlank { null },
                                o.optString("storageLocationId").ifBlank { null },
                                o.optString("condition", "Sealed"),
                                o.optString("notes", ""),
                                o.optString("photoUri").ifBlank { null },
                                if (o.optBoolean("isDepleted", false)) 1 else 0,
                                o.optLong("createdAt", System.currentTimeMillis()),
                                o.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Insert projects
                val pArray = json.optJSONArray("projects")
                if (pArray != null) {
                    for (i in 0 until pArray.length()) {
                        val o = pArray.getJSONObject(i)
                        val cDate = if (o.isNull("completionDate")) null else o.optLong("completionDate")
                        db.execSQL(
                            "INSERT INTO projects (id, title, status, craftType, sourceUrl, sourceName, notes, coverPhotoUri, progressPhotosJson, finishedMeasurements, completionDate, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(
                                o.getString("id"),
                                o.getString("title"),
                                o.optString("status", ProjectStatus.PLANNED.name),
                                o.optString("craftType", ""),
                                o.optString("sourceUrl").ifBlank { null },
                                o.optString("sourceName").ifBlank { null },
                                o.optString("notes", ""),
                                o.optString("coverPhotoUri").ifBlank { null },
                                o.optString("progressPhotosJson", ""),
                                o.optString("finishedMeasurements").ifBlank { null },
                                cDate,
                                o.optLong("createdAt", System.currentTimeMillis()),
                                o.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Insert requirements
                val rArray = json.optJSONArray("projectRequirements")
                if (rArray != null) {
                    for (i in 0 until rArray.length()) {
                        val o = rArray.getJSONObject(i)
                        val qReq = if (o.isNull("quantityRequired")) null else o.optDouble("quantityRequired")
                        val wReq = if (o.isNull("dimensionWidthRequired")) null else o.optDouble("dimensionWidthRequired")
                        val lReq = if (o.isNull("dimensionLengthRequired")) null else o.optDouble("dimensionLengthRequired")
                        db.execSQL(
                            "INSERT INTO project_requirements (id, projectId, description, category, material, colorPreference, quantityRequired, unit, dimensionWidthRequired, dimensionLengthRequired, dimensionUnit, singleContinuousPieceRequired, multipleBatchesAllowed, dyeLotMixingAllowed, isReusableTool, isRequired) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(
                                o.getString("id"),
                                o.getString("projectId"),
                                o.getString("description"),
                                o.optString("category").ifBlank { null },
                                o.optString("material").ifBlank { null },
                                o.optString("colorPreference").ifBlank { null },
                                qReq,
                                o.optString("unit").ifBlank { null },
                                wReq,
                                lReq,
                                o.optString("dimensionUnit").ifBlank { null },
                                if (o.optBoolean("singleContinuousPieceRequired", false)) 1 else 0,
                                if (o.optBoolean("multipleBatchesAllowed", true)) 1 else 0,
                                if (o.optBoolean("dyeLotMixingAllowed", true)) 1 else 0,
                                if (o.optBoolean("isReusableTool", false)) 1 else 0,
                                if (o.optBoolean("isRequired", true)) 1 else 0
                            )
                        )
                    }
                }

                // Insert allocations
                val aArray = json.optJSONArray("projectAllocations")
                if (aArray != null) {
                    for (i in 0 until aArray.length()) {
                        val o = aArray.getJSONObject(i)
                        val rQty = if (o.isNull("reservedQuantity")) null else o.optDouble("reservedQuantity")
                        db.execSQL(
                            "INSERT INTO project_allocations (id, projectId, requirementId, batchId, reservedQuantity, reservedUnit, matchStatus, notes, allocatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(
                                o.getString("id"),
                                o.getString("projectId"),
                                o.getString("requirementId"),
                                o.getString("batchId"),
                                rQty,
                                o.optString("reservedUnit").ifBlank { null },
                                o.optString("matchStatus", MatchStatus.EXACT_MATCH.name),
                                o.optString("notes", ""),
                                o.optLong("allocatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Insert consumption
                val cArray = json.optJSONArray("consumptionRecords")
                if (cArray != null) {
                    for (i in 0 until cArray.length()) {
                        val o = cArray.getJSONObject(i)
                        val qUsed = if (o.isNull("quantityUsed")) null else o.optDouble("quantityUsed")
                        db.execSQL(
                            "INSERT INTO consumption_records (id, projectId, batchId, quantityUsed, unit, timestamp, notes) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(
                                o.getString("id"),
                                o.getString("projectId"),
                                o.getString("batchId"),
                                qUsed,
                                o.optString("unit").ifBlank { null },
                                o.optLong("timestamp", System.currentTimeMillis()),
                                o.optString("notes", "")
                            )
                        )
                    }
                }

                db.setTransactionSuccessful()
                Result.success(true)
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
