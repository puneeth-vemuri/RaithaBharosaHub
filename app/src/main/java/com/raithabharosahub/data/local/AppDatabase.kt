package com.raithabharosahub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.SkipQueryVerification
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.raithabharosahub.BuildConfig
import com.raithabharosahub.data.local.dao.FarmerDao
import com.raithabharosahub.data.local.dao.NpkDao
import com.raithabharosahub.data.local.dao.PlotDao
import com.raithabharosahub.data.local.dao.SeasonDao
import com.raithabharosahub.data.local.dao.WeatherDao
import com.raithabharosahub.data.local.entity.FarmerEntity
import com.raithabharosahub.data.local.entity.NpkEntity
import com.raithabharosahub.data.local.entity.PlotEntity
import com.raithabharosahub.data.local.entity.SeasonEntity
import com.raithabharosahub.data.local.entity.WeatherEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.Date
import java.util.concurrent.Executors

/**
 * Type converters for Room to handle Date and other custom types.
 */
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

/**
 * Main Room Database for Raitha-Bharosa Hub.
 * Includes all 5 entities and their DAOs.
 * Personal tables (farmer, npk) are encrypted using SQLCipher.
 */
@Database(
    entities = [
        FarmerEntity::class,
        PlotEntity::class,
        WeatherEntity::class,
        NpkEntity::class,
        SeasonEntity::class
    ],
    version = 2,
    exportSchema = false
)
@SkipQueryVerification
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun farmerDao(): FarmerDao
    abstract fun plotDao(): PlotDao
    abstract fun weatherDao(): WeatherDao
    abstract fun npkDao(): NpkDao
    abstract fun seasonDao(): SeasonDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "raitha_bharosa.db"
        private const val ENCRYPTION_PASSWORD = "default_encryption_key_change_in_production" // TODO: Secure this

        /**
         * Migration v1 → v2: adds the `last_updated_at` column to the weather table.
         * Existing rows get epoch (0) as the default, which the UI treats as "never updated".
         * A default of 0 avoids NOT NULL constraint issues without requiring a rebuild.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE weather ADD COLUMN last_updated_at INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /**
         * Get the database instance.
         * @param context Application context
         * @param useEncryption Whether to encrypt personal tables (farmer, npk). Default is true.
         */
        fun getDatabase(context: Context, useEncryption: Boolean = true): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, useEncryption)
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context, useEncryption: Boolean): AppDatabase {
            val passphrase = resolvePassphrase()
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback())
                .build()
        }

        private fun resolvePassphrase(): String {
            val configuredPassphrase = BuildConfig.DATABASE_PASSPHRASE
            if (configuredPassphrase.isNotBlank()) {
                return configuredPassphrase
            }

            check(BuildConfig.DEBUG) {
                "DATABASE_PASSPHRASE must be configured for release builds"
            }

            return ENCRYPTION_PASSWORD
        }

        /**
         * Database callback for pre-population and migration.
         */
        internal class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate with sample data if needed
                Executors.newSingleThreadExecutor().execute {
                    // Optional: Insert default data
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }
    }
}