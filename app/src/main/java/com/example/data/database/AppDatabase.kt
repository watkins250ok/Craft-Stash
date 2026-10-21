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
        InspirationGalleryItemEntity::class,
        SavedFilterEntity::class,
        AppSettingsEntity::class
    ],
    version = 2,
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
    abstract fun inspirationGalleryDao(): InspirationGalleryDao
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_INSPIRATION_SOURCES = listOf(
            InspirationSourceEntity(
                id = "src_pinterest",
                name = "Pinterest",
                baseUrl = "https://www.pinterest.com/",
                searchTemplate = "https://www.pinterest.com/search/pins/?q=%s",
                craftTags = "general, crochet, embroidery",
                description = "Broad visual inspiration and maker boards.",
                accountPaymentNote = "Free browsing.",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_dmc",
                name = "DMC Patterns",
                baseUrl = "https://www.dmc.com/US/en/patterns",
                searchTemplate = "https://www.dmc.com/US/en/patterns?q=%s",
                craftTags = "crochet, embroidery, felting, cross stitch, weaving, needlework",
                description = "Embroidery and needlework pattern library.",
                accountPaymentNote = "Free pattern library.",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_ravelry",
                name = "Ravelry Patterns",
                baseUrl = "https://www.ravelry.com/patterns",
                searchTemplate = "https://www.ravelry.com/patterns/search#query=%s",
                craftTags = "yarn, crochet, knitting",
                description = "Knitting and crochet community pattern directory and yarn database.",
                accountPaymentNote = "Requires login",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_craftsy",
                name = "Craftsy Patterns",
                baseUrl = "https://www.craftsy.com/patterns",
                searchTemplate = "https://www.craftsy.com/patterns/search?q=%s",
                craftTags = "quilting, sewing, crochet",
                description = "Craftsy pattern directory.",
                accountPaymentNote = "Free and paid patterns available.",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_spruce",
                name = "The Spruce Crafts",
                baseUrl = "https://www.thesprucecrafts.com/",
                searchTemplate = "https://www.thesprucecrafts.com/search?q=%s",
                craftTags = "paper, needlework, kids, general",
                description = "Craft projects and tutorials.",
                accountPaymentNote = "Free articles and guides.",
                isEnabled = true,
                isBundled = true
            ),
            InspirationSourceEntity(
                id = "src_creativebug",
                name = "Creativebug Patterns",
                baseUrl = "https://www.creativebug.com/patterns",
                searchTemplate = "https://www.creativebug.com/search?q=%s",
                craftTags = "needlework, sewing, knitting, crochet, quilting",
                description = "Creativebug workshop patterns.",
                accountPaymentNote = "Subscription or login may be required.",
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

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
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
