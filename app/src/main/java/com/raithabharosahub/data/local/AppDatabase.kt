package com.raithabharosahub.data.local

import android.content.Context
import com.raithabharosahub.BuildConfig
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.SkipQueryVerification
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.raithabharosahub.data.local.dao.*
import com.raithabharosahub.data.local.entity.*
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
    version = 1,
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