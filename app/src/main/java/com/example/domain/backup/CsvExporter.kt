package com.example.domain.backup

import android.content.Context
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.domain.measurement.UnitConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

object CsvExporter {
    suspend fun exportInventoryToCsv(context: Context, database: AppDatabase, outputUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val db = database.openHelper.readableDatabase
            val locationsMap = mutableMapOf<String, String>()
            val locCursor = db.query("SELECT id, name FROM storage_locations")
            while (locCursor.moveToNext()) {
                val id = locCursor.getString(0)
                val name = locCursor.getString(1)
                locationsMap[id] = name
            }
            locCursor.close()

            val query = """
                SELECT 
                    s.id AS supplyId,
                    s.name AS supplyName,
                    s.category,
                    s.craftTypes,
                    s.material,
                    s.colorFamilies,
                    s.manufacturerColor,
                    s.appearance,
                    s.brand,
                    s.barcode,
                    s.tags,
                    s.isFavorite,
                    s.notes AS supplyNotes,
                    b.id AS batchId,
                    b.batchIdentifier,
                    b.quantityMode,
                    b.quantityValue,
                    b.unit,
                    b.dimensionWidth,
                    b.dimensionLength,
                    b.dimensionUnit,
                    b.storageLocationId,
                    b.condition,
                    b.isDepleted,
                    b.notes AS batchNotes
                FROM supplies s
                LEFT JOIN batches b ON s.id = b.supplyId
                WHERE s.isDraft = 0
                ORDER BY s.name ASC, b.createdAt ASC
            """.trimIndent()

            val cursor = db.query(query)
            val outputStream = context.contentResolver.openOutputStream(outputUri)
                ?: return@withContext Result.failure(Exception("Cannot open CSV output stream"))

            var rowCount = 0
            BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                // Write CSV disclaimer header explaining CSV is not a full backup
                writer.write("# Craft Stash Inventory Export (Readable spreadsheet export - not a full database backup)\n")
                // CSV header
                val headers = listOf(
                    "Supply ID",
                    "Supply Name",
                    "Category",
                    "Craft Types",
                    "Material",
                    "Color Families",
                    "Manufacturer Color",
                    "Appearance",
                    "Brand",
                    "Barcode",
                    "Tags",
                    "Favorite",
                    "Batch ID",
                    "Batch Identifier",
                    "Quantity Mode",
                    "Quantity",
                    "Unit",
                    "Formatted Quantity",
                    "Dimensions (W x L)",
                    "Storage Location",
                    "Condition",
                    "Status",
                    "Supply Notes",
                    "Batch Notes"
                )
                writer.write(headers.joinToString(",") { escapeCsv(it) })
                writer.write("\n")

                while (cursor.moveToNext()) {
                    rowCount++
                    val supplyId = cursor.getString(0)
                    val supplyName = cursor.getString(1)
                    val category = cursor.getString(2)
                    val craftTypes = cursor.getString(3)
                    val material = cursor.getString(4)
                    val colorFamilies = cursor.getString(5)
                    val mfgColor = cursor.getString(6)
                    val appearance = cursor.getString(7)
                    val brand = cursor.getString(8)
                    val barcode = cursor.getString(9)
                    val tags = cursor.getString(10)
                    val isFavorite = if (cursor.getInt(11) == 1) "Yes" else "No"
                    val supplyNotes = cursor.getString(12)

                    val batchId = cursor.getString(13) ?: ""
                    val batchIdentifier = cursor.getString(14) ?: ""
                    val qModeStr = cursor.getString(15) ?: ""
                    val qMode = try { com.example.domain.model.QuantityMode.valueOf(qModeStr) } catch(e: Exception) { com.example.domain.model.QuantityMode.EXACT }
                    val qVal = if (cursor.isNull(16)) null else cursor.getDouble(16)
                    val unit = cursor.getString(17) ?: ""
                    val formattedQty = UnitConverter.formatDisplayQuantity(qVal, unit, qMode)

                    val dimW = if (cursor.isNull(18)) null else cursor.getDouble(18)
                    val dimL = if (cursor.isNull(19)) null else cursor.getDouble(19)
                    val dimUnit = cursor.getString(20) ?: ""
                    val dims = if (dimW != null && dimL != null) "${UnitConverter.formatQuantity(dimW)} x ${UnitConverter.formatQuantity(dimL)} $dimUnit".trim() else ""

                    val locId = cursor.getString(21) ?: ""
                    val locName = locationsMap[locId] ?: ""
                    val condition = cursor.getString(22) ?: ""
                    val isDepleted = if (!cursor.isNull(23) && cursor.getInt(23) == 1) "Depleted" else "Active"
                    val batchNotes = cursor.getString(24) ?: ""

                    val row = listOf(
                        supplyId,
                        supplyName,
                        category,
                        craftTypes,
                        material,
                        colorFamilies,
                        mfgColor,
                        appearance,
                        brand,
                        barcode,
                        tags,
                        isFavorite,
                        batchId,
                        batchIdentifier,
                        qMode.displayName,
                        qVal?.toString() ?: "",
                        unit,
                        formattedQty,
                        dims,
                        locName,
                        condition,
                        isDepleted,
                        supplyNotes,
                        batchNotes
                    )
                    writer.write(row.joinToString(",") { escapeCsv(it) })
                    writer.write("\n")
                }
            }
            cursor.close()
            Result.success(rowCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }
}
