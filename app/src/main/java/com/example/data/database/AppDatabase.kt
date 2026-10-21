package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        SupplyEntity::class,
        BatchEntity::class,
        StorageLocationEntity::class,
        ProjectEntity::class,
        ProjectRequirementEntity::class,
        ProjectAllocationEntity::class,
        ConsumptionRecordEntity::class,
        InspirationSourceEntity::class,
        SavedFilterEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun supplyDao(): SupplyDao
    abstract fun batchDao(): BatchDao
    abstract fun storageLocationDao(): StorageLocationDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectRequirementDao(): ProjectRequirementDao
    abstract fun projectAllocationDao(): ProjectAllocationDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun inspirationSourceDao(): InspirationSourceDao
    abstract fun savedFilterDao(): SavedFilterDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "craft_stash.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_INSPIRATION_SOURCES = listOf(
            InspirationSourceEntity(
                id = "src_pinterest",
                name = "Pinterest",
                baseUrl = "https://www.pinterest.com",
                searchTemplate = "https://www.pinterest.com/search/pins/?q=%s",
                craftTags = "All, Sewing, Knitting, Crochet, Embroidery, Jewelry, Paper",
                description = "Broad visual inspiration and maker boards.",
                accountPaymentNote = "Free browsing; account may be requested by Pinterest for extensive scrolling.",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_ravelry",
                name = "Ravelry",
                baseUrl = "https://www.ravelry.com",
                searchTemplate = "https://www.ravelry.com/patterns/search#query=%s",
                craftTags = "Knitting, Crochet, Weaving, Spinning",
                description = "Knitting and crochet community pattern directory and yarn database.",
                accountPaymentNote = "Free account required to save or purchase some community patterns; many free patterns available.",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_dmc",
                name = "DMC",
                baseUrl = "https://www.dmc.com",
                searchTemplate = "https://www.dmc.com/search?q=%s",
                craftTags = "Embroidery, Cross Stitch, Needlepoint, Punch Needle",
                description = "Embroidery floss manufacturer pattern and tutorial library.",
                accountPaymentNote = "Offers both free downloadable PDF patterns and paid design kits.",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_instructables",
                name = "Instructables",
                baseUrl = "https://www.instructables.com",
                searchTemplate = "https://www.instructables.com/howto/%s/",
                craftTags = "DIY, Woodworking, Sewing, Jewelry, Electronics, Paper",
                description = "Step-by-step community craft and maker tutorials.",
                accountPaymentNote = "Free open reading; free account optional.",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_duckduckgo",
                name = "Web Search",
                baseUrl = "https://duckduckgo.com",
                searchTemplate = "https://duckduckgo.com/?q=%s",
                craftTags = "All Crafts",
                description = "Privacy-friendly general web search for independent craft blogs and tutorials.",
                accountPaymentNote = "No account needed.",
                isEnabled = true,
                isBundled = true
            )
        )
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultSources(database.inspirationSourceDao())
                }
            }
        }

        suspend fun populateDefaultSources(sourceDao: InspirationSourceDao) {
            sourceDao.insertSources(DEFAULT_INSPIRATION_SOURCES)
        }
    }
}
